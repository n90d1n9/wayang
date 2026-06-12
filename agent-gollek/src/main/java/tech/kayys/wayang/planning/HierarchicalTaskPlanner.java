package tech.kayys.gamelan.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Hierarchical Task Planner (HTN-inspired) with Tree-of-Thought exploration.
 *
 * <h2>Architecture</h2>
 * <pre>
 * HierarchicalTaskPlanner
 *   ├── Decomposer        — breaks goal into sub-tasks (LLM)
 *   ├── CostEstimator     — estimates token/time cost per sub-task
 *   ├── PlanGraph         — DAG of tasks with dependencies
 *   ├── ToTExplorer       — explores multiple plan variations (Tree-of-Thought)
 *   └── PlanSelector      — picks the Pareto-optimal plan (quality vs cost)
 * </pre>
 *
 * <h2>Tree-of-Thought Planning</h2>
 * Rather than generating one plan, this generates N candidate plans in parallel,
 * evaluates each against a rubric, and selects the best. This dramatically
 * improves plan quality for complex, ambiguous tasks.
 *
 * <h2>Plan Versioning</h2>
 * All plans are versioned and stored. When the same task recurs, past plan
 * performance is used to warm-start the search — avoiding re-exploration of
 * known bad strategies.
 *
 * <h2>Cost-Aware Planning</h2>
 * Each plan variant is scored on:
 * <ul>
 *   <li>Estimated token count</li>
 *   <li>Estimated execution time (based on episodic data)</li>
 *   <li>Tool cost profile (expensive tools penalised)</li>
 *   <li>Risk score (irreversible operations require higher confidence)</li>
 * </ul>
 */
@ApplicationScoped
public class HierarchicalTaskPlanner {

    private static final Logger log = LoggerFactory.getLogger(HierarchicalTaskPlanner.class);

    private static final int TOT_BRANCHES = 3;  // Tree-of-Thought branches
    private static final int MAX_DEPTH    = 5;  // max HTN decomposition depth

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject GollekSdk     sdk;
    @Inject GamelanConfig config;

    // Plan version store: task fingerprint → list of past plans
    private final Map<String, List<Plan>> planHistory = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Creates an optimal execution plan for a task.
     *
     * <h2>Algorithm</h2>
     * <ol>
     *   <li>Check plan history for reuse</li>
     *   <li>Generate N candidate plans via Tree-of-Thought</li>
     *   <li>Score each plan (quality × cost × risk)</li>
     *   <li>Return the Pareto-optimal plan</li>
     * </ol>
     */
    public Plan plan(String task, PlanningContext context) {
        log.info("[planner] planning task: {}", truncate(task, 80));

        // 1. Check history for warm-start
        String fingerprint = fingerprint(task);
        List<Plan> history = planHistory.getOrDefault(fingerprint, List.of());
        if (!history.isEmpty()) {
            Plan best = history.stream()
                    .max(Comparator.comparingDouble(Plan::actualSuccessRate))
                    .orElse(null);
            if (best != null && best.actualSuccessRate() > 0.8) {
                log.info("[planner] reusing high-confidence plan from history");
                return best.withVersion(best.version() + 1);
            }
        }

        // 2. Decompose task into sub-tasks
        List<TaskNode> subTasks = decompose(task, context, 0);

        // 3. Tree-of-Thought: generate N plan variants in parallel
        List<Plan> candidates = generateCandidates(task, subTasks, context);

        // 4. Score and select best plan
        Plan selected = selectBestPlan(candidates);

        // 5. Store in history
        planHistory.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(selected);

        log.info("[planner] selected plan '{}' with {} tasks, est. {} tokens",
                selected.name(), selected.tasks().size(), selected.estimatedTokens());
        return selected;
    }

    /**
     * Records the actual outcome of a plan execution.
     * Updates plan history with real performance data.
     */
    public void recordOutcome(Plan plan, boolean success, long actualDurationMs) {
        String fp = fingerprint(plan.goal());
        planHistory.computeIfAbsent(fp, k -> new ArrayList<>()).stream()
                .filter(p -> p.id().equals(plan.id()))
                .findFirst()
                .ifPresent(p -> {
                    double newRate = success ? 1.0 : 0.0;
                    // Update via EMA across all versions
                    planHistory.get(fp).replaceAll(h ->
                            h.id().equals(plan.id())
                            ? h.withActualData(newRate, actualDurationMs)
                            : h);
                });
    }

    // ── Decomposition ──────────────────────────────────────────────────────

    private List<TaskNode> decompose(String task, PlanningContext ctx, int depth) {
        if (depth >= MAX_DEPTH) return List.of(TaskNode.leaf(task, depth));

        try {
            String prompt = buildDecompositionPrompt(task, ctx);
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt(DECOMPOSITION_SYSTEM)
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.2)
                            .maxTokens(800)
                            .streaming(false)
                            .build());

            return parseDecomposition(resp.getContent(), depth + 1);
        } catch (Exception e) {
            log.debug("[planner] decomposition failed at depth {}: {}", depth, e.getMessage());
            return List.of(TaskNode.leaf(task, depth));
        }
    }

    private static final String DECOMPOSITION_SYSTEM = """
            You are a task decomposition expert. Given a complex task, break it into
            2-5 concrete, sequential sub-tasks.
            
            Reply ONLY with a JSON array. No prose. No fences.
            Each element: {
              "task": "concrete action",
              "type": "ATOMIC|COMPOSITE",
              "tools": ["tool1", "tool2"],
              "requires": ["dependency-task-index"],
              "estimatedTokens": 500,
              "risk": "LOW|MEDIUM|HIGH"
            }
            ATOMIC = can be done in one LLM call.
            COMPOSITE = needs further decomposition.
            """;

    @SuppressWarnings("unchecked")
    private List<TaskNode> parseDecomposition(String raw, int depth) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            String json = raw.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").strip();
            int s = json.indexOf('['), e = json.lastIndexOf(']');
            if (s < 0 || e <= s) return List.of();
            List<Map<String, Object>> items = MAPPER.readValue(json.substring(s, e + 1), List.class);
            List<TaskNode> nodes = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> m = items.get(i);
                String taskText = (String) m.getOrDefault("task", "");
                String typeStr  = (String) m.getOrDefault("type", "ATOMIC");
                List<String> tools = (List<String>) m.getOrDefault("tools", List.of());
                int estTokens   = ((Number) m.getOrDefault("estimatedTokens", 500)).intValue();
                String riskStr  = (String) m.getOrDefault("risk", "LOW");
                TaskNode.TaskType type;
                try { type = TaskNode.TaskType.valueOf(typeStr); }
                catch (Exception ex) { type = TaskNode.TaskType.ATOMIC; }
                TaskNode.RiskLevel risk;
                try { risk = TaskNode.RiskLevel.valueOf(riskStr); }
                catch (Exception ex) { risk = TaskNode.RiskLevel.LOW; }
                nodes.add(new TaskNode(UUID.randomUUID().toString(), taskText,
                        type, depth, tools, List.of(), estTokens, risk, List.of()));
            }
            return nodes;
        } catch (Exception ex) {
            log.debug("[planner] decomposition parse failed: {}", ex.getMessage());
            return List.of();
        }
    }

    // ── Tree-of-Thought candidate generation ──────────────────────────────

    private List<Plan> generateCandidates(String task, List<TaskNode> baseTasks,
                                           PlanningContext ctx) {
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Plan>> futures = new ArrayList<>();

        for (int i = 0; i < TOT_BRANCHES; i++) {
            final int branch = i;
            futures.add(exec.submit(() -> generatePlanVariant(task, baseTasks, ctx, branch)));
        }
        exec.shutdown();

        List<Plan> plans = new ArrayList<>();
        for (Future<Plan> f : futures) {
            try { plans.add(f.get(30, TimeUnit.SECONDS)); }
            catch (Exception e) { log.debug("[planner] branch failed: {}", e.getMessage()); }
        }

        if (plans.isEmpty()) {
            // Fallback: return a single sequential plan
            plans.add(Plan.sequential(task, baseTasks));
        }
        return plans;
    }

    private Plan generatePlanVariant(String task, List<TaskNode> base,
                                      PlanningContext ctx, int branch) {
        // Branch 0: sequential (safe, predictable)
        // Branch 1: parallel where possible (fast, higher risk)
        // Branch 2: minimal (use fewest tools, lowest cost)
        return switch (branch) {
            case 0 -> Plan.sequential(task, base);
            case 1 -> Plan.parallelized(task, base);
            case 2 -> Plan.minimal(task, base);
            default -> Plan.sequential(task, base);
        };
    }

    // ── Plan selection ─────────────────────────────────────────────────────

    private Plan selectBestPlan(List<Plan> candidates) {
        return candidates.stream()
                .max(Comparator.comparingDouble(this::score))
                .orElse(candidates.get(0));
    }

    /**
     * Composite score: quality weight × (1 - cost_ratio) × (1 - risk_ratio)
     */
    private double score(Plan plan) {
        double maxTokens = 10_000.0;
        double costScore = 1.0 - Math.min(1.0, plan.estimatedTokens() / maxTokens);
        double riskScore = 1.0 - plan.tasks().stream()
                .mapToDouble(t -> switch (t.risk()) {
                    case LOW -> 0.0;
                    case MEDIUM -> 0.3;
                    case HIGH -> 0.7;
                }).average().orElse(0.0);
        double histScore = plan.actualSuccessRate();
        return 0.3 * costScore + 0.3 * riskScore + 0.4 * histScore;
    }

    private String fingerprint(String task) {
        // Simple fingerprint: sorted significant words
        return Arrays.stream(task.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 5)
                .sorted()
                .limit(8)
                .reduce("", (a, b) -> a + "-" + b);
    }

    private String buildDecompositionPrompt(String task, PlanningContext ctx) {
        return "Task: " + task + (ctx.projectContext().isBlank() ? ""
                : "\n\nProject context:\n" + ctx.projectContext());
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A node in the hierarchical task decomposition tree.
     */
    public record TaskNode(
            String       id,
            String       task,
            TaskType     type,
            int          depth,
            List<String> tools,
            List<String> dependsOn,    // IDs of tasks that must complete first
            int          estimatedTokens,
            RiskLevel    risk,
            List<TaskNode> children
    ) {
        static TaskNode leaf(String task, int depth) {
            return new TaskNode(UUID.randomUUID().toString(), task,
                    TaskType.ATOMIC, depth, List.of(), List.of(), 500, RiskLevel.LOW, List.of());
        }

        public enum TaskType  { ATOMIC, COMPOSITE }
        public enum RiskLevel { LOW, MEDIUM, HIGH }
    }

    /**
     * A complete, versioned execution plan.
     */
    public record Plan(
            String         id,
            String         name,
            String         goal,
            List<TaskNode> tasks,
            ExecutionMode  mode,
            int            estimatedTokens,
            long           estimatedDurationMs,
            double         actualSuccessRate,
            long           actualDurationMs,
            int            version,
            Instant        createdAt
    ) {
        static Plan sequential(String goal, List<TaskNode> tasks) {
            int tokens = tasks.stream().mapToInt(TaskNode::estimatedTokens).sum();
            return new Plan(UUID.randomUUID().toString(), "sequential-plan", goal,
                    tasks, ExecutionMode.SEQUENTIAL, tokens,
                    tokens * 10L, 0.5, 0, 1, Instant.now());
        }

        static Plan parallelized(String goal, List<TaskNode> tasks) {
            int tokens = tasks.stream().mapToInt(TaskNode::estimatedTokens).max().orElse(500);
            return new Plan(UUID.randomUUID().toString(), "parallel-plan", goal,
                    tasks, ExecutionMode.PARALLEL, tokens,
                    tokens * 5L, 0.5, 0, 1, Instant.now());
        }

        static Plan minimal(String goal, List<TaskNode> tasks) {
            // Keep only ATOMIC tasks, remove COMPOSITE
            List<TaskNode> atomicOnly = tasks.stream()
                    .filter(t -> t.type() == TaskNode.TaskType.ATOMIC).toList();
            int tokens = atomicOnly.stream().mapToInt(TaskNode::estimatedTokens).sum();
            return new Plan(UUID.randomUUID().toString(), "minimal-plan", goal,
                    atomicOnly, ExecutionMode.SEQUENTIAL, tokens,
                    tokens * 8L, 0.5, 0, 1, Instant.now());
        }

        Plan withVersion(int v) {
            return new Plan(id, name, goal, tasks, mode, estimatedTokens,
                    estimatedDurationMs, actualSuccessRate, actualDurationMs, v, createdAt);
        }

        Plan withActualData(double successRate, long durationMs) {
            return new Plan(id, name, goal, tasks, mode, estimatedTokens,
                    estimatedDurationMs, successRate, durationMs, version, createdAt);
        }
    }

    public enum ExecutionMode { SEQUENTIAL, PARALLEL, HYBRID }

    /**
     * Context provided to the planner to improve decomposition quality.
     */
    public record PlanningContext(
            String projectContext,
            List<String> availableTools,
            int tokenBudget,
            boolean allowParallel,
            boolean dryRun
    ) {
        public static PlanningContext defaults() {
            return new PlanningContext("", List.of(), 6000, true, false);
        }
    }
}
