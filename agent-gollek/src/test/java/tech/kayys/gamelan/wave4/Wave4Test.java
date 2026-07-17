package tech.kayys.gamelan.wave4;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.chain.*;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.analytics.session.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.knowledge.graph.*;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.memory.snapshot.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.planning.versioning.PlanVersionStore;

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
class Wave4Test {

    // ═══════════════════════════════════════════════════════════════════════
    // AgentChain
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    SingleAgentOrchestrator orchestrator;
    @Mock
    GamelanConfig config;
    @Mock
    AgentTelemetry telemetry;

    @InjectMocks
    AgentChain chainEngine;

    @BeforeEach
    void setUpChain() {
        lenient().when(config.defaultModel()).thenReturn("test-model");
        lenient().when(config.tokenBudget()).thenReturn(6000);
        lenient().when(config.sessionPersist()).thenReturn(false);
    }

    @Test
    void singleLlmStepSucceeds() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("summarized text", "react", 1, List.of(), Duration.ZERO));

        AgentChain.ChainResult result = chainEngine.builder("test")
                .llm("summarize", "Summarize: {{input}}")
                .build()
                .run("long article here");

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("summarized text");
        assertThat(result.steps()).hasSize(1);
    }

    @Test
    void multiStepLlmChainPassesOutputForward() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("summary", "react", 1, List.of(), Duration.ZERO))
                .thenReturn(OrchestratorResult.ok("POSITIVE", "react", 1, List.of(), Duration.ZERO));

        AgentChain.ChainResult result = chainEngine.builder("pipeline")
                .llm("summarize", "Summarize: {{input}}")
                .llm("classify", "Classify sentiment of: {{input}}")
                .build()
                .run("some long text");

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("POSITIVE");
        assertThat(result.steps()).hasSize(2);
        // Second step receives first step's output
        assertThat(result.steps().get(1).input()).isEqualTo("summary");
    }

    @Test
    void transformStepAppliesPureFn() {
        AgentChain.ChainResult result = chainEngine.builder("transform")
                .transform("upper", String::toUpperCase)
                .build()
                .run("hello world");

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("HELLO WORLD");
        assertThat(result.steps().get(0).type()).isEqualTo(AgentChain.StepType.TRANSFORM);
    }

    @Test
    void filterStepShortCircuitsOnFalse() {
        AgentChain.ChainResult result = chainEngine.builder("filter-test")
                .filter("non-empty", s -> !s.isBlank())
                .llm("should-not-run", "Process: {{input}}")
                .build()
                .run("   "); // blank input → filter returns false

        assertThat(result.shortCircuited()).isTrue();
        assertThat(result.steps()).hasSize(1);
        verify(orchestrator, never()).execute(any());
    }

    @Test
    void filterStepContinuesOnTrue() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("processed", "react", 1, List.of(), Duration.ZERO));

        AgentChain.ChainResult result = chainEngine.builder("filter-pass")
                .filter("non-empty", s -> !s.isBlank())
                .llm("process", "Process: {{input}}")
                .build()
                .run("valid input");

        assertThat(result.shortCircuited()).isFalse();
        assertThat(result.output()).isEqualTo("processed");
    }

    @Test
    void validateStepThrowsOnFailure() {
        AgentChain.ChainResult result = chainEngine.builder("validate-test")
                .validate("has-content", s -> s.length() > 10)
                .build()
                .run("short"); // < 10 chars → validate fails

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("Validation failed");
    }

    @Test
    void branchStepRunsAllSubStepsInParallel() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("bugs found", "react", 1, List.of(), Duration.ZERO))
                .thenReturn(OrchestratorResult.ok("security ok", "react", 1, List.of(), Duration.ZERO))
                .thenReturn(OrchestratorResult.ok("perf ok", "react", 1, List.of(), Duration.ZERO));

        AgentChain.ChainResult result = chainEngine.builder("branch-test")
                .branch("parallel-review",
                        AgentChain.step("bugs", "Find bugs: {{input}}"),
                        AgentChain.step("security", "Find security issues: {{input}}"),
                        AgentChain.step("perf", "Find perf issues: {{input}}"))
                .build()
                .run("public void process() { /* code */ }");

        assertThat(result.success()).isTrue();
        // Branch output contains all three results merged
        assertThat(result.output()).contains("BUGS");
        assertThat(result.output()).contains("SECURITY");
        assertThat(result.output()).contains("PERF");
    }

    @Test
    void cachedStepReturnsSameResultForSameInput() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("cached result", "react", 1, List.of(), Duration.ZERO));

        var chain = chainEngine.builder("cache-test")
                .cached("expensive", "Analyze: {{input}}")
                .build();

        var r1 = chain.run("same input");
        var r2 = chain.run("same input");

        assertThat(r1.output()).isEqualTo(r2.output());
        // Second call should hit cache (no new LLM call)
        verify(orchestrator, times(1)).execute(any());
    }

    @Test
    void retryStepRetriesOnFailure() {
        when(orchestrator.execute(any()))
                .thenThrow(new RuntimeException("transient error"))
                .thenThrow(new RuntimeException("transient error"))
                .thenReturn(OrchestratorResult.ok("success on 3rd", "react", 1, List.of(), Duration.ZERO));

        AgentChain.ChainResult result = chainEngine.builder("retry-test")
                .withRetry("flaky-step", "Do: {{input}}", 3)
                .build()
                .run("input");

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("success on 3rd");
        verify(orchestrator, times(3)).execute(any());
    }

    @Test
    void retryStepFailsAfterMaxRetries() {
        when(orchestrator.execute(any()))
                .thenThrow(new RuntimeException("always fails"));

        AgentChain.ChainResult result = chainEngine.builder("retry-exhaust")
                .withRetry("always-fail", "Do: {{input}}", 2)
                .build()
                .run("input");

        assertThat(result.success()).isFalse();
        verify(orchestrator, times(2)).execute(any());
    }

    @Test
    void runBatchProcessesAllInputsInParallel() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("result", "react", 1, List.of(), Duration.ZERO));

        var chain = chainEngine.builder("batch")
                .llm("process", "Process: {{input}}")
                .build();

        List<AgentChain.ChainResult> results = chain.runBatch(
                List.of("input1", "input2", "input3"));

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(AgentChain.ChainResult::success);
    }

    @Test
    void chainResultSummaryIsNonBlank() {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("ok", "react", 1, List.of(), Duration.ZERO));

        var result = chainEngine.builder("summary-test")
                .llm("step", "Do: {{input}}")
                .build().run("x");
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void runAsyncCompletesSuccessfully() throws Exception {
        when(orchestrator.execute(any()))
                .thenReturn(OrchestratorResult.ok("async result", "react", 1, List.of(), Duration.ZERO));

        var future = chainEngine.builder("async")
                .llm("step", "Process: {{input}}")
                .build()
                .runAsync("input");

        AgentChain.ChainResult result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.success()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SessionAnalyticsEngine
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    EpisodicMemory episodic2;
    @Mock
    AgentTelemetry telemetry2;

    @InjectMocks
    SessionAnalyticsEngine analytics;

    @Test
    void recordTaskUpdatesHeatMap() {
        analytics.recordTask("t1", "refactor code", "refactoring", true, 3, 15_000,
                List.of("read_file", "apply_patch", "run_command"), 0.9);

        var heat = analytics.toolHeatMap();
        assertThat(heat).isNotEmpty();
        assertThat(heat).anyMatch(e -> e.toolName().equals("read_file"));
    }

    @Test
    void complexityClassificationSimple() {
        analytics.recordTask("t1", "quick fix", "bugfix", true, 1, 3_000,
                List.of("read_file"), 0.95);
        var report = analytics.generateReport(null);
        assertThat(report.complexityDistribution())
                .containsKey(SessionAnalyticsEngine.TaskComplexity.SIMPLE);
    }

    @Test
    void complexityClassificationVeryComplex() {
        analytics.recordTask("t2", "huge refactor", "refactoring", true, 15, 200_000,
                List.of("read_file", "write_file", "run_command"), 0.7);
        var report = analytics.generateReport(null);
        assertThat(report.complexityDistribution())
                .containsKey(SessionAnalyticsEngine.TaskComplexity.VERY_COMPLEX);
    }

    @Test
    void reportContainsToolHeatMap() {
        analytics.recordTask("t1", "task", "cat", true, 2, 5_000,
                List.of("read_file", "search_files"), 0.8);
        var report = analytics.generateReport(null);
        assertThat(report.toolHeatMap()).isNotEmpty();
    }

    @Test
    void bottleneckDetectedForSlowTool() {
        // Record a tool call with >5s latency
        analytics.recordToolCall("slow_tool", 6_000, false, 100, Instant.now());
        analytics.recordToolCall("slow_tool", 7_000, false, 100, Instant.now());
        var report = analytics.generateReport(null);
        assertThat(report.bottlenecks()).anyMatch(
                b -> b.name().equals("slow_tool") &&
                        b.type() == SessionAnalyticsEngine.BottleneckType.HIGH_LATENCY);
    }

    @Test
    void bottleneckDetectedForHighErrorRate() {
        for (int i = 0; i < 5; i++)
            analytics.recordToolCall("flaky_tool", 500, true, 0, Instant.now());
        var report = analytics.generateReport(null);
        assertThat(report.bottlenecks()).anyMatch(
                b -> b.name().equals("flaky_tool") &&
                        b.type() == SessionAnalyticsEngine.BottleneckType.HIGH_ERROR_RATE);
    }

    @Test
    void sequencePatternsTracked() {
        analytics.recordTask("t1", "task1", "cat", true, 2, 5_000,
                List.of("read_file", "write_file", "run_command"), 0.9);
        analytics.recordTask("t2", "task2", "cat", true, 2, 5_000,
                List.of("read_file", "write_file", "run_command"), 0.9);

        var report = analytics.generateReport(null);
        // read_file→write_file should appear as a sequence
        assertThat(report.topSequences()).anyMatch(
                s -> s.sequence().contains("read_file") && s.sequence().contains("write_file"));
    }

    @Test
    void strugglePatternsIdentifiedForHighFailRate() {
        for (int i = 0; i < 5; i++) {
            analytics.recordTask("t" + i, "hard task", "complex_domain",
                    false, 8, 60_000, List.of("read_file"), 0.2);
        }
        var struggles = analytics.identifyStrugglePatterns();
        assertThat(struggles).anyMatch(s -> s.category().equals("complex_domain"));
        assertThat(struggles.get(0).summary()).isNotBlank();
    }

    @Test
    void recommendationsGeneratedForBottlenecks() {
        for (int i = 0; i < 5; i++)
            analytics.recordToolCall("very_slow", 8_000, false, 0, Instant.now());
        var recs = analytics.generateRecommendations();
        assertThat(recs).isNotEmpty();
    }

    @Test
    void reportSummaryIsNonBlank() {
        analytics.recordTask("t1", "task", "cat", true, 2, 5_000, List.of("read_file"), 0.8);
        assertThat(analytics.generateReport(null).summary()).isNotBlank();
    }

    @Test
    void emptyReportWhenNoEvents() {
        var report = analytics.generateReport(null);
        assertThat(report.totalTasks()).isEqualTo(0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // KnowledgeGraph
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    AgentTelemetry telemetry3;

    @InjectMocks
    KnowledgeGraph kg;

    @Test
    void upsertAndFindNode() {
        kg.upsertNode("class:UserService", KnowledgeGraph.NodeType.CLASS, "UserService",
                Map.of("package", "tech.kayys"));
        var node = kg.findNode("class:UserService");
        assertThat(node).isPresent();
        assertThat(node.get().label()).isEqualTo("UserService");
        assertThat(node.get().type()).isEqualTo(KnowledgeGraph.NodeType.CLASS);
    }

    @Test
    void addEdgeAndQueryNeighbors() {
        kg.upsertNode("class:A", KnowledgeGraph.NodeType.CLASS, "A", Map.of());
        kg.upsertNode("class:B", KnowledgeGraph.NodeType.CLASS, "B", Map.of());
        kg.addEdge("class:A", "class:B", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, Map.of());

        var neighbors = kg.neighbors("class:A", KnowledgeGraph.EdgeType.DEPENDS_ON);
        assertThat(neighbors).hasSize(1);
        assertThat(neighbors.get(0).id()).isEqualTo("class:B");
    }

    @Test
    void predecessorsQueryReversesEdgeDirection() {
        kg.addEdge("class:A", "class:B", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        var preds = kg.predecessors("class:B", KnowledgeGraph.EdgeType.DEPENDS_ON);
        assertThat(preds).anyMatch(n -> n.id().equals("class:A"));
    }

    @Test
    void reachableComputesTransitiveClosure() {
        // A → B → C → D
        kg.addEdge("A", "B", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("B", "C", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("C", "D", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);

        var reachable = kg.reachable("A", KnowledgeGraph.EdgeType.DEPENDS_ON, 10);
        assertThat(reachable).extracting(KnowledgeGraph.KgNode::id)
                .containsExactlyInAnyOrder("B", "C", "D");
    }

    @Test
    void impactAnalysisFindsAffectedNodes() {
        // UserController → UserService → UserRepository
        kg.addEdge("UserController", "UserService", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("UserService", "UserRepository", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        // Test covers UserService
        kg.addEdge("UserServiceTest", "UserService", KnowledgeGraph.EdgeType.TESTED_BY, 1.0, null);

        var impact = kg.impactAnalysis("UserRepository", 5);
        assertThat(impact.impactCount()).isGreaterThan(0);
        assertThat(impact.impactedNodes()).extracting(KnowledgeGraph.KgNode::id)
                .contains("UserService");
        assertThat(impact.summary()).isNotBlank();
    }

    @Test
    void detectCyclesFindsCircularDependency() {
        // A → B → C → A
        kg.addEdge("A", "B", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("B", "C", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("C", "A", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);

        var cycles = kg.detectCycles();
        assertThat(cycles).isNotEmpty();
        // The SCC should contain A, B, C
        var cycleNodes = cycles.stream().flatMap(List::stream).toList();
        assertThat(cycleNodes).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void detectCyclesNoneForAcyclicGraph() {
        kg.addEdge("X", "Y", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("Y", "Z", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        var cycles = kg.detectCycles();
        assertThat(cycles).isEmpty();
    }

    @Test
    void shortestPathFound() {
        kg.addEdge("S", "M1", KnowledgeGraph.EdgeType.RELATED_TO, 1.0, null);
        kg.addEdge("M1", "T", KnowledgeGraph.EdgeType.RELATED_TO, 1.0, null);
        kg.addEdge("S", "T", KnowledgeGraph.EdgeType.RELATED_TO, 1.0, null); // direct path

        var paths = kg.shortestPaths("S", "T", 5);
        assertThat(paths).isNotEmpty();
        // Shortest path should be S → T (direct)
        assertThat(paths.get(0)).containsExactly("S", "T");
    }

    @Test
    void removeNodeCleansUpEdges() {
        kg.addEdge("X", "Y", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("X", "Z", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        boolean removed = kg.removeNode("X");
        assertThat(removed).isTrue();
        assertThat(kg.findNode("X")).isEmpty();
        assertThat(kg.neighbors("X")).isEmpty();
    }

    @Test
    void searchByLabelText() {
        kg.upsertNode("class:AuthService", KnowledgeGraph.NodeType.CLASS, "AuthService", Map.of());
        kg.upsertNode("class:UserService", KnowledgeGraph.NodeType.CLASS, "UserService", Map.of());
        var results = kg.search("auth");
        assertThat(results).anyMatch(n -> n.id().equals("class:AuthService"));
    }

    @Test
    void findByTypeFilters() {
        kg.upsertNode("c:A", KnowledgeGraph.NodeType.CLASS, "A", Map.of());
        kg.upsertNode("m:a", KnowledgeGraph.NodeType.METHOD, "a()", Map.of());
        var classes = kg.findByType(KnowledgeGraph.NodeType.CLASS);
        assertThat(classes).allMatch(n -> n.type() == KnowledgeGraph.NodeType.CLASS);
    }

    @Test
    void statsReturnsNodeAndEdgeCounts() {
        kg.addEdge("A", "B", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        kg.addEdge("B", "C", KnowledgeGraph.EdgeType.CALLS, 1.0, null);
        var stats = kg.stats();
        assertThat(stats.nodeCount()).isGreaterThanOrEqualTo(3);
        assertThat(stats.edgeCount()).isGreaterThanOrEqualTo(2);
        assertThat(stats.summary()).isNotBlank();
    }

    @Test
    void toMermaidGeneratesValidSyntax() {
        kg.addEdge("Service", "Repository", KnowledgeGraph.EdgeType.DEPENDS_ON, 1.0, null);
        String mermaid = kg.toMermaid(50);
        assertThat(mermaid).startsWith("graph TD");
        assertThat(mermaid).contains("DEPENDS_ON");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MemorySnapshotManager
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    EpisodicMemory episodic3;
    @Mock
    SemanticMemory semantic3;
    @Mock
    ProceduralMemory procedural3;
    @Mock
    PlanVersionStore planStore3;
    @Mock
    AgentTelemetry telemetry4;

    @InjectMocks
    MemorySnapshotManager snapshotMgr;

    private EpisodicMemory.Episode fakeEpisode(String id) {
        return new EpisodicMemory.Episode(id, "task " + id, "answer " + id,
                true, 1, 1000L, List.of(), 0.9, Instant.now());
    }

    private SemanticMemory.KnowledgeNode fakeNode(String concept) {
        return new SemanticMemory.KnowledgeNode(concept.hashCode(), concept,
                "fact about " + concept, SemanticMemory.NodeType.FACT,
                0L, 0.9, Instant.now(), 1);
    }

    @BeforeEach
    void setUpSnapshot() {
        lenient().when(episodic3.all()).thenReturn(List.of(fakeEpisode("e1"), fakeEpisode("e2")));
        lenient().when(semantic3.allNodes()).thenReturn(Map.of(
                1L, fakeNode("java-version"),
                2L, fakeNode("test-command")));
        lenient().when(procedural3.allStrategies()).thenReturn(List.of());
    }

    @Test
    void captureCreatesSnapshot() {
        var snap = snapshotMgr.capture("test-snapshot", Map.of("project", "gamelan"));
        assertThat(snap).isNotNull();
        assertThat(snap.label()).isEqualTo("test-snapshot");
        assertThat(snap.episodes()).hasSize(2);
        assertThat(snap.semanticNodes()).hasSize(2);
        assertThat(snap.contentHash()).isNotBlank();
        assertThat(snap.summary()).isNotBlank();
    }

    @Test
    void captureLightSkipsEpisodic() {
        var snap = snapshotMgr.captureLight("light-snap");
        assertThat(snap.label()).contains("light");
    }

    @Test
    void saveAndLoadRoundtrip(@TempDir Path tmp) throws IOException {
        var snap = snapshotMgr.capture("roundtrip", Map.of());
        // Override snapshot dir by saving directly
        Files.createDirectories(tmp);
        Path file = tmp.resolve("test.snap.gz");
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(
                Files.newOutputStream(file))) {
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writerWithDefaultPrettyPrinter().writeValue(gz, snap);
        }

        var loaded = snapshotMgr.load(file);
        assertThat(loaded.id()).isEqualTo(snap.id());
        assertThat(loaded.label()).isEqualTo("roundtrip");
        assertThat(loaded.episodes()).hasSize(snap.episodes().size());
        assertThat(loaded.semanticNodes()).hasSize(snap.semanticNodes().size());
    }

    @Test
    void restoreDryRunDoesNotModifyMemory() {
        var snap = snapshotMgr.capture("dry-run-test", Map.of());
        var result = snapshotMgr.restore(snap, true);
        assertThat(result.dryRun()).isTrue();
        // Memory should NOT have been cleared
        verify(episodic3, never()).clear();
        verify(semantic3, never()).clear();
    }

    @Test
    void restoreAppliedCallsMemoryClear() {
        var snap = snapshotMgr.capture("restore-test", Map.of());
        snapshotMgr.restore(snap, false);
        verify(episodic3, atLeastOnce()).clear();
        verify(semantic3, atLeastOnce()).clear();
    }

    @Test
    void restoreResultSummaryIsNonBlank() {
        var snap = snapshotMgr.capture("summary-test", Map.of());
        var result = snapshotMgr.restore(snap, true);
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void diffDetectsAddedEpisodes() {
        var before = snapshotMgr.capture("before", Map.of());

        when(episodic3.all()).thenReturn(
                List.of(fakeEpisode("e1"), fakeEpisode("e2"), fakeEpisode("e3")));
        var after = snapshotMgr.capture("after", Map.of());

        var diff = snapshotMgr.diff(before, after);
        assertThat(diff.addedEpisodes()).hasSize(1);
        assertThat(diff.addedEpisodes().get(0).id()).isEqualTo("e3");
        assertThat(diff.summary()).isNotBlank();
    }

    @Test
    void diffDetectsRemovedConcepts() {
        var before = snapshotMgr.capture("before", Map.of());

        // Remove "test-command" concept from after
        when(semantic3.allNodes()).thenReturn(Map.of(1L, fakeNode("java-version")));
        var after = snapshotMgr.capture("after", Map.of());

        var diff = snapshotMgr.diff(before, after);
        assertThat(diff.removedConcepts()).contains("test-command");
    }

    @Test
    void diffHasChangesWhenContentDiffers() {
        var before = snapshotMgr.capture("before", Map.of());
        when(episodic3.all()).thenReturn(List.of(fakeEpisode("e99")));
        var after = snapshotMgr.capture("after", Map.of());
        assertThat(snapshotMgr.diff(before, after).hasChanges()).isTrue();
    }

    @Test
    void noDiffWhenIdentical() {
        var snap = snapshotMgr.capture("same", Map.of());
        var diff = snapshotMgr.diff(snap, snap);
        assertThat(diff.addedEpisodes()).isEmpty();
        assertThat(diff.removedEpisodes()).isEmpty();
        assertThat(diff.addedConcepts()).isEmpty();
    }

    @Test
    void verifyPassesForFreshSnapshot() {
        var snap = snapshotMgr.capture("verify-test", Map.of());
        // Verify should pass since hash was just computed
        boolean ok = snapshotMgr.verify(snap);
        assertThat(ok).isTrue();
    }
}
