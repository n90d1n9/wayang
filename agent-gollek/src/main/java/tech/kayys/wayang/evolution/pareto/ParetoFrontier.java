package tech.kayys.gamelan.evolution.pareto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Pareto Frontier Tracker — multi-objective optimization for skill evolution.
 *
 * <h2>Why Multi-Objective</h2>
 * Skill quality cannot be reduced to a single number. A skill that is:
 * <ul>
 *   <li>Fast but wrong → useless</li>
 *   <li>Correct but slow → frustrating in interactive use</li>
 *   <li>Correct and fast but expensive → not viable at scale</li>
 * </ul>
 * The Pareto frontier captures the set of solutions that are NOT dominated —
 * i.e., no other solution is better on ALL objectives simultaneously.
 *
 * <h2>Objectives Tracked</h2>
 * <pre>
 * Maximize:
 *   quality_score    — output correctness (0.0-1.0, from benchmark rubric)
 *   success_rate     — fraction of tasks completed without error
 *
 * Minimize:
 *   latency_ms       — wall-clock execution time
 *   token_cost       — total tokens consumed
 *   tool_calls       — number of tool invocations (fewer = simpler)
 * </pre>
 *
 * <h2>Dominance Rule</h2>
 * Solution A dominates Solution B iff:
 * <ul>
 *   <li>A is at least as good as B on ALL objectives, AND</li>
 *   <li>A is strictly better than B on at least ONE objective</li>
 * </ul>
 *
 * <h2>Acceptance Policy</h2>
 * A new skill version is accepted ONLY if it is Pareto-non-dominated by all
 * existing frontier members. This prevents regression: you can improve speed
 * without worsening quality, but you cannot improve speed AT THE COST of quality.
 *
 * <h2>Persistence</h2>
 * The frontier is persisted to {@code ~/.gamelan/evolution/pareto/{skill}.json}
 * so evolution history survives restarts. This enables long-running autonomous
 * optimization (the AVO 7-day GPU kernel optimization scenario).
 */
public final class ParetoFrontier {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String                    skillName;
    private final List<ParetoPoint>         frontier;
    private final List<ParetoPoint>         allHistory;
    private final Path                      persistPath;

    public ParetoFrontier(String skillName) {
        this.skillName   = skillName;
        this.frontier    = new CopyOnWriteArrayList<>();
        this.allHistory  = new CopyOnWriteArrayList<>();
        this.persistPath = Path.of(System.getProperty("user.home"),
                ".gamelan", "evolution", "pareto", skillName + ".json");
        load();
    }

    // ── Core operations ────────────────────────────────────────────────────

    /**
     * Attempts to add a new point to the Pareto frontier.
     *
     * @param point the candidate skill evaluation metrics
     * @return the result: ACCEPTED (non-dominated), DOMINATED (rejected), or DOMINATES (improves frontier)
     */
    public AddResult add(ParetoPoint point) {
        allHistory.add(point);

        // Empty frontier: always accept
        if (frontier.isEmpty()) {
            frontier.add(point);
            persist();
            return new AddResult(AddOutcome.ACCEPTED_FIRST, point, List.of(),
                    "First point — automatically added to empty frontier");
        }

        // Check if any existing point dominates the new one
        List<ParetoPoint> dominators = frontier.stream()
                .filter(existing -> dominates(existing, point))
                .toList();

        if (!dominators.isEmpty()) {
            return new AddResult(AddOutcome.DOMINATED, point, dominators,
                    "Point is dominated by " + dominators.size() + " existing solution(s): " +
                    dominators.stream().map(p -> p.version() + "(" + formatMetrics(p) + ")")
                            .collect(Collectors.joining(", ")));
        }

        // Find existing points that the new point dominates (will be removed)
        List<ParetoPoint> dominated = frontier.stream()
                .filter(existing -> dominates(point, existing))
                .toList();

        // Add the new point
        frontier.add(point);

        // Remove points that are now dominated
        frontier.removeAll(dominated);
        persist();

        if (!dominated.isEmpty()) {
            return new AddResult(AddOutcome.DOMINATES_EXISTING, point, dominated,
                    "New point dominates " + dominated.size() + " existing solution(s) — frontier pruned");
        }

        return new AddResult(AddOutcome.ACCEPTED, point, List.of(),
                "Non-dominated — added to frontier (frontier size: " + frontier.size() + ")");
    }

    /**
     * Returns the current Pareto frontier (non-dominated set).
     */
    public List<ParetoPoint> frontier() { return List.copyOf(frontier); }

    /**
     * Returns the "best" point on the frontier according to a weighted score.
     * Weights default to equal importance across all objectives.
     */
    public Optional<ParetoPoint> bestWeighted(ObjectiveWeights weights) {
        if (frontier.isEmpty()) return Optional.empty();
        return frontier.stream()
                .max(Comparator.comparingDouble(p -> weightedScore(p, weights)));
    }

    /**
     * Returns the point that is best on a single objective.
     */
    public Optional<ParetoPoint> bestOn(Objective obj) {
        if (frontier.isEmpty()) return Optional.empty();
        return switch (obj) {
            case QUALITY_SCORE -> frontier.stream().max(Comparator.comparingDouble(ParetoPoint::qualityScore));
            case SUCCESS_RATE  -> frontier.stream().max(Comparator.comparingDouble(ParetoPoint::successRate));
            case LATENCY_MS    -> frontier.stream().min(Comparator.comparingLong(ParetoPoint::latencyMs));
            case TOKEN_COST    -> frontier.stream().min(Comparator.comparingInt(ParetoPoint::tokenCost));
            case TOOL_CALLS    -> frontier.stream().min(Comparator.comparingInt(ParetoPoint::toolCalls));
        };
    }

    /**
     * Returns whether the frontier has improved since the baseline version.
     */
    public boolean hasImproved(String baselineVersion) {
        Optional<ParetoPoint> baseline = allHistory.stream()
                .filter(p -> p.version().equals(baselineVersion)).findFirst();
        if (baseline.isEmpty() || frontier.isEmpty()) return false;
        // Improved if any frontier point dominates or matches the baseline
        return frontier.stream().anyMatch(fp -> !dominates(baseline.get(), fp));
    }

    /**
     * Returns a text report of the current frontier suitable for logging/display.
     */
    public String report() {
        if (frontier.isEmpty()) return "Pareto frontier for '" + skillName + "' is empty";

        int w = skillName.length() + 20;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Pareto Frontier: %s (%d points, %d history)%n",
                skillName, frontier.size(), allHistory.size()));
        sb.append(String.format("  %-12s %8s %8s %10s %10s %8s%n",
                "version", "quality", "success", "latency_ms", "tokens", "tools"));
        sb.append("  " + "-".repeat(62) + "\n");

        frontier.stream()
                .sorted(Comparator.comparingDouble(ParetoPoint::qualityScore).reversed())
                .forEach(p -> sb.append(String.format(
                        "  %-12s %8.3f %8.3f %10d %10d %8d%n",
                        p.version(), p.qualityScore(), p.successRate(),
                        p.latencyMs(), p.tokenCost(), p.toolCalls())));
        return sb.toString();
    }

    public List<ParetoPoint> history()  { return List.copyOf(allHistory); }
    public int frontierSize()           { return frontier.size(); }
    public boolean isEmpty()            { return frontier.isEmpty(); }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Dominance predicate: a dominates b iff a is at least as good on all objectives
     * and strictly better on at least one.
     *
     * Note the objective directions:
     * - quality_score, success_rate: MAXIMIZE (higher = better)
     * - latency_ms, token_cost, tool_calls: MINIMIZE (lower = better)
     */
    private boolean dominates(ParetoPoint a, ParetoPoint b) {
        // At least as good on all objectives
        boolean atleastAsGood =
                a.qualityScore() >= b.qualityScore() - 1e-9 &&
                a.successRate()  >= b.successRate()  - 1e-9 &&
                a.latencyMs()    <= b.latencyMs()    + 1 &&  // +1ms tolerance
                a.tokenCost()    <= b.tokenCost()    + 10 && // +10 token tolerance
                a.toolCalls()    <= b.toolCalls();

        if (!atleastAsGood) return false;

        // Strictly better on at least one
        return a.qualityScore() > b.qualityScore() + 1e-9 ||
               a.successRate()  > b.successRate()  + 1e-9 ||
               a.latencyMs()    < b.latencyMs()    - 1    ||
               a.tokenCost()    < b.tokenCost()    - 10   ||
               a.toolCalls()    < b.toolCalls();
    }

    private double weightedScore(ParetoPoint p, ObjectiveWeights w) {
        // Normalize latency and token cost to [0,1] using frontier bounds
        long maxLatency = frontier.stream().mapToLong(ParetoPoint::latencyMs).max().orElse(1);
        int  maxTokens  = frontier.stream().mapToInt(ParetoPoint::tokenCost).max().orElse(1);
        int  maxTools   = frontier.stream().mapToInt(ParetoPoint::toolCalls).max().orElse(1);

        double latNorm   = maxLatency > 0 ? 1.0 - (double) p.latencyMs() / maxLatency : 1.0;
        double tokenNorm = maxTokens  > 0 ? 1.0 - (double) p.tokenCost() / maxTokens  : 1.0;
        double toolNorm  = maxTools   > 0 ? 1.0 - (double) p.toolCalls() / maxTools   : 1.0;

        return w.quality()  * p.qualityScore() +
               w.success()  * p.successRate()  +
               w.latency()  * latNorm           +
               w.tokens()   * tokenNorm         +
               w.tools()    * toolNorm;
    }

    private String formatMetrics(ParetoPoint p) {
        return String.format("q=%.2f,s=%.2f,l=%d,t=%d", p.qualityScore(), p.successRate(),
                p.latencyMs(), p.tokenCost());
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(persistPath)) return;
        try {
            ParetoFrontierData data = MAPPER.readValue(persistPath.toFile(), ParetoFrontierData.class);
            frontier.addAll(data.frontier());
            allHistory.addAll(data.history());
        } catch (IOException e) {
            // Corrupted file: start fresh
        }
    }

    private void persist() {
        try {
            Files.createDirectories(persistPath.getParent());
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(persistPath.toFile(),
                          new ParetoFrontierData(skillName, List.copyOf(frontier), List.copyOf(allHistory)));
        } catch (IOException ignored) {}
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A single evaluated point in the multi-objective space.
     */
    public record ParetoPoint(
            String   version,        // skill version identifier
            double   qualityScore,   // 0.0–1.0 (benchmark rubric score)
            double   successRate,    // 0.0–1.0 (fraction of tasks completed)
            long     latencyMs,      // wall-clock execution time
            int      tokenCost,      // total tokens consumed
            int      toolCalls,      // number of tool invocations
            String   notes,          // free-form annotation
            Instant  evaluatedAt
    ) {
        /** Creates a new version by inheriting all metrics and overriding specified ones. */
        public ParetoPoint withQuality(double q) {
            return new ParetoPoint(version, q, successRate, latencyMs, tokenCost, toolCalls, notes, Instant.now());
        }
        public ParetoPoint withLatency(long ms) {
            return new ParetoPoint(version, qualityScore, successRate, ms, tokenCost, toolCalls, notes, Instant.now());
        }
    }

    public record ObjectiveWeights(double quality, double success, double latency, double tokens, double tools) {
        public static ObjectiveWeights equal() { return new ObjectiveWeights(0.2, 0.2, 0.2, 0.2, 0.2); }
        public static ObjectiveWeights qualityFirst() { return new ObjectiveWeights(0.5, 0.3, 0.1, 0.05, 0.05); }
        public static ObjectiveWeights costFirst()    { return new ObjectiveWeights(0.2, 0.2, 0.2, 0.3, 0.1); }
        public static ObjectiveWeights speedFirst()   { return new ObjectiveWeights(0.2, 0.2, 0.4, 0.1, 0.1); }

        void validate() {
            double sum = quality + success + latency + tokens + tools;
            if (Math.abs(sum - 1.0) > 0.01)
                throw new IllegalArgumentException("Weights must sum to 1.0, got: " + sum);
        }
    }

    public enum Objective { QUALITY_SCORE, SUCCESS_RATE, LATENCY_MS, TOKEN_COST, TOOL_CALLS }

    public enum AddOutcome { ACCEPTED_FIRST, ACCEPTED, DOMINATES_EXISTING, DOMINATED }

    public record AddResult(
            AddOutcome      outcome,
            ParetoPoint     point,
            List<ParetoPoint> relatedPoints,
            String          reason
    ) {
        public boolean accepted()  { return outcome != AddOutcome.DOMINATED; }
        public boolean improved()  { return outcome == AddOutcome.DOMINATES_EXISTING; }
    }

    private record ParetoFrontierData(
            String           skillName,
            List<ParetoPoint> frontier,
            List<ParetoPoint> history
    ) {}
}
