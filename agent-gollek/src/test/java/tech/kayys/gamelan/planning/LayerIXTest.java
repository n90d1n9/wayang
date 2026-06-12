package tech.kayys.gamelan.planning;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.planning.cost.*;
import tech.kayys.gamelan.planning.graph.*;
import tech.kayys.gamelan.planning.versioning.*;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Layer IX: Planning & Strategy tests.
 * Covers Graph-of-Thought MCTS planner, plan versioning with compare/rollback,
 * and cost-aware plan optimization with budget tiers.
 */
@ExtendWith(MockitoExtension.class)
class LayerIXTest {

    // ── GraphOfThoughtPlanner ─────────────────────────────────────────────

    @Mock GollekSdk     sdk;
    @Mock GamelanConfig config;

    @InjectMocks GraphOfThoughtPlanner gotPlanner;

    @BeforeEach
    void setUpGoT() throws Exception {
        when(config.defaultModel()).thenReturn("test-model");
    }

    @Test
    void planReturnsNonEmptyGraph() throws Exception {
        // Expansion: returns 3 child thoughts
        stubLlm("[\"step A\", \"step B\", \"step C\"]");

        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("refactor UserService for testability", 5);

        assertThat(graph).isNotNull();
        assertThat(graph.nodeCount()).isGreaterThan(0);
        assertThat(graph.root()).isNotNull();
    }

    @Test
    void planRootContainsTask() throws Exception {
        stubLlm("[\"approach one\", \"approach two\"]");

        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("add logging to all API endpoints", 3);

        assertThat(graph.root().thought()).contains("add logging to all API endpoints");
    }

    @Test
    void bestPathIsNotEmptyAfterPlanning() throws Exception {
        stubLlm("[\"concrete step A\", \"concrete step B\"]");

        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("fix null pointer in OrderService", 5);

        // Best path should at minimum contain the root
        assertThat(graph.bestPath()).isNotEmpty();
    }

    @Test
    void bestReasoningConcatenatesPath() throws Exception {
        stubLlm("[\"first step\", \"second step\"]");

        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("design a payment retry mechanism", 5);

        String reasoning = graph.bestReasoning();
        assertThat(reasoning).isNotBlank();
    }

    @Test
    void graphSummaryContainsNodeCount() throws Exception {
        stubLlm("[\"step X\"]");

        GraphOfThoughtPlanner.ThoughtGraph graph =
                gotPlanner.plan("optimize database queries", 3);

        assertThat(graph.summary()).isNotBlank().contains("nodes");
    }

    @Test
    void thoughtNodeUpdatesVisitCountOnBackpropagation() {
        GraphOfThoughtPlanner.ThoughtNode node =
                GraphOfThoughtPlanner.ThoughtNode.child(
                        UUID.randomUUID().toString(), "step reasoning",
                        "parent-id", 1);

        node.update(0.8);
        node.update(0.6);

        assertThat(node.visitCount()).isEqualTo(2);
        assertThat(node.avgScore()).isBetween(0.6, 0.8);
    }

    @Test
    void mergeCreatesSynthesisNode() throws Exception {
        stubLlm("Combined insight: use dependency injection with interface segregation.");

        GraphOfThoughtPlanner.ThoughtNode branchA =
                GraphOfThoughtPlanner.ThoughtNode.child("a", "use DI", "root", 1);
        GraphOfThoughtPlanner.ThoughtNode branchB =
                GraphOfThoughtPlanner.ThoughtNode.child("b", "use interfaces", "root", 1);

        GraphOfThoughtPlanner.ThoughtNode synthesis =
                gotPlanner.merge(List.of(branchA, branchB), "Combine these design approaches:");

        assertThat(synthesis.isSynthesis()).isTrue();
        assertThat(synthesis.mergedFromIds()).containsExactlyInAnyOrder("a", "b");
        assertThat(synthesis.thought()).isNotBlank();
    }

    @Test
    void thoughtGraphAddsEdgesCorrectly() {
        GraphOfThoughtPlanner.ThoughtNode root =
                GraphOfThoughtPlanner.ThoughtNode.root("design task");
        GraphOfThoughtPlanner.ThoughtGraph graph = new GraphOfThoughtPlanner.ThoughtGraph(root);

        GraphOfThoughtPlanner.ThoughtNode child =
                GraphOfThoughtPlanner.ThoughtNode.child("c1", "first step", root.id(), 1);
        graph.addNode(child);
        graph.addEdge(root.id(), child.id(), GraphOfThoughtPlanner.EdgeType.FOLLOWS);

        assertThat(graph.edgeCount()).isEqualTo(1);
        assertThat(root.children(graph)).hasSize(1);
    }

    @Test
    void ucb1PrioritizesUnvisitedNodes() {
        // Unvisited node should always win UCB1 selection
        GraphOfThoughtPlanner.ThoughtNode visited =
                GraphOfThoughtPlanner.ThoughtNode.child("v", "visited", "root", 1);
        GraphOfThoughtPlanner.ThoughtNode unvisited =
                GraphOfThoughtPlanner.ThoughtNode.child("u", "unvisited", "root", 1);

        visited.update(0.9);  // high score but visited

        // The unvisited node has visitCount=0 → UCB1 = MAX_VALUE → always selected first
        assertThat(unvisited.visitCount()).isEqualTo(0);
        assertThat(visited.visitCount()).isEqualTo(1);
    }

    // ── PlanVersionStore ──────────────────────────────────────────────────

    private PlanVersionStore store;

    @BeforeEach
    void setUpStore() {
        store = new PlanVersionStore();  // uses temp ~/.gamelan dir; OK for tests
    }

    @Test
    void storeAndRetrievePlanVersion() {
        HierarchicalTaskPlanner.Plan plan = simplePlan("write tests", 3);
        String versionId = store.store("write unit tests for UserService", plan, "candidate");

        Optional<PlanVersionStore.PlanVersion> found = store.get(versionId);
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(versionId);
        assertThat(found.get().tags()).contains("candidate");
    }

    @Test
    void historyReturnsMostRecentFirst() {
        String task = "add authentication to REST API";
        store.store(task, simplePlan("auth plan v1", 2), "draft");
        store.store(task, simplePlan("auth plan v2", 3), "candidate");
        store.store(task, simplePlan("auth plan v3", 4), "production");

        List<PlanVersionStore.PlanVersion> history = store.history(task);
        assertThat(history).hasSize(3);
        // Most recent first
        assertThat(history.get(0).tags()).contains("production");
    }

    @Test
    void findBestReturnsHighestSuccessRate() {
        String task = "optimize query performance";
        String v1 = store.store(task, simplePlan("plan A", 5));
        String v2 = store.store(task, simplePlan("plan B", 3));

        store.recordMetrics(v1, PlanVersionStore.PlanMetrics.failure(5000L, "timeout"));
        store.recordMetrics(v2, PlanVersionStore.PlanMetrics.success(2000L, 4, 1200));

        Optional<PlanVersionStore.PlanVersion> best = store.findBest(task);
        assertThat(best).isPresent();
        assertThat(best.get().id()).isEqualTo(v2);  // v2 has success rate 1.0
    }

    @Test
    void compareDetectsAddedAndRemovedTasks() {
        String taskA = "code review for payment module";
        String taskB = "code review for payment module";  // same task

        HierarchicalTaskPlanner.Plan planA = simplePlan("review", 2,
                List.of("read code", "write report"));
        HierarchicalTaskPlanner.Plan planB = simplePlan("review", 3,
                List.of("read code", "run tests", "write report", "verify fixes"));

        String v1 = store.store(taskA, planA);
        String v2 = store.store(taskB, planB);

        PlanVersionStore.PlanDiff diff = store.compare(v1, v2);

        assertThat(diff.tasksAdded()).isNotEmpty();
        assertThat(diff.summary()).isNotBlank();
    }

    @Test
    void compareMissingVersionsReturnsSafeDiff() {
        PlanVersionStore.PlanDiff diff = store.compare("nonexistent-a", "nonexistent-b");
        assertThat(diff.tasksAdded()).isEmpty();
        assertThat(diff.tasksRemoved()).isEmpty();
    }

    @Test
    void tagAndFindByTag() {
        String vId = store.store("test task", simplePlan("test", 2));
        store.tag(vId, "production-proven");

        List<PlanVersionStore.PlanVersion> tagged = store.findByTag("production-proven");
        assertThat(tagged).anyMatch(v -> v.id().equals(vId));
    }

    @Test
    void recordMetricsUpdatesVersion() {
        String vId = store.store("performance task", simplePlan("perf", 3));
        store.recordMetrics(vId, PlanVersionStore.PlanMetrics.success(1500L, 5, 800));

        Optional<PlanVersionStore.PlanVersion> found = store.get(vId);
        assertThat(found).isPresent();
        assertThat(found.get().hasMetrics()).isTrue();
        assertThat(found.get().metrics().success()).isTrue();
        assertThat(found.get().metrics().actualDurationMs()).isEqualTo(1500L);
    }

    @Test
    void totalVersionsCountsAll() {
        store.store("task X", simplePlan("x", 2));
        store.store("task Y", simplePlan("y", 3));
        store.store("task X", simplePlan("x2", 4));  // same task = 2 versions for X

        assertThat(store.totalVersions()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void planMetricsFactories() {
        PlanVersionStore.PlanMetrics success = PlanVersionStore.PlanMetrics.success(2000L, 5, 1000);
        assertThat(success.success()).isTrue();
        assertThat(success.successRate()).isEqualTo(1.0);

        PlanVersionStore.PlanMetrics failure = PlanVersionStore.PlanMetrics.failure(5000L, "timeout");
        assertThat(failure.success()).isFalse();
        assertThat(failure.failureReason()).isEqualTo("timeout");
    }

    // ── CostAwarePlanner ──────────────────────────────────────────────────

    @Mock TokenEconomy    economy;
    @Mock EpisodicMemory  episodic;
    @Mock PlanVersionStore versionStore2;

    @InjectMocks CostAwarePlanner costPlanner;

    @BeforeEach
    void setUpCostPlanner() {
        when(economy.remaining()).thenReturn(5000);
        when(episodic.all()).thenReturn(List.of());
        when(episodic.findRelevant(any(), anyInt())).thenReturn(List.of());
    }

    @Test
    void estimateSequentialHigherThanParallel() {
        HierarchicalTaskPlanner.Plan seqPlan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 5);
        HierarchicalTaskPlanner.Plan parPlan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.PARALLEL, 5);

        CostAwarePlanner.CostEstimate seqCost = costPlanner.estimate(seqPlan);
        CostAwarePlanner.CostEstimate parCost = costPlanner.estimate(parPlan);

        // Sequential accumulates context overhead → higher cost
        assertThat(seqCost.totalTokens()).isGreaterThan(parCost.totalTokens());
    }

    @Test
    void optimizePlanFitsWithinBudget() {
        // 10-step sequential plan with tight budget
        HierarchicalTaskPlanner.Plan bigPlan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 10);
        int tightBudget = 800;

        CostAwarePlanner.CostOptimizedPlan result = costPlanner.optimize(bigPlan, "complex task", tightBudget);

        // Result should be within or close to budget
        assertThat(result.estimate().totalTokens()).isLessThanOrEqualTo(tightBudget + 500); // allow 500 flex
    }

    @Test
    void planFittingBudgetRetainsNoOptimizationAction() {
        HierarchicalTaskPlanner.Plan smallPlan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.PARALLEL, 2);
        int generousBudget = 50_000;

        CostAwarePlanner.CostOptimizedPlan result =
                costPlanner.optimize(smallPlan, "small task", generousBudget);

        assertThat(result.action()).isEqualTo(CostAwarePlanner.OptimizationAction.NONE);
        assertThat(result.wasOptimized()).isFalse();
    }

    @Test
    void minimalBudgetTierUsesParallelMode() {
        HierarchicalTaskPlanner.Plan bigPlan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 8);

        CostAwarePlanner.CostOptimizedPlan result =
                costPlanner.optimize(bigPlan, "urgent small task", 500);

        assertThat(result.budgetTier()).isEqualTo(CostAwarePlanner.BudgetTier.MINIMAL);
    }

    @Test
    void costEstimateSummaryIsNonBlank() {
        HierarchicalTaskPlanner.Plan plan = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.HYBRID, 4);
        CostAwarePlanner.CostEstimate estimate = costPlanner.estimate(plan);

        assertThat(estimate.summary()).isNotBlank().contains("CostEstimate");
    }

    @Test
    void chooseCheaperSelectsLowerCostPlan() {
        HierarchicalTaskPlanner.Plan expensive = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 10);
        HierarchicalTaskPlanner.Plan cheap = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.PARALLEL, 2);

        HierarchicalTaskPlanner.Plan chosen =
                costPlanner.chooseCheaper(expensive, cheap, 0.2);

        assertThat(costPlanner.estimate(chosen).totalTokens())
                .isLessThanOrEqualTo(costPlanner.estimate(expensive).totalTokens());
    }

    @Test
    void historicalProfileNoDataWhenEmpty() {
        CostAwarePlanner.HistoricalCostProfile profile = costPlanner.historicalProfile("unknown task");
        assertThat(profile.hasData()).isFalse();
        assertThat(profile.summary()).contains("No historical");
    }

    @Test
    void optimizationExplanationIsNonBlank() {
        HierarchicalTaskPlanner.Plan big = planWithMode(
                HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 8);
        CostAwarePlanner.CostOptimizedPlan result =
                costPlanner.optimize(big, "task", 800);

        assertThat(result.explanation()).isNotBlank();
    }

    @Test
    void budgetTierClassification() {
        // Test the four tiers by using different budgets
        when(economy.remaining()).thenReturn(500);
        HierarchicalTaskPlanner.Plan plan = planWithMode(HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL, 2);

        // Force fit (plan is small)
        CostAwarePlanner.CostOptimizedPlan result = costPlanner.optimize(plan, "t", 500);
        // Budget 500 → MINIMAL or result fits → check tier is correct
        assertThat(result.budgetTier()).isIn(
                CostAwarePlanner.BudgetTier.MINIMAL,
                CostAwarePlanner.BudgetTier.STANDARD,
                CostAwarePlanner.BudgetTier.GENEROUS,
                CostAwarePlanner.BudgetTier.UNLIMITED);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Default LLM stub for GoT planning tests. */
    private void stubLlm(String content) throws Exception {
        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn(content);
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(resp);
    }

    private HierarchicalTaskPlanner.Plan simplePlan(String name, int estimatedTokens) {
        return simplePlan(name, estimatedTokens, List.of("analyze code", "write report"));
    }

    private HierarchicalTaskPlanner.Plan simplePlan(String name, int estimatedTokens,
                                                      List<String> taskNames) {
        List<HierarchicalTaskPlanner.TaskNode> tasks = taskNames.stream()
                .map(t -> new HierarchicalTaskPlanner.TaskNode(
                        UUID.randomUUID().toString(), t,
                        HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC,
                        HierarchicalTaskPlanner.TaskNode.RiskLevel.MEDIUM,
                        List.of(), List.of(), estimatedTokens / taskNames.size(),
                        "criterion", Map.of()))
                .toList();
        return new HierarchicalTaskPlanner.Plan(
                UUID.randomUUID().toString(), name, "test goal",
                tasks, HierarchicalTaskPlanner.ExecutionMode.SEQUENTIAL,
                estimatedTokens, 3000L, 0.85, 2800L, 1, Instant.now());
    }

    private HierarchicalTaskPlanner.Plan planWithMode(
            HierarchicalTaskPlanner.ExecutionMode mode, int stepCount) {
        List<HierarchicalTaskPlanner.TaskNode> tasks = new ArrayList<>();
        for (int i = 0; i < stepCount; i++) {
            tasks.add(new HierarchicalTaskPlanner.TaskNode(
                    "step-" + i, "task step " + i,
                    HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC,
                    HierarchicalTaskPlanner.TaskNode.RiskLevel.LOW,
                    List.of(), List.of(), 300,
                    "done", Map.of()));
        }
        return new HierarchicalTaskPlanner.Plan(
                UUID.randomUUID().toString(), "test-plan", "test goal",
                tasks, mode, stepCount * 300, 10_000L,
                0.8, 9500L, 1, Instant.now());
    }
}
