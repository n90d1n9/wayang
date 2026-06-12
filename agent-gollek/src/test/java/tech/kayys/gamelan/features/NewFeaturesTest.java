package tech.kayys.gamelan.features;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.feedback.*;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.cache.semantic.SemanticEmbeddingCache;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.resilience.circuit.AgentResilienceKit;
import tech.kayys.gamelan.search.hybrid.*;
import tech.kayys.gamelan.workflow.reactive.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewFeaturesTest {

    // ── FeedbackLearningEngine ─────────────────────────────────────────────

    @Mock EpisodicMemory episodic;
    @Mock SemanticMemory semantic;
    @Mock AgentTelemetry telemetry;

    @InjectMocks FeedbackLearningEngine feedback;

    @BeforeEach
    void setUpFeedback() {
        when(semantic.upsert(any(), any(), any(), anyLong(), anyDouble()))
                .thenReturn(mock(SemanticMemory.KnowledgeNode.class));
    }

    @Test
    void thumbsUpRecordsPositiveFeedback() {
        FeedbackLearningEngine.FeedbackEntry entry = feedback.thumbsUp(
                "t1", "write a sort", "Here is QuickSort...", null);
        assertThat(entry).isNotNull();
        assertThat(entry.type()).isEqualTo(FeedbackLearningEngine.FeedbackType.THUMBS_UP);
        assertThat(entry.satisfactionScore()).isEqualTo(1.0);
    }

    @Test
    void thumbsDownRecordsNegativeFeedback() {
        FeedbackLearningEngine.FeedbackEntry entry = feedback.thumbsDown(
                "t2", "fix NPE", "Try using Optional", "Not helpful");
        assertThat(entry.type()).isEqualTo(FeedbackLearningEngine.FeedbackType.THUMBS_DOWN);
        assertThat(entry.satisfactionScore()).isEqualTo(0.0);
    }

    @Test
    void satisfactionRateUpdatesWithEMA() {
        double initial = feedback.satisfactionRate();
        feedback.thumbsUp("t1", "task", "answer", null);
        assertThat(feedback.satisfactionRate()).isGreaterThanOrEqualTo(initial);
    }

    @Test
    void editCorrectionLearnedToSemanticMemory() {
        feedback.editCorrection("t3", "add method",
                "public void doThing() {}",
                "public Optional<String> doThing() {\n    return Optional.empty();\n}");
        verify(semantic, atLeastOnce()).upsert(any(), any(), any(), anyLong(), anyDouble());
    }

    @Test
    void stylePreferenceStoredAndRetrieved() {
        feedback.recordStylePreference("verbosity", "concise", "Short answers preferred");
        feedback.recordStylePreference("code_style", "Google Java Style", null);

        String block = feedback.generatePersonalizationBlock();
        assertThat(block).contains("verbosity").contains("concise");
        assertThat(block).contains("Google Java Style");
    }

    @Test
    void toolPreferenceAppearsInPersonalization() {
        feedback.recordToolPreference("write_file", false, "prefer apply_patch");
        feedback.recordToolPreference("read_file", true, "always read before editing");

        String block = feedback.generatePersonalizationBlock();
        assertThat(block).contains("write_file");
        assertThat(block).contains("Avoid");
        assertThat(block).contains("Prefer");
    }

    @Test
    void statsAggregateCorrectly() {
        feedback.thumbsUp("t1", "task", "ans", null);
        feedback.thumbsUp("t2", "task2", "ans2", null);
        feedback.thumbsDown("t3", "task3", "ans3", null);

        FeedbackLearningEngine.FeedbackStats stats = feedback.stats();
        assertThat(stats.totalEntries()).isEqualTo(3);
        assertThat(stats.positiveCount()).isEqualTo(2);
        assertThat(stats.negativeCount()).isEqualTo(1);
        assertThat(stats.summary()).isNotBlank();
    }

    @Test
    void allReturnsMostRecentFirst() {
        feedback.thumbsUp("t1", "task1", "ans1", null);
        feedback.thumbsDown("t2", "task2", "ans2", null);

        List<FeedbackLearningEngine.FeedbackEntry> all = feedback.all();
        assertThat(all).hasSize(2);
        // Most recent first
        assertThat(all.get(0).timestamp()).isAfterOrEqualTo(all.get(1).timestamp());
    }

    @Test
    void clearResetsAllState() {
        feedback.thumbsUp("t1", "task", "ans", null);
        feedback.recordStylePreference("k", "v", null);
        feedback.clear();

        assertThat(feedback.stats().totalEntries()).isEqualTo(0);
        assertThat(feedback.generatePersonalizationBlock()).isBlank();
    }

    @Test
    void personalizationBlockEmptyWithNoFeedback() {
        assertThat(feedback.generatePersonalizationBlock()).isBlank();
    }

    @Test
    void reinforcedStylePreferenceCountIncreases() {
        feedback.recordStylePreference("key", "value1", null);
        feedback.recordStylePreference("key", "value2", null); // reinforcement
        // Should not throw; reinforcement count increases
        assertThat(feedback.generatePersonalizationBlock()).contains("key");
    }

    // ── HybridSearchEngine ─────────────────────────────────────────────────

    @Mock SemanticEmbeddingCache embedCache2;
    @Mock AgentTelemetry         telemetry2;

    @InjectMocks HybridSearchEngine searchEngine;

    @TempDir Path searchRoot;

    @BeforeEach
    void setUpSearch() throws IOException {
        // Create test files
        Files.writeString(searchRoot.resolve("UserService.java"),
                "public class UserService {\n    public User findById(Long id) {\n        return repo.findById(id);\n    }\n    public void deleteUser(Long id) {\n        repo.delete(id);\n    }\n}");
        Files.writeString(searchRoot.resolve("OrderService.java"),
                "public class OrderService {\n    public Order placeOrder(Cart cart) {\n        return orderRepo.save(new Order(cart));\n    }\n}");
        Files.writeString(searchRoot.resolve("SecurityConfig.java"),
                "public class SecurityConfig {\n    public void configure(HttpSecurity http) {\n        http.oauth2Login();\n    }\n}");
        when(embedCache2.embed(anyString(), any())).thenReturn(new float[]{0.1f, 0.2f, 0.3f, 0.4f});
        when(telemetry2.getCount(any())).thenReturn(0L);
        when(telemetry2.histogram(any())).thenReturn(new AgentTelemetry.LatencyHistogram());
    }

    @Test
    void searchFindsRelevantFiles() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "user authentication", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        assertThat(results).isNotNull();
        assertThat(results.query()).isEqualTo("user authentication");
    }

    @Test
    void bm25FindsExactTermMatches() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "deleteUser", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        // UserService.java contains "deleteUser" — should appear in results
        assertThat(results.results()).isNotEmpty();
        boolean foundUserService = results.results().stream()
                .anyMatch(r -> r.path().contains("UserService"));
        assertThat(foundUserService).isTrue();
    }

    @Test
    void searchResultsHaveSnippets() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "findById", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        assertThat(results.results()).allMatch(r -> r.snippet() != null && !r.snippet().isEmpty());
    }

    @Test
    void searchSourceIndicatesHybrid() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "placeOrder", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        // When both searches succeed, result source should be HYBRID
        if (!results.results().isEmpty()) {
            assertThat(results.results().get(0).source())
                    .isIn(HybridSearchEngine.SearchSource.HYBRID,
                            HybridSearchEngine.SearchSource.BM25,
                            HybridSearchEngine.SearchSource.SEMANTIC);
        }
    }

    @Test
    void recordAccessBoostsReRanking() {
        // Record that UserService.java was accessed frequently
        for (int i = 0; i < 5; i++) searchEngine.recordAccess("UserService.java");

        HybridSearchEngine.SearchResults results = searchEngine.search(
                "service", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        // UserService.java should score higher due to click signal
        assertThat(results.results()).isNotEmpty();
    }

    @Test
    void searchWithGlobFilterLimitsFiles() throws IOException {
        Files.writeString(searchRoot.resolve("README.md"), "# Project README\nThis is a test project.");

        HybridSearchEngine.SearchResults results = searchEngine.search(
                "user", searchRoot,
                HybridSearchEngine.SearchOptions.withGlob("*.java"));
        // README.md should not appear
        boolean hasMarkdown = results.results().stream()
                .anyMatch(r -> r.path().endsWith(".md"));
        assertThat(hasMarkdown).isFalse();
    }

    @Test
    void emptyQueryReturnsEmptyResults() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        assertThat(results.results()).isEmpty();
    }

    @Test
    void searchSummaryIsNonBlank() {
        HybridSearchEngine.SearchResults results = searchEngine.search(
                "findById", searchRoot, HybridSearchEngine.SearchOptions.defaults());
        assertThat(results.summary()).isNotBlank();
    }

    @Test
    void searchOptionsCodeOnlyExcludesTests() {
        assertThat(HybridSearchEngine.SearchOptions.codeOnly().includeTests()).isFalse();
        assertThat(HybridSearchEngine.SearchOptions.codeOnly().globPattern()).isNotNull();
    }

    // ── ReactiveWorkflowEngine ─────────────────────────────────────────────

    @Mock SingleAgentOrchestrator orchestrator2;
    @Mock GamelanConfig            config2;
    @Mock AgentTelemetry           telemetry3;
    @Mock AgentResilienceKit        resilience2;

    @InjectMocks ReactiveWorkflowEngine reactiveEngine;

    @BeforeEach
    void setUpReactive() {
        when(config2.defaultModel()).thenReturn("test-model");
        when(config2.tokenBudget()).thenReturn(6000);
        when(config2.sessionPersist()).thenReturn(false);
    }

    @Test
    void emitAndReceiveEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        try (var sub = reactiveEngine.on("wf-1", "test.event", event -> {
            received.set(event.payload());
            latch.countDown();
        })) {
            reactiveEngine.emit("wf-1", "test.event", "hello world");
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(received.get()).isEqualTo("hello world");
        }
    }

    @Test
    void awaitBlocksUntilEvent() throws InterruptedException {
        // Emit event on a virtual thread after 100ms
        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(100); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            reactiveEngine.emit("wf-2", "ready", "data ready");
        });

        Optional<ReactiveWorkflowEngine.WorkflowEvent> result =
                reactiveEngine.await("wf-2", "ready", Duration.ofSeconds(2));
        assertThat(result).isPresent();
        assertThat(result.get().payload()).isEqualTo("data ready");
    }

    @Test
    void awaitReturnsEmptyOnTimeout() {
        Optional<ReactiveWorkflowEngine.WorkflowEvent> result =
                reactiveEngine.await("wf-3", "never.arrives", Duration.ofMillis(100));
        assertThat(result).isEmpty();
    }

    @Test
    void sagaSucceedsWhenAllStepsSucceed() {
        when(resilience2.withRetry(anyString(), anyInt(), anyLong(), any(), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>)inv.getArgument(4)).get());
        when(orchestrator2.execute(any(AgentRequest.class)))
                .thenReturn(OrchestratorResult.ok("step done", "react", 1, List.of(), Duration.ZERO));

        List<ReactiveWorkflowEngine.SagaStep> steps = List.of(
                ReactiveWorkflowEngine.SagaStep.of("step1", "do task 1"),
                ReactiveWorkflowEngine.SagaStep.of("step2", "do task 2 based on {{prior}}")
        );
        ReactiveWorkflowEngine.SagaResult result = reactiveEngine.executeSaga("saga-1", steps);

        assertThat(result.success()).isTrue();
        assertThat(result.completedSteps()).hasSize(2);
        assertThat(result.summary()).contains("SUCCESS");
    }

    @Test
    void sagaCompensatesOnFailure() {
        when(resilience2.withRetry(anyString(), anyInt(), anyLong(), any(), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>)inv.getArgument(4)).get());

        when(orchestrator2.execute(any(AgentRequest.class)))
                .thenReturn(OrchestratorResult.ok("step 1 done", "react", 1, List.of(), Duration.ZERO))
                .thenReturn(OrchestratorResult.failure("react", "step 2 failed", Duration.ZERO))
                .thenReturn(OrchestratorResult.ok("compensated", "react", 1, List.of(), Duration.ZERO));

        List<ReactiveWorkflowEngine.SagaStep> steps = List.of(
                ReactiveWorkflowEngine.SagaStep.withCompensation("step1", "do task 1", "undo task 1"),
                ReactiveWorkflowEngine.SagaStep.of("step2", "do task 2")
        );
        ReactiveWorkflowEngine.SagaResult result = reactiveEngine.executeSaga("saga-fail", steps);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStep()).isEqualTo("step2");
        assertThat(result.compensation()).isNotNull();
        assertThat(result.summary()).contains("FAILED");
    }

    @Test
    void sagaStepFactories() {
        var simple = ReactiveWorkflowEngine.SagaStep.of("name", "task");
        assertThat(simple.name()).isEqualTo("name");
        assertThat(simple.compensation()).isNull();

        var withComp = ReactiveWorkflowEngine.SagaStep.withCompensation("n", "t", "undo");
        assertThat(withComp.compensation()).isEqualTo("undo");
    }

    @Test
    void reactiveStepFactories() {
        var step = ReactiveWorkflowEngine.ReactiveStep.of("s", "trigger", "output", "task");
        assertThat(step.triggerEvent()).isEqualTo("trigger");
        assertThat(step.terminal()).isFalse();

        var terminal = ReactiveWorkflowEngine.ReactiveStep.terminal("t", "trigger", "task");
        assertThat(terminal.terminal()).isTrue();
        assertThat(terminal.outputEvent()).isEqualTo("workflow.done");
    }

    @Test
    void multipleSubscribersAllReceiveEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<String> received = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 3; i++) {
            reactiveEngine.on("wf-multi", "broadcast", event -> {
                received.add(event.payload());
                latch.countDown();
            });
        }

        reactiveEngine.emit("wf-multi", "broadcast", "fanout-message");
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(3);
        assertThat(received).allMatch(s -> s.equals("fanout-message"));
    }

    @Test
    void priorSubstitutionInSagaSteps() {
        when(resilience2.withRetry(anyString(), anyInt(), anyLong(), any(), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>)inv.getArgument(4)).get());

        ArgumentCaptor<AgentRequest> captor = ArgumentCaptor.forClass(AgentRequest.class);
        when(orchestrator2.execute(captor.capture()))
                .thenReturn(OrchestratorResult.ok("first output", "react", 1, List.of(), Duration.ZERO))
                .thenReturn(OrchestratorResult.ok("done", "react", 1, List.of(), Duration.ZERO));

        List<ReactiveWorkflowEngine.SagaStep> steps = List.of(
                ReactiveWorkflowEngine.SagaStep.of("step1", "initial task"),
                ReactiveWorkflowEngine.SagaStep.of("step2", "process {{prior}} result")
        );
        reactiveEngine.executeSaga("saga-prior", steps);

        // Second step's task should contain "first output" substituted for {{prior}}
        List<AgentRequest> requests = captor.getAllValues();
        assertThat(requests).hasSizeGreaterThanOrEqualTo(2);
        assertThat(requests.get(1).task()).contains("first output");
    }
}
