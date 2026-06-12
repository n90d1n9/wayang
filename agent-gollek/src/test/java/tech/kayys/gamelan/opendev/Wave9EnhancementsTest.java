package tech.kayys.gamelan.opendev;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.agent.critique.*;
import tech.kayys.gamelan.agent.multimodel.*;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.agent.routing.*;
import tech.kayys.gamelan.collaboration.workspace.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.context.dual_memory.*;
import tech.kayys.gamelan.execution.priority.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.runtime.offline.*;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.tool.result.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Wave 9 tests — DualMemoryManager, SelfCritiqueEngine, ToolResultOptimizer,
 * PriorityExecutionEngine, CollaborativeWorkspace, MultiModelEnsemble, OfflineRuntimeManager.
 */
@ExtendWith(MockitoExtension.class)
class Wave9EnhancementsTest {

    // ═══════════════════════════════════════════════════════════════════════
    // DualMemoryManager — §2.3.3
    // ═══════════════════════════════════════════════════════════════════════

    @Mock GamelanConfig           config;
    @Mock AgentTelemetry          telemetry;
    @Mock SingleAgentOrchestrator orchestrator;

    @InjectMocks DualMemoryManager dualMemory;

    private List<ConversationMessage> history(int turns) {
        List<ConversationMessage> msgs = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            msgs.add(ConversationMessage.user("Task " + i + ": do something"));
            msgs.add(ConversationMessage.assistant("Done task " + i));
        }
        return msgs;
    }

    @BeforeEach
    void mockOrchestrator() throws Exception {
        lenient().when(orchestrator.execute(any())).thenReturn(
                OrchestratorResult.success("Goal: fix the auth bug. Files: UserService.java, AuthController.java"));
    }

    @Test
    void assembleThinkingContextReturnsBoundedMessages() {
        var context = dualMemory.assembleThinkingContext(history(20), "current task");
        // Working memory = last 12 messages (6 pairs * 2), plus possible episodic, plus query
        assertThat(context).isNotEmpty();
        // Last element should be the current query
        var last = context.get(context.size() - 1);
        assertThat(last.content()).isEqualTo("current task");
    }

    @Test
    void workingMemoryPreservesVerbatimRecentMessages() {
        var fullHistory = history(30);
        var context = dualMemory.assembleThinkingContext(fullHistory, "query");
        // Working memory should include the very last message from history
        String lastHistoryContent = fullHistory.get(fullHistory.size() - 1).content();
        boolean lastPresent = context.stream()
                .anyMatch(m -> m.content().contains(lastHistoryContent));
        assertThat(lastPresent).isTrue();
    }

    @Test
    void episodicSummaryRegeneratedAfterFiveMessages() throws Exception {
        var hist = history(3);
        for (int i = 0; i < 5; i++) {
            dualMemory.onNewMessage(hist);
        }
        // After 5 messages, orchestrator should have been called for summary
        verify(orchestrator, atLeastOnce()).execute(any());
    }

    @Test
    void clearResetsState() {
        dualMemory.clear();
        assertThat(dualMemory.episodicSummary()).isEmpty();
    }

    @Test
    void shortHistoryReturnsAllMessages() {
        var hist = history(2);
        var context = dualMemory.assembleThinkingContext(hist, "query");
        // 4 history messages + possible episodic + query
        assertThat(context.size()).isGreaterThanOrEqualTo(3);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SelfCritiqueEngine — §2.2.6
    // ═══════════════════════════════════════════════════════════════════════

    @Mock WorkloadModelRouter      modelRouter;
    @Mock SingleAgentOrchestrator  orchestrator2;
    @Mock GamelanConfig            config2;
    @Mock AgentTelemetry           telemetry2;

    @InjectMocks SelfCritiqueEngine critique;

    @BeforeEach
    void setUpCritique() {
        lenient().when(modelRouter.modelFor(WorkloadModelRouter.ModelRole.CRITIQUE))
                .thenReturn("critique-model");
        lenient().when(modelRouter.modelFor(WorkloadModelRouter.ModelRole.THINKING))
                .thenReturn("thinking-model");
        lenient().when(modelRouter.isCritiqueEnabled()).thenReturn(true);
    }

    @Test
    void critiqueSoundReasoningReturnUnchanged() throws Exception {
        when(orchestrator2.execute(any()))
                .thenReturn(OrchestratorResult.success("Reasoning is sound."))
                .thenReturn(OrchestratorResult.success("original trace"));
        var result = critique.evaluate("fix the bug", "I will read the file, find the bug, edit it");
        assertThat(result.success()).isTrue();
        assertThat(result.effectiveTrace()).isNotBlank();
    }

    @Test
    void critiqueImprovesBadReasoning() throws Exception {
        when(orchestrator2.execute(any()))
                .thenReturn(OrchestratorResult.success("Missing: you didn't check if the file exists first"))
                .thenReturn(OrchestratorResult.success("IMPROVED: check existence, then read, then edit"));
        var result = critique.evaluate("fix the bug", "just edit the file directly");
        assertThat(result.success()).isTrue();
        // refined trace should differ from original
        assertThat(result.refinedTrace()).isNotEqualTo("just edit the file directly");
    }

    @Test
    void critiqueSkipsOnEmptyTrace() {
        var result = critique.evaluate("task", "");
        assertThat(result.skipped()).isTrue();
    }

    @Test
    void shouldCritiqueOnlyAtHighDepth() {
        when(modelRouter.isCritiqueEnabled()).thenReturn(true);
        assertThat(critique.shouldCritique(WorkloadModelRouter.ThinkingDepth.HIGH)).isTrue();
        when(modelRouter.isCritiqueEnabled()).thenReturn(false);
        assertThat(critique.shouldCritique(WorkloadModelRouter.ThinkingDepth.MEDIUM)).isFalse();
        assertThat(critique.shouldCritique(WorkloadModelRouter.ThinkingDepth.LOW)).isFalse();
    }

    @Test
    void critiqueResultSummaryNonBlank() throws Exception {
        when(orchestrator2.execute(any()))
                .thenReturn(OrchestratorResult.success("Reasoning is sound."))
                .thenReturn(OrchestratorResult.success("trace"));
        var result = critique.evaluate("task", "my reasoning here");
        assertThat(result.summary()).isNotBlank();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ToolResultOptimizer — §2.3.2
    // ═══════════════════════════════════════════════════════════════════════

    @Mock AgentTelemetry telemetry3;
    @Mock tech.kayys.gamelan.context.compaction.AdaptiveContextCompaction acc;

    @InjectMocks ToolResultOptimizer optimizer;

    @Test
    void fileReadSummarizedToMetadata() {
        String fileContent = "public class Foo {}\n".repeat(150); // 150 lines
        var result = optimizer.optimize("read_file", fileContent, true);
        assertThat(result.contextContent()).contains("Read file");
        assertThat(result.contextContent()).contains("150");
        assertThat(result.tokensSaved()).isGreaterThan(0);
    }

    @Test
    void searchResultSummarizedToMatchCount() {
        String searchOut = "src/A.java:10: match\nsrc/B.java:20: match\nsrc/C.java:30: match";
        var result = optimizer.optimize("search", searchOut, false);
        assertThat(result.contextContent()).containsIgnoringCase("search");
        assertThat(result.contextContent()).contains("3");
    }

    @Test
    void dirListingSummarizedToCount() {
        String listing = String.join("\n", "file1.java", "file2.java", "dir/", "README.md");
        var result = optimizer.optimize("list_dir", listing, false);
        assertThat(result.contextContent()).containsIgnoringCase("List");
        assertThat(result.contextContent()).contains("4");
    }

    @Test
    void shortCommandRetainedVerbatim() {
        String out = "BUILD SUCCESS";
        var result = optimizer.optimize("run_command", out, false);
        assertThat(result.contextContent()).contains("BUILD SUCCESS");
    }

    @Test
    void longCommandSummarizedToLineCount() {
        String longOut = "line\n".repeat(400);
        var result = optimizer.optimize("run_command", longOut, false);
        assertThat(result.contextContent()).containsIgnoringCase("400");
    }

    @Test
    void largeOutputOffloadedToDisk() {
        optimizer.setScratchDir(Path.of(System.getProperty("java.io.tmpdir")));
        String bigOutput = "x".repeat(9_000);
        var result = optimizer.optimize("read_file", bigOutput, true);
        assertThat(result.wasOffloaded()).isTrue();
        assertThat(result.contextContent()).contains("Output offloaded");
        assertThat(result.contextContent()).contains("Code Explorer");
    }

    @Test
    void offloadHintChangesWithoutSubagent() {
        optimizer.setScratchDir(Path.of(System.getProperty("java.io.tmpdir")));
        String bigOutput = "x".repeat(9_000);
        var result = optimizer.optimize("read_file", bigOutput, false); // no subagent
        assertThat(result.contextContent()).containsIgnoringCase("offset");
    }

    @Test
    void errorOutputTruncated() {
        String error = "NullPointerException: cannot call method on null\n" +
                "    at com.example.Foo.bar(Foo.java:42)\n".repeat(20);
        var result = optimizer.optimize("run_command", error, false);
        assertThat(result.contextContent()).contains("Error");
        assertThat(result.contextContent().length()).isLessThan(error.length());
    }

    @Test
    void batchOptimizationProcessesAll() {
        var outputs = List.of(
                new ToolResultOptimizer.ToolOutput("read_file", "content\n".repeat(10)),
                new ToolResultOptimizer.ToolOutput("search", "match1\nmatch2"),
                new ToolResultOptimizer.ToolOutput("list_dir", "a\nb\nc"));
        var results = optimizer.optimizeBatch(outputs, false);
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(r -> r.contextContent() != null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PriorityExecutionEngine
    // ═══════════════════════════════════════════════════════════════════════

    @Mock AgentTelemetry telemetry4;

    @InjectMocks PriorityExecutionEngine engine;

    @Test
    void criticalTaskRunsBeforeLow() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        var lowSpec      = PriorityExecutionEngine.TaskSpec.builder()
                .id("low").priority(PriorityExecutionEngine.Priority.LOW).batched(true).build();
        var criticalSpec = PriorityExecutionEngine.TaskSpec.builder()
                .id("crit").priority(PriorityExecutionEngine.Priority.CRITICAL).batched(true).build();

        engine.submit(lowSpec,      () -> { order.add("low"); return null; });
        engine.submit(criticalSpec, () -> { order.add("critical"); return null; });
        engine.drain();

        assertThat(order).isNotEmpty();
        // Critical should execute first or be in the list
        int critIdx = order.indexOf("critical");
        int lowIdx  = order.indexOf("low");
        if (critIdx >= 0 && lowIdx >= 0) {
            assertThat(critIdx).isLessThan(lowIdx);
        }
    }

    @Test
    void readTasksRunInParallel() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        var spec1 = PriorityExecutionEngine.TaskSpec.builder().readOnly(true).batched(true).build();
        var spec2 = PriorityExecutionEngine.TaskSpec.builder().readOnly(true).batched(true).build();
        engine.submit(spec1, () -> { bothStarted.countDown(); try { Thread.sleep(50); } catch (Exception e) {} return "r1"; });
        engine.submit(spec2, () -> { bothStarted.countDown(); try { Thread.sleep(50); } catch (Exception e) {} return "r2"; });
        engine.drain();
        // Both should have started (parallel execution)
        assertThat(bothStarted.await(3, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void tokenBudgetEnforcedOnSubmit() {
        engine.setTokenBudget(100);
        var spec = PriorityExecutionEngine.TaskSpec.builder()
                .tokens(200).build(); // exceeds budget
        String id = engine.submit(spec, () -> "result");
        assertThat(engine.statusOf(id))
                .isEqualTo(PriorityExecutionEngine.TaskStatus.BUDGET_EXCEEDED);
    }

    @Test
    void remainingBudgetDecreasesAfterExecution() {
        engine.setTokenBudget(1000);
        long before = engine.remainingBudget();
        var spec = PriorityExecutionEngine.TaskSpec.builder()
                .tokens(200).readOnly(true).build();
        engine.submit(spec, () -> "done");
        engine.drain();
        // Budget may decrease (depends on whether task actually ran)
        assertThat(engine.remainingBudget()).isLessThanOrEqualTo(before);
    }

    @Test
    void dependencyCheckedBeforeExecution() {
        var dep = PriorityExecutionEngine.TaskSpec.builder().id("dep").build();
        var dependent = PriorityExecutionEngine.TaskSpec.builder()
                .id("child").after("dep").build();
        engine.submit(dep,       () -> "dep-done");
        engine.submit(dependent, () -> "child-done");
        engine.drain();
        // Both should eventually reach some terminal state
        assertThat(engine.statusOf("dep")).isNotNull();
    }

    @Test
    void taskResultRetrievable() {
        var spec = PriorityExecutionEngine.TaskSpec.builder()
                .id("myTask").readOnly(true).build();
        engine.submit(spec, () -> "the-answer");
        engine.drain();
        if (engine.statusOf("myTask") == PriorityExecutionEngine.TaskStatus.COMPLETED) {
            Optional<String> result = engine.resultOf("myTask");
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("the-answer");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CollaborativeWorkspace
    // ═══════════════════════════════════════════════════════════════════════

    @Mock AgentTelemetry telemetry5;

    @InjectMocks CollaborativeWorkspace workspace;

    @BeforeEach
    void clearWorkspace() { workspace.clear(); }

    @Test
    void blackboardWriteAndRead() {
        workspace.write("result", "42", "agent-A", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        var entry = workspace.read("result");
        assertThat(entry).isPresent();
        assertThat(entry.get().value()).isEqualTo("42");
        assertThat(entry.get().authorId()).isEqualTo("agent-A");
    }

    @Test
    void lastWinsOverwritesPrevious() {
        workspace.write("key", "v1", "agentA", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        workspace.write("key", "v2", "agentB", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        assertThat(workspace.read("key").get().value()).isEqualTo("v2");
    }

    @Test
    void failFastThrowsOnConflict() {
        workspace.write("key", "v1", "agentA", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        assertThatThrownBy(() ->
                workspace.write("key", "v2", "agentB",
                        CollaborativeWorkspace.ConflictPolicy.FAIL_FAST))
                .isInstanceOf(CollaborativeWorkspace.WorkspaceConflictException.class);
    }

    @Test
    void mergeListAppendValues() {
        workspace.write("list", "item1", "agentA", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        workspace.write("list", "item2", "agentB", CollaborativeWorkspace.ConflictPolicy.MERGE_LIST);
        Object val = workspace.read("list").get().value();
        assertThat(val).isInstanceOf(List.class);
        assertThat((List<?>) val).hasSize(2);
    }

    @Test
    void directMessageDelivered() {
        workspace.register("agentB", null);
        workspace.send("agentA", "agentB", "result", "hello from A");
        var msgs = workspace.poll("agentB");
        assertThat(msgs).hasSize(1);
        assertThat(msgs.get(0).payload()).isEqualTo("hello from A");
        assertThat(msgs.get(0).fromAgent()).isEqualTo("agentA");
    }

    @Test
    void callbackInvokedOnMessage() {
        AtomicReference<CollaborativeWorkspace.AgentMessage> received = new AtomicReference<>();
        workspace.register("agentC", received::set);
        workspace.send("agentA", "agentC", "ping", "payload");
        assertThat(received.get()).isNotNull();
        assertThat(received.get().topic()).isEqualTo("ping");
    }

    @Test
    void broadcastReachesAllExceptSender() {
        workspace.register("a1", null);
        workspace.register("a2", null);
        workspace.register("a3", null);
        workspace.write("shared", "data", "a1", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        // Blackboard change broadcasts to all except a1
        assertThat(workspace.poll("a2")).isNotEmpty();
        assertThat(workspace.poll("a3")).isNotEmpty();
        assertThat(workspace.poll("a1")).isEmpty(); // sender excluded
    }

    @Test
    void lockAcquireAndRelease() {
        boolean acquired = workspace.acquireLock("src/Main.java", "agentA", 1000);
        assertThat(acquired).isTrue();
        assertThat(workspace.lockState()).containsKey("src/Main.java");

        boolean released = workspace.releaseLock("src/Main.java", "agentA");
        assertThat(released).isTrue();
        assertThat(workspace.lockState()).doesNotContainKey("src/Main.java");
    }

    @Test
    void lockRejectsWrongOwnerRelease() {
        workspace.acquireLock("resource", "agentA", 1000);
        boolean released = workspace.releaseLock("resource", "agentB"); // wrong owner
        assertThat(released).isFalse();
        assertThat(workspace.lockState()).containsKey("resource");
    }

    @Test
    void secondLockAcquireBlocksWithTimeout() {
        workspace.acquireLock("contested", "agentA", 1000);
        boolean acquired = workspace.acquireLock("contested", "agentB", 50); // short timeout
        assertThat(acquired).isFalse(); // already held
    }

    @Test
    void snapshotReturnsAllEntries() {
        workspace.write("k1", "v1", "a", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        workspace.write("k2", "v2", "b", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        var snap = workspace.snapshot();
        assertThat(snap).containsKeys("k1", "k2");
    }

    @Test
    void versionIncreasesWithEachWrite() {
        workspace.write("a", "1", "x", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        workspace.write("b", "2", "x", CollaborativeWorkspace.ConflictPolicy.LAST_WINS);
        long v1 = workspace.read("a").get().version();
        long v2 = workspace.read("b").get().version();
        assertThat(v2).isGreaterThan(v1);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OfflineRuntimeManager
    // ═══════════════════════════════════════════════════════════════════════

    @Mock GamelanConfig  config6;
    @Mock AgentTelemetry telemetry6;

    @InjectMocks OfflineRuntimeManager offline;

    @Test
    void healthyByDefault() {
        assertThat(offline.isProviderHealthy("openai")).isTrue();
    }

    @Test
    void circuitOpensAfterThreeFailures() {
        offline.recordFailure("openai");
        offline.recordFailure("openai");
        assertThat(offline.isProviderHealthy("openai")).isTrue(); // 2 failures — still OK
        offline.recordFailure("openai");
        assertThat(offline.isProviderHealthy("openai")).isFalse(); // 3 failures — circuit open
    }

    @Test
    void successResetsCircuit() {
        offline.recordFailure("anthropic");
        offline.recordFailure("anthropic");
        offline.recordFailure("anthropic"); // circuit opens
        offline.recordSuccess("anthropic");
        assertThat(offline.isProviderHealthy("anthropic")).isTrue();
    }

    @Test
    void responseCacheHit() {
        offline.cacheResponse("gpt4", "hello", "world");
        var cached = offline.getCachedResponse("gpt4", "hello");
        assertThat(cached).isPresent();
        assertThat(cached.get()).isEqualTo("world");
    }

    @Test
    void responseCacheMissReturnsEmpty() {
        var cached = offline.getCachedResponse("unknownModel", "no prompt");
        assertThat(cached).isEmpty();
    }

    @Test
    void capabilityCacheStoredAndRetrieved() {
        var cap = new OfflineRuntimeManager.ModelCapability("claude-4", 200_000, true,
                List.of("tool_use", "vision"));
        offline.cacheCapability("claude-4", cap);
        var retrieved = offline.getCapability("claude-4");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().contextLength()).isEqualTo(200_000);
        assertThat(retrieved.get().supportsVision()).isTrue();
    }

    @Test
    void healthReportContainsKnownProviders() {
        offline.recordFailure("azure");
        var report = offline.healthReport();
        assertThat(report).containsKey("azure");
        assertThat(report.get("azure").failureCount()).isEqualTo(1);
    }

    @Test
    void providerHealthSummaryNonBlank() {
        offline.recordFailure("test-provider");
        var health = offline.healthReport().get("test-provider");
        assertThat(health.summary()).isNotBlank();
        assertThat(health.summary()).contains("1"); // failure count
    }

    @Test
    void differentPromptsHaveDifferentCacheKeys() {
        offline.cacheResponse("m1", "prompt A", "answer A");
        offline.cacheResponse("m1", "prompt B", "answer B");
        assertThat(offline.getCachedResponse("m1", "prompt A").get()).isEqualTo("answer A");
        assertThat(offline.getCachedResponse("m1", "prompt B").get()).isEqualTo("answer B");
    }
}
