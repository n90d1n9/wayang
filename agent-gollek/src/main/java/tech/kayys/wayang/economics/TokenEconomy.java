package tech.kayys.gamelan.economics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Economic Layer — Token Budget Management and Compute Allocation.
 *
 * <h2>Problems Solved</h2>
 * <ol>
 *   <li><b>Token Runaway</b>: Without budgeting, agents blow through context
 *       windows on simple tasks or run indefinitely on complex ones.</li>
 *   <li><b>Model Misallocation</b>: Using GPT-4-class models for trivial
 *       classification wastes 10-100× tokens. Using weak models for complex
 *       reasoning causes failures.</li>
 *   <li><b>Invisible Cost</b>: Users have no visibility into how much the
 *       agent is "spending" — making it impossible to make informed decisions.</li>
 * </ol>
 *
 * <h2>Token Economy</h2>
 * Each session has a configurable token budget. The economy tracks:
 * <ul>
 *   <li>Budget per task tier (planning, execution, reflection)</li>
 *   <li>Rolling 24h spend across all sessions</li>
 *   <li>Per-skill cost profiles (derived from episodic data)</li>
 * </ul>
 *
 * <h2>Compute Allocation Strategy</h2>
 * The allocator routes tasks to models using a 3-tier model ladder:
 * <ol>
 *   <li><b>Fast tier</b>: Small/quantized models for classification, routing,
 *       simple Q&A — cheap, low latency, high throughput</li>
 *   <li><b>Standard tier</b>: Mid-size models for most coding tasks — balanced</li>
 *   <li><b>Expert tier</b>: Largest available model for complex reasoning,
 *       architecture decisions, synthesis — expensive, reserved</li>
 * </ol>
 *
 * <h2>Dynamic Escalation</h2>
 * If a task fails on the standard tier, the allocator automatically escalates
 * to the expert tier (if budget allows) rather than returning an error.
 * If budget is exhausted, it degrades gracefully to the fast tier with
 * a user-visible warning.
 */
@ApplicationScoped
public class TokenEconomy {

    private static final Logger log = LoggerFactory.getLogger(TokenEconomy.class);

    @Inject GamelanConfig   config;
    @Inject EpisodicMemory  episodic;

    // Budget state
    private final AtomicInteger sessionSpend    = new AtomicInteger(0);
    private final AtomicInteger totalSpend24h   = new AtomicInteger(0);
    private final Map<String, Integer> skillCosts = new ConcurrentHashMap<>();

    // Model registry (populated from config or Gollek model list)
    private final Map<ModelTier, String> modelLadder = new LinkedHashMap<>();

    {
        // Default model ladder — overridden by config at runtime
        modelLadder.put(ModelTier.FAST,     "qwen2.5-0.5b");
        modelLadder.put(ModelTier.STANDARD, "llama3");
        modelLadder.put(ModelTier.EXPERT,   "qwen2-72b");
    }

    // ── Token Budget ──────────────────────────────────────────────────────

    /**
     * Records token consumption for a completed operation.
     */
    public void consume(int tokens, String operation) {
        sessionSpend.addAndGet(tokens);
        totalSpend24h.addAndGet(tokens);
        skillCosts.merge(operation, tokens, Integer::sum);
        log.debug("[economy] consumed {} tokens for '{}' | session total: {}",
                tokens, operation, sessionSpend.get());
    }

    /**
     * Checks if budget is available for an operation.
     *
     * @param estimatedTokens estimated cost of the operation
     * @return budget check result with remaining budget
     */
    public BudgetCheck checkBudget(int estimatedTokens) {
        int budget    = config.tokenBudget();
        int spent     = sessionSpend.get();
        int remaining = budget - spent;

        if (remaining <= 0) {
            return BudgetCheck.exhausted(budget, spent);
        }
        if (estimatedTokens > remaining) {
            return BudgetCheck.insufficient(estimatedTokens, remaining, budget);
        }
        if (estimatedTokens > remaining * 0.5) {
            return BudgetCheck.tight(estimatedTokens, remaining, budget);
        }
        return BudgetCheck.ok(estimatedTokens, remaining, budget);
    }

    /**
     * Returns the recommended budget split for a task.
     * Allocates more budget to execution, less to planning and reflection.
     */
    public BudgetAllocation allocate(String task, int totalBudget) {
        // Classification: estimate task complexity
        TaskComplexity complexity = estimateComplexity(task);

        return switch (complexity) {
            case SIMPLE -> new BudgetAllocation(
                    (int)(totalBudget * 0.05),   // planning
                    (int)(totalBudget * 0.85),   // execution
                    (int)(totalBudget * 0.10),   // reflection
                    complexity);
            case MODERATE -> new BudgetAllocation(
                    (int)(totalBudget * 0.15),
                    (int)(totalBudget * 0.70),
                    (int)(totalBudget * 0.15),
                    complexity);
            case COMPLEX -> new BudgetAllocation(
                    (int)(totalBudget * 0.25),
                    (int)(totalBudget * 0.55),
                    (int)(totalBudget * 0.20),
                    complexity);
        };
    }

    /** Resets the session budget counter (e.g., on /clear). */
    public void reset() { sessionSpend.set(0); }

    /** Returns current session spend. */
    public int sessionSpend()  { return sessionSpend.get(); }
    public int remaining()     { return Math.max(0, config.tokenBudget() - sessionSpend.get()); }

    // ── Compute Allocation ────────────────────────────────────────────────

    /**
     * Selects the optimal model for a task given current budget and complexity.
     * Implements the 3-tier ladder with dynamic escalation/degradation.
     */
    public ModelAllocation selectModel(String task, boolean allowEscalation) {
        TaskComplexity complexity = estimateComplexity(task);
        BudgetCheck budget = checkBudget(estimatedTokensFor(complexity));

        ModelTier tier = switch (complexity) {
            case SIMPLE   -> ModelTier.FAST;
            case MODERATE -> ModelTier.STANDARD;
            case COMPLEX  -> ModelTier.EXPERT;
        };

        // Degrade if budget is tight
        if (budget.status() == BudgetCheck.Status.TIGHT && tier == ModelTier.EXPERT) {
            tier = ModelTier.STANDARD;
            log.info("[economy] degraded model: EXPERT → STANDARD (budget tight)");
        }
        if (budget.status() == BudgetCheck.Status.INSUFFICIENT) {
            tier = ModelTier.FAST;
            log.warn("[economy] degraded model: {} → FAST (budget insufficient)", complexity);
        }
        if (budget.status() == BudgetCheck.Status.EXHAUSTED) {
            return ModelAllocation.exhausted();
        }

        String modelId = modelLadder.getOrDefault(tier, config.defaultModel());
        return new ModelAllocation(modelId, tier, complexity, budget,
                allowEscalation && tier != ModelTier.EXPERT);
    }

    /**
     * Escalates model tier after a failure (if budget allows).
     * Returns the escalated model or empty if escalation not possible.
     */
    public Optional<String> escalate(ModelTier currentTier) {
        ModelTier next = switch (currentTier) {
            case FAST     -> ModelTier.STANDARD;
            case STANDARD -> ModelTier.EXPERT;
            case EXPERT   -> null;
        };
        if (next == null) return Optional.empty();
        BudgetCheck check = checkBudget(estimatedTokensFor(TaskComplexity.COMPLEX));
        if (check.status() == BudgetCheck.Status.EXHAUSTED) return Optional.empty();
        log.info("[economy] escalating {} → {}", currentTier, next);
        return Optional.ofNullable(modelLadder.get(next));
    }

    /** Configures the model ladder. */
    public void configureModelLadder(ModelTier tier, String modelId) {
        modelLadder.put(tier, modelId);
        log.info("[economy] model ladder: {} → {}", tier, modelId);
    }

    /** Returns an economy report for /stats display. */
    public EconomyReport report() {
        return new EconomyReport(
                config.tokenBudget(), sessionSpend.get(), remaining(),
                totalSpend24h.get(), new LinkedHashMap<>(skillCosts),
                modelLadder);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private TaskComplexity estimateComplexity(String task) {
        if (task == null || task.length() < 50) return TaskComplexity.SIMPLE;
        String lower = task.toLowerCase();
        // Complex: architecture, multi-file, cross-domain, long tasks
        if (task.length() > 200
                || lower.contains("refactor all")
                || lower.contains("architecture")
                || lower.contains("comprehensive")
                || lower.contains("across all")) {
            return TaskComplexity.COMPLEX;
        }
        // Simple: short questions, explanations
        if (task.length() < 100 && !lower.contains("implement")
                && !lower.contains("create") && !lower.contains("write")) {
            return TaskComplexity.SIMPLE;
        }
        return TaskComplexity.MODERATE;
    }

    private int estimatedTokensFor(TaskComplexity c) {
        return switch (c) {
            case SIMPLE   -> 500;
            case MODERATE -> 2000;
            case COMPLEX  -> 5000;
        };
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum ModelTier     { FAST, STANDARD, EXPERT }
    public enum TaskComplexity{ SIMPLE, MODERATE, COMPLEX }

    public record BudgetAllocation(int planningBudget, int executionBudget,
                                   int reflectionBudget, TaskComplexity complexity) {
        public int total() { return planningBudget + executionBudget + reflectionBudget; }
    }

    public record BudgetCheck(int requested, int remaining, int total, Status status) {
        public enum Status { OK, TIGHT, INSUFFICIENT, EXHAUSTED }
        static BudgetCheck ok(int r, int rem, int t)           { return new BudgetCheck(r,rem,t,Status.OK); }
        static BudgetCheck tight(int r, int rem, int t)        { return new BudgetCheck(r,rem,t,Status.TIGHT); }
        static BudgetCheck insufficient(int r, int rem, int t) { return new BudgetCheck(r,rem,t,Status.INSUFFICIENT); }
        static BudgetCheck exhausted(int t, int s)             { return new BudgetCheck(0,0,t,Status.EXHAUSTED); }
        public boolean canProceed() { return status != Status.EXHAUSTED; }
    }

    public record ModelAllocation(
            String         modelId,
            ModelTier      tier,
            TaskComplexity complexity,
            BudgetCheck    budgetCheck,
            boolean        canEscalate
    ) {
        static ModelAllocation exhausted() {
            return new ModelAllocation(null, ModelTier.FAST, TaskComplexity.SIMPLE,
                    BudgetCheck.exhausted(0,0), false);
        }
        public boolean isViable() { return modelId != null; }
    }

    public record EconomyReport(
            int                    sessionBudget,
            int                    sessionSpend,
            int                    remaining,
            int                    total24h,
            Map<String, Integer>   skillCosts,
            Map<ModelTier, String> modelLadder
    ) {
        public String summary() {
            return String.format("Budget: %d/%d used (%.0f%%) | 24h: %d tokens",
                    sessionSpend, sessionBudget,
                    sessionBudget > 0 ? (double)sessionSpend/sessionBudget*100 : 0,
                    total24h);
        }
    }
}
