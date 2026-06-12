package tech.kayys.gamelan;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.execution.actor.ActorSystem;
import tech.kayys.gamelan.execution.dag.*;
import tech.kayys.gamelan.memory.consolidation.MemoryConsolidationPipeline;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.memory.working.*;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.planning.cost.*;
import tech.kayys.gamelan.planning.graph.*;
import tech.kayys.gamelan.planning.versioning.*;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration test for the full Layer VII → VIII → IX pipeline.
 *
 * <p>This test exercises the complete lifecycle:
 * <ol>
 *   <li>Plan a task using GoT (Layer IX)</li>
 *   <li>Execute the plan as a DAG (Layer VII)</li>
 *   <li>Store results in working memory (Layer VIII)</li>
 *   <li>Consolidate memory and extract patterns (Layer VIII)</li>
 *   <li>Version the plan and record metrics (Layer IX)</li>
 *   <li>Optimize a follow-up plan based on cost (Layer IX)</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class LayerVII_VIII_IX_IntegrationTest {

    @Mock GollekSdk               sdk;
    @Mock GamelanConfig            config;
    @Mock SingleAgentOrchestrator  orchestrator;
    @Mock EpisodicMemory           episodic;
    @Mock SemanticMemory           semantic;
    @Mock ProceduralMemory         procedural;
    @Mock TokenEconomy             economy;

    @InjectMocks GraphOfThoughtPlanner gotPlanner;
    @InjectMocks DagExecutionEngine    dagEngine;
    @InjectMocks WorkingMemoryManager  workingMemory;
    @InjectMocks MemoryConsolidationPipeline consolidator;
    @InjectMocks PlanVersionStore      versionStore;
    @InjectMocks CostAwarePlanner      costPlanner;

    @BeforeEach
    void setUp() throws Exception {
        when(config.defaultModel()).thenReturn("test-model");
        when(config.sessionPersist()).thenReturn(false);
        when(config.tokenBudget()).thenReturn(8000);
        when(economy.remaining()).thenReturn(4000);
        when(episodic.all()).thenReturn(List.of());
        when(episodic.findRelevant(any(), anyInt())).thenReturn(List.of());
        when(semantic.allNodes()).thenReturn(Map.of());
        when(semantic.upsert(any(), any(), any(), anyLong(), anyDouble()))
                .thenReturn(mock(SemanticMemory.KnowledgeNode.class));

        // GoT expansion returns planning steps
        InferenceResponse gotResp = mock(InferenceResponse.class);
        when(gotResp.getContent()).thenReturn(
                "[\"read all service files\", \"identify test gaps\", \"write unit tests\"]");
        // GoT simulation scores return a number
        InferenceResponse simResp = mock(InferenceResponse.class);
        when(simResp.getContent()).thenReturn("0.8");
        when(sdk.createCompletion(any(InferenceRequest.class)))
                .thenReturn(gotResp)   // expansion
                .thenReturn(simResp)   // simulation score
                .thenReturn(gotResp);  // subsequent expansions

        // DAG execution returns success
        when(orchestrator.execute(any(AgentRequest.class)))
                .thenReturn(OrchestratorResult.ok(
                        "Test coverage increased from 45% to 82%.",
                        "react", 3, List.of(), Duration.ofSeconds(2)));
    }

    @Test
    void fullPipelineProducesResultAtEachStage() {
        // ── Stage 1: Plan via GoT ─────────────────────────────────────────
        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("Add comprehensive unit tests to OrderService", 5);

        assertThat(graph).isNotNull();
        assertThat(graph.nodeCount()).isGreaterThan(0);
        assertThat(graph.bestReasoning()).isNotBlank();

        // ── Stage 2: Build DAG from best reasoning ────────────────────────
        List<DagExecutionEngine.DagNode> nodes = List.of(
                DagExecutionEngine.DagNode.node("read-code",  "Read OrderService.java").build(),
                DagExecutionEngine.DagNode.node("find-gaps",  "Identify untested methods")
                        .dependsOn("read-code").build(),
                DagExecutionEngine.DagNode.node("write-tests","Write JUnit 5 tests")
                        .dependsOn("find-gaps").build(),
                DagExecutionEngine.DagNode.node("verify",     "Run tests and check coverage")
                        .dependsOn("write-tests").build()
        );
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(nodes);
        assertThat(dag.criticalPathLength()).isEqualTo(4);

        DagExecutionEngine.DagResult dagResult = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.SKIP_ON_FAILURE, null);
        assertThat(dagResult).isNotNull();
        assertThat(dagResult.summary()).isNotBlank();

        // ── Stage 3: Store execution output in working memory ─────────────
        int sysId = workingMemory.add(
                ConversationMessage.system("You are a test-writing expert."),
                WorkingMemoryManager.MessageImportance.CRITICAL);
        workingMemory.pin(sysId);

        workingMemory.add(ConversationMessage.user("Add unit tests to OrderService"));
        workingMemory.add(ConversationMessage.assistant(
                        dagResult.outputs().getOrDefault("write-tests", "Tests written.")));

        assertThat(workingMemory.messageCount()).isGreaterThanOrEqualTo(3);
        assertThat(workingMemory.totalTokens()).isGreaterThan(0);

        // ── Stage 4: Get optimized context ───────────────────────────────
        List<ConversationMessage> context =
                workingMemory.getContext("unit test OrderService", 8000);
        assertThat(context).isNotEmpty();
        // System message was pinned → must be in context
        assertThat(context.stream().anyMatch(m ->
                m.content().contains("test-writing expert"))).isTrue();

        // ── Stage 5: Memory consolidation ────────────────────────────────
        MemoryConsolidationPipeline.ConsolidationResult consolidation =
                consolidator.consolidate(true); // force=true

        assertThat(consolidation.ran()).isTrue();
        assertThat(consolidator.history()).hasSize(1);

        // ── Stage 6: Version the plan ────────────────────────────────────
        HierarchicalTaskPlanner.Plan plan = buildTestPlan("add-tests-plan", 4);
        String versionId = versionStore.store("Add unit tests to OrderService", plan, "executed");

        assertThat(versionId).isNotBlank();
        assertThat(versionStore.get(versionId)).isPresent();

        // Record actual metrics
        versionStore.recordMetrics(versionId,
                PlanVersionStore.PlanMetrics.success(
                        dagResult.elapsed().toMillis(), dagResult.successCount(), 2400));

        assertThat(versionStore.get(versionId).get().hasMetrics()).isTrue();

        // ── Stage 7: Cost-optimize a follow-up plan ───────────────────────
        HierarchicalTaskPlanner.Plan followUp = buildTestPlan("integration-tests-plan", 12);
        CostAwarePlanner.CostOptimizedPlan optimized =
                costPlanner.optimize(followUp, "Add integration tests", 2000);

        assertThat(optimized).isNotNull();
        assertThat(optimized.explanation()).isNotBlank();
        // Budget 2000 on a 12-step plan should trigger optimization
        assertThat(optimized.budgetTier()).isNotNull();
    }

    @Test
    void actorSystemLifecycleInPipeline() throws InterruptedException {
        ActorSystem system = ActorSystem.create("integration-test");

        // Spawn a "planning actor" that processes tasks
        java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger();
        ActorSystem.ActorRef<String> planActor = system.spawn("plan-actor",
                50, msg -> { processed.incrementAndGet(); return "planned:" + msg; },
                ActorSystem.SupervisionStrategy.RESTART);

        // Send messages representing planning steps
        planActor.tell("read code");
        planActor.tell("analyze gaps");
        planActor.tell("generate plan");

        // Give actors time to process
        Thread.sleep(200);

        assertThat(processed.get()).isGreaterThan(0);
        assertThat(system.isRunning()).isTrue();
        assertThat(system.actorCount()).isEqualTo(1);

        system.shutdown();
        assertThat(system.isRunning()).isFalse();
    }

    @Test
    void workingMemoryAndPlanVersionsIntegrate() {
        // Working memory accumulates conversation
        workingMemory.add(ConversationMessage.user("analyze security vulnerabilities"), WorkingMemoryManager.MessageImportance.HIGH);
        workingMemory.add(ConversationMessage.assistant("Found 3 SQL injection risks in UserRepository."), WorkingMemoryManager.MessageImportance.HIGH);
        workingMemory.add(ConversationMessage.user("fix them"), WorkingMemoryManager.MessageImportance.MEDIUM);

        // Create and version a fix plan
        HierarchicalTaskPlanner.Plan secPlan = buildTestPlan("security-fix", 3);
        String secVersionId = versionStore.store("fix SQL injection risks", secPlan, "security");

        // Retrieve best context for next step
        List<ConversationMessage> ctx = workingMemory.getContext("fix SQL injection", 8000);
        assertThat(ctx).isNotEmpty();

        // Version should be findable by tag
        List<PlanVersionStore.PlanVersion> secPlans = versionStore.findByTag("security");
        assertThat(secPlans).anyMatch(v -> v.id().equals(secVersionId));

        // Cost estimate for the security plan
        CostAwarePlanner.CostEstimate estimate = costPlanner.estimate(secPlan);
        assertThat(estimate.totalTokens()).isGreaterThan(0);
        assertThat(estimate.summary()).contains("CostEstimate");
    }

    @Test
    void dagAndWorkingMemoryShareOutputs() {
        // DAG with 2 independent nodes + 1 dependent
        DagExecutionEngine.ExecutionDag dag = dagEngine.build(List.of(
                DagExecutionEngine.DagNode.node("analyze", "Analyze code quality").build(),
                DagExecutionEngine.DagNode.node("benchmark", "Run benchmarks").build(),
                DagExecutionEngine.DagNode.node("report", "Write final report")
                        .dependsOn("analyze", "benchmark").build()
        ));

        DagExecutionEngine.DagResult result = dagEngine.execute(dag,
                DagExecutionEngine.FailurePolicy.CONTINUE_ON_FAILURE, null);

        // Store DAG outputs in working memory for follow-up
        result.outputs().forEach((nodeId, output) -> {
            if (!output.startsWith("[FAILED]") && !output.startsWith("[SKIPPED]")) {
                workingMemory.add(
                        ConversationMessage.assistant("[DAG:" + nodeId + "] " + output),
                        WorkingMemoryManager.MessageImportance.HIGH);
            }
        });

        WorkingMemoryManager.WorkingMemoryStats stats = workingMemory.stats();
        assertThat(stats.messageCount()).isGreaterThan(0);
        assertThat(stats.summary()).isNotBlank();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private HierarchicalTaskPlanner.Plan buildTestPlan(String name, int stepCount) {
        List<HierarchicalTaskPlanner.TaskNode> tasks = new ArrayList<>();
        for (int i = 0; i < stepCount; i++) {
            tasks.add(new HierarchicalTaskPlanner.TaskNode(
                    "task-" + i, "step " + i,
                    HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC,
                    i == 0 ? HierarchicalTaskPlanner.TaskNode.RiskLevel.LOW
                           : HierarchicalTaskPlanner.TaskNode.RiskLevel.MEDIUM,
                    List.of(), List.of(), 300,
                    "completed", Map.of()));
        }
        return new HierarchicalTaskPlanner.Plan(
                UUID.randomUUID().toString(), name, "test goal",
                tasks, HierarchicalTaskPlanner.ExecutionMode.HYBRID,
                stepCount * 300, 5000L, 0.88, 4800L, 1, Instant.now());
    }
}
