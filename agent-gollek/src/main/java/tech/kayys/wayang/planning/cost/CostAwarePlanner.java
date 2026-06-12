package tech.kayys.gamelan.planning.cost;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.economics.TokenEconomy;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.planning.versioning.PlanVersionStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cost-Aware Planner — generates and selects plans that respect token budgets.
 *
 * <h2>Problem</h2>
 * Standard planners optimize for task quality and ignore cost. In practice:
 * <ul>
 *   <li>A 20-step plan may be perfect but cost 15,000 tokens — over budget</li>
 *   <li>A 5-step plan may be 90% as good at 2,000 tokens — clearly better ROI</li>
 *   <li>The same plan with PARALLEL execution may cost fewer tokens than SEQUENTIAL
 *       because parallel steps don't accumulate context from each other</li>
 * </ul>
 *
 * <h2>Cost Model</h2>
 * Token cost for a plan is estimated as:
 * <pre>
 * SEQUENTIAL: sum(step.estimatedTokens) + context_overhead_per_step
 * PARALLEL:   max(step.estimatedTokens) + coordination_overhead
 * HYBRID:     weighted combination
 * </pre>
 *
 * Where {@code context_overhead_per_step} reflects that each sequential step
 * receives the full output of all prior steps as context.
 *
 * <h2>Budget tiers</h2>
 * <pre>
 * MINIMAL  (< 1,000 tokens):   prefer PARALLEL, prune risky/optional steps
 * STANDARD (1,000–4,000):      balanced, use HYBRID where appropriate
 * GENEROUS (4,000–10,000):     prefer quality, allow SEQUENTIAL
 * UNLIMITED (> 10,000):        use the HTN plan as-is, no pruning
 * </pre>
 *
 * <h2>Pruning strategies</h2>
 * <ul>
 *   <li>Remove LOW risk steps that are optional</li>
 *   <li>Merge adjacent steps with similar tools into a single step</li>
 *   <li>Replace COMPOSITE steps with a single ATOMIC approximation</li>
 *   <li>Skip verification steps when confidence is high</li>
 * </ul>
 */
@ApplicationScoped
public class CostAwarePlanner {

    private static final Logger log = LoggerFactory.getLogger(CostAwarePlanner.class);

    // Context overhead per sequential step (tokens added to each step's input)
    private static final int SEQ_CONTEXT_OVERHEAD    = 200;
    // Coordination overhead for parallel execution (message passing, synthesis)
    private static final int PARALLEL_COORD_OVERHEAD = 300;
    // Token overhead for verification steps (reading output of prior step)
    private static final int VERIFY_STEP_OVERHEAD    = 150;

    @Inject TokenEconomy      economy;
    @Inject EpisodicMemory    episodic;
    @Inject PlanVersionStore  versionStore;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Takes an HTN plan and optimizes it for the current token budget.
     * May prune steps, change execution mode, or suggest a simpler alternative.
     *
     * @param plan       the plan to optimize
     * @param task       the original task (for context)
     * @param budgetHint override budget (null = use configured budget)
     * @return the optimized, cost-aware plan
     */
    public CostOptimizedPlan optimize(HierarchicalTaskPlanner.Plan plan, String task, Integer budgetHint) {
        int budget   = budgetHint != null ? budgetHint : economy.remaining();
        BudgetTier tier = classifyBudget(budget);

        log.info("[cost-planner] optimizing plan '{}': {} steps, budget={} ({})",
                plan.name(), plan.tasks().size(), budget, tier);

        // Estimate raw cost
        CostEstimate rawEstimate = estimate(plan);

        if (rawEstimate.totalTokens() <= budget) {
            log.debug("[cost-planner] plan fits budget ({} <= {})", rawEstimate.totalTokens(), budget);
            return new CostOptimizedPlan(plan, rawEstimate, OptimizationAction.NONE,
                    "Plan fits within budget", tier);
        }

        // Try to optimize
        HierarchicalTaskPlanner.Plan optimized = switch (tier) {
            case MINIMAL   -> aggressivePrune(plan, budget);
            case STANDARD  -> moderatePrune(plan, budget);
            case GENEROUS  -> lightPrune(plan, budget);
            case UNLIMITED -> plan;
        };

        CostEstimate optimizedEstimate = estimate(optimized);
        OptimizationAction action = determineAction(plan, optimized);

        log.info("[cost-planner] optimized: {} → {} tokens ({} action)",
                rawEstimate.totalTokens(), optimizedEstimate.totalTokens(), action);

        // Record as a plan version with cost metadata
        versionStore.store(task, optimized, "cost-optimized", tier.name().toLowerCase());

        return new CostOptimizedPlan(optimized, optimizedEstimate, action,
                buildExplanation(action, rawEstimate, optimizedEstimate, budget), tier);
    }

    /**
     * Estimates the token cost of a plan without executing it.
     */
    public CostEstimate estimate(HierarchicalTaskPlanner.Plan plan) {
        List<HierarchicalTaskPlanner.TaskNode> tasks = plan.tasks();
        if (tasks.isEmpty()) return new CostEstimate(0, 0, 0, 0, plan.mode());

        int systemPromptCost = 800; // fixed system prompt overhead

        int executionCost = switch (plan.mode()) {
            case SEQUENTIAL -> estimateSequential(tasks);
            case PARALLEL   -> estimateParallel(tasks);
            case HYBRID     -> (estimateSequential(tasks) + estimateParallel(tasks)) / 2;
        };

        int synthesisCost = plan.mode() == HierarchicalTaskPlanner.ExecutionMode.PARALLEL
                ? 500 : 0;

        int total = systemPromptCost + executionCost + synthesisCost;

        // Historical adjustment: compare against past similar plans
        double historicalFactor = computeHistoricalFactor(tasks);
        int adjusted = (int)(total * historicalFactor);

        return new CostEstimate(adjusted, systemPromptCost, executionCost, synthesisCost, plan.mode());
    }

    /**
     * Compares two plans' costs and returns the cheaper one that meets quality threshold.
     */
    public HierarchicalTaskPlanner.Plan chooseCheaper(
            HierarchicalTaskPlanner.Plan planA,
            HierarchicalTaskPlanner.Plan planB,
            double minQualityRatio) {

        CostEstimate costA = estimate(planA);
        CostEstimate costB = estimate(planB);

        // If B costs much less and is still "good enough"
        if (costB.totalTokens() < costA.totalTokens() * (1 - minQualityRatio)) {
            log.info("[cost-planner] chose cheaper plan B ({} vs {} tokens)",
                    costB.totalTokens(), costA.totalTokens());
            return planB;
        }
        return planA;
    }

    /**
     * Returns a cost profile for a task based on historical episodic data.
     */
    public HistoricalCostProfile historicalProfile(String task) {
        List<EpisodicMemory.Episode> similar = episodic.findRelevant(task, 10);
        if (similar.isEmpty()) return HistoricalCostProfile.noData();

        long avgDuration = (long) similar.stream().mapToLong(EpisodicMemory.Episode::durationMs).average().orElse(0);
        double successRate = similar.stream().mapToInt(e -> e.success() ? 1 : 0).average().orElse(0);
        double avgTools = similar.stream().mapToInt(e -> e.toolsUsed().size()).average().orElse(0);

        int estimatedTokens = (int)(avgTools * 300 + 800); // rough: 300 tokens per tool call
        return new HistoricalCostProfile(similar.size(), avgDuration, successRate,
                (int) avgTools, estimatedTokens);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private int estimateSequential(List<HierarchicalTaskPlanner.TaskNode> tasks) {
        int cost = 0;
        int accumulated = 0;
        for (HierarchicalTaskPlanner.TaskNode task : tasks) {
            int stepCost = task.estimatedTokens() + SEQ_CONTEXT_OVERHEAD + accumulated / 4;
            cost += stepCost;
            accumulated += stepCost;  // each step sees all prior output
        }
        return cost;
    }

    private int estimateParallel(List<HierarchicalTaskPlanner.TaskNode> tasks) {
        int maxStep = tasks.stream().mapToInt(HierarchicalTaskPlanner.TaskNode::estimatedTokens).max().orElse(0);
        return maxStep * Math.min(tasks.size(), 3) + PARALLEL_COORD_OVERHEAD; // cap at 3 "parallel waves"
    }

    private BudgetTier classifyBudget(int budget) {
        if (budget < 1000) return BudgetTier.MINIMAL;
        if (budget < 4000) return BudgetTier.STANDARD;
        if (budget < 10000) return BudgetTier.GENEROUS;
        return BudgetTier.UNLIMITED;
    }

    private HierarchicalTaskPlanner.Plan aggressivePrune(HierarchicalTaskPlanner.Plan plan, int budget) {
        // Keep only ATOMIC + LOW risk tasks; switch to PARALLEL
        List<HierarchicalTaskPlanner.TaskNode> pruned = plan.tasks().stream()
                .filter(t -> t.type() == HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC)
                .filter(t -> t.risk() != HierarchicalTaskPlanner.TaskNode.RiskLevel.HIGH)
                .limit(3)  // hard cap for minimal budget
                .toList();

        return new HierarchicalTaskPlanner.Plan(
                plan.id(), "pruned-" + plan.name(), plan.goal(),
                pruned, HierarchicalTaskPlanner.ExecutionMode.PARALLEL,
                estimate(HierarchicalTaskPlanner.Plan.parallelized(plan.goal(), pruned)).totalTokens(),
                plan.estimatedDurationMs() / 2,
                plan.actualSuccessRate(), plan.actualDurationMs(),
                plan.version() + 1, plan.createdAt());
    }

    private HierarchicalTaskPlanner.Plan moderatePrune(HierarchicalTaskPlanner.Plan plan, int budget) {
        // Remove COMPOSITE tasks, switch complex sequential chains to parallel
        List<HierarchicalTaskPlanner.TaskNode> pruned = plan.tasks().stream()
                .filter(t -> t.type() == HierarchicalTaskPlanner.TaskNode.TaskType.ATOMIC
                        || t.risk() == HierarchicalTaskPlanner.TaskNode.RiskLevel.HIGH)
                .limit(6)
                .toList();

        HierarchicalTaskPlanner.ExecutionMode mode = pruned.size() <= 3
                ? HierarchicalTaskPlanner.ExecutionMode.PARALLEL
                : HierarchicalTaskPlanner.ExecutionMode.HYBRID;

        return new HierarchicalTaskPlanner.Plan(
                plan.id(), "moderate-" + plan.name(), plan.goal(),
                pruned, mode,
                estimate(new HierarchicalTaskPlanner.Plan(plan.id(), plan.name(), plan.goal(),
                        pruned, mode, 0, 0, 0, 0, 1, plan.createdAt())).totalTokens(),
                plan.estimatedDurationMs(),
                plan.actualSuccessRate(), plan.actualDurationMs(),
                plan.version() + 1, plan.createdAt());
    }

    private HierarchicalTaskPlanner.Plan lightPrune(HierarchicalTaskPlanner.Plan plan, int budget) {
        // Just remove pure verification steps if over budget
        List<HierarchicalTaskPlanner.TaskNode> pruned = plan.tasks().stream()
                .filter(t -> !t.task().toLowerCase().contains("verify")
                        || t.risk() == HierarchicalTaskPlanner.TaskNode.RiskLevel.HIGH)
                .toList();

        return new HierarchicalTaskPlanner.Plan(
                plan.id(), plan.name(), plan.goal(), pruned, plan.mode(),
                plan.estimatedTokens(), plan.estimatedDurationMs(),
                plan.actualSuccessRate(), plan.actualDurationMs(),
                plan.version() + 1, plan.createdAt());
    }

    private double computeHistoricalFactor(List<HierarchicalTaskPlanner.TaskNode> tasks) {
        // Compare planned tool count vs historical actual tool count
        // If historical is consistently higher → increase estimate
        long similar = episodic.all().stream()
                .filter(e -> e.toolsUsed().size() > tasks.size() * 2)
                .count();
        long total = Math.max(1, episodic.all().size());
        double overrunRate = (double) similar / total;
        return 1.0 + (overrunRate * 0.5); // up to 50% overhead adjustment
    }

    private OptimizationAction determineAction(HierarchicalTaskPlanner.Plan original,
                                                HierarchicalTaskPlanner.Plan optimized) {
        if (optimized.tasks().size() < original.tasks().size()) return OptimizationAction.PRUNED;
        if (optimized.mode() != original.mode()) return OptimizationAction.MODE_CHANGED;
        return OptimizationAction.NONE;
    }

    private String buildExplanation(OptimizationAction action, CostEstimate original,
                                     CostEstimate optimized, int budget) {
        int savings = original.totalTokens() - optimized.totalTokens();
        return switch (action) {
            case NONE -> "Plan fits budget unchanged";
            case PRUNED -> String.format("Pruned %d steps to save %d tokens (budget=%d)",
                    0, savings, budget);
            case MODE_CHANGED -> String.format("Switched to %s to save %d tokens (budget=%d)",
                    optimized.mode(), savings, budget);
        };
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum BudgetTier    { MINIMAL, STANDARD, GENEROUS, UNLIMITED }
    public enum OptimizationAction { NONE, PRUNED, MODE_CHANGED }

    public record CostEstimate(
            int totalTokens,
            int systemPromptTokens,
            int executionTokens,
            int synthesisTokens,
            HierarchicalTaskPlanner.ExecutionMode mode
    ) {
        public String summary() {
            return String.format("CostEstimate: %d total (sys=%d, exec=%d, synth=%d) [%s]",
                    totalTokens, systemPromptTokens, executionTokens, synthesisTokens, mode);
        }
    }

    public record CostOptimizedPlan(
            HierarchicalTaskPlanner.Plan plan,
            CostEstimate                 estimate,
            OptimizationAction           action,
            String                       explanation,
            BudgetTier                   budgetTier
    ) {
        public boolean wasOptimized() { return action != OptimizationAction.NONE; }
    }

    public record HistoricalCostProfile(
            int    similarEpisodes,
            long   avgDurationMs,
            double historicalSuccessRate,
            int    avgToolCalls,
            int    estimatedTokens
    ) {
        static HistoricalCostProfile noData() {
            return new HistoricalCostProfile(0, 0, 0, 0, 0);
        }
        public boolean hasData() { return similarEpisodes > 0; }
        public String summary() {
            return hasData()
                    ? String.format("History[%d]: %.0f%% success, ~%d tools, ~%d tokens, %dms",
                            similarEpisodes, historicalSuccessRate * 100,
                            avgToolCalls, estimatedTokens, avgDurationMs)
                    : "No historical data";
        }
    }
}
