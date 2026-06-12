package tech.kayys.gamelan.planning.adaptive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.planning.versioning.PlanVersionStore;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * AdaptivePlanner — a planning strategy that learns from its own execution history.
 *
 * <h2>Core insight</h2>
 * Static planners generate the same plan for the same task regardless of what
 * happened last time. An adaptive planner observes:
 * <ul>
 *   <li>Which plans succeeded and why</li>
 *   <li>Which tool sequences worked for which task types</li>
 *   <li>How estimates compare to actuals (calibration)</li>
 *   <li>Which strategies perform better for specific codebase characteristics</li>
 * </ul>
 * …and adjusts its planning strategy accordingly.
 *
 * <h2>Learning mechanisms</h2>
 * <ol>
 *   <li><b>Plan warm-start</b>: find the historically best plan for this task
 *       type and use it as the starting point (vs generating from scratch)</li>
 *   <li><b>Tool sequence injection</b>: inject proven tool sequences from
 *       {@link tech.kayys.gamelan.memory.hierarchy.ProceduralMemory} into
 *       the plan steps</li>
 *   <li><b>Calibration</b>: if estimated tokens consistently overrun by 40%,
 *       apply a 1.4x correction factor to new estimates</li>
 *   <li><b>Mode selection</b>: if PARALLEL consistently outperforms SEQUENTIAL
 *       for a task type, bias toward PARALLEL</li>
 *   <li><b>Step pruning</b>: if verification steps are consistently skipped
 *       without impacting quality, mark them as optional</li>
 * </ol>
 *
 * <h2>Adaptation rate</h2>
 * Uses exponential moving average (α=0.3) so recent data influences decisions
 * more than old data, but old data is not completely discarded.
 */
@ApplicationScoped
public class AdaptivePlanner {

    private static final Logger log = LoggerFactory.getLogger(AdaptivePlanner.class);

    private static final double EMA_ALPHA          = 0.3;   // learning rate
    private static final int    MIN_DATA_POINTS     = 5;    // min history before adapting
    private static final double CALIBRATION_TRIGGER = 0.25; // >25% overrun triggers correction

    @Inject EpisodicMemory     episodic;
    @Inject PlanVersionStore   versionStore;
    @Inject HierarchicalTaskPlanner htn;

    // Learned parameters — updated from episodic data
    private final Map<String, Double>     tokenCalibration  = new ConcurrentHashMap<>();
    private final Map<String, String>     preferredMode     = new ConcurrentHashMap<>();
    private final Map<String, Double>     taskSuccessRates  = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> executionHistory  = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Generates a plan adapted to historical performance for this task type.
     *
     * @param task         the task description
     * @param taskCategory optional category override (e.g. "code-review", "refactor")
     * @return an adapted plan with historically-informed parameters
     */
    public AdaptedPlan plan(String task, String taskCategory) {
        String category = taskCategory != null ? taskCategory : classifyTask(task);
        updateLearning(category);

        // Step 1: Try warm-start from version history
        Optional<HierarchicalTaskPlanner.Plan> warmStart = findBestHistoricalPlan(task, category);

        // Step 2: Generate fresh plan (or adapt warm-start)
        HierarchicalTaskPlanner.Plan basePlan = warmStart
                .map(p -> adaptFromHistory(p, category))
                .orElseGet(() -> htn.plan(task, null, null));

        // Step 3: Apply calibration corrections
        HierarchicalTaskPlanner.Plan calibrated = applyCalibration(basePlan, category);

        // Step 4: Recommend execution mode based on history
        HierarchicalTaskPlanner.ExecutionMode recommendedMode = recommendMode(category, calibrated);

        // Step 5: Compute confidence score
        double confidence = computeConfidence(category);

        String explanation = buildExplanation(warmStart.isPresent(), category, confidence,
                recommendedMode, calibrated);

        log.info("[adaptive-planner] category='{}' confidence={:.2f} mode={} warm={}",
                category, confidence, recommendedMode, warmStart.isPresent());

        return new AdaptedPlan(calibrated, recommendedMode, category, confidence,
                warmStart.isPresent(), explanation, Instant.now());
    }

    /**
     * Records actual execution results to improve future planning.
     * Call this after every completed task.
     *
     * @param category     the task category this result belongs to
     * @param success      whether the task completed successfully
     * @param actualMs     actual wall-clock duration
     * @param estimatedMs  what was estimated
     * @param actualTokens  actual tokens used
     * @param estimatedTokens what was estimated
     */
    public void recordOutcome(String category, boolean success,
                               long actualMs, long estimatedMs,
                               int actualTokens, int estimatedTokens) {
        // Update success rate via EMA
        double currentRate = taskSuccessRates.getOrDefault(category, 0.5);
        double newRate = EMA_ALPHA * (success ? 1.0 : 0.0) + (1 - EMA_ALPHA) * currentRate;
        taskSuccessRates.put(category, newRate);

        // Update token calibration
        if (estimatedTokens > 0) {
            double overrun = (double) actualTokens / estimatedTokens;
            double currentCal = tokenCalibration.getOrDefault(category, 1.0);
            tokenCalibration.put(category, EMA_ALPHA * overrun + (1 - EMA_ALPHA) * currentCal);
        }

        // Record execution time
        executionHistory.computeIfAbsent(category, k -> new CopyOnWriteArrayList<>())
                        .add(actualMs);

        log.debug("[adaptive-planner] recorded outcome for '{}': success={} tokens={}/{}",
                category, success, actualTokens, estimatedTokens);
    }

    /**
     * Returns a calibration report showing learned parameters per task category.
     */
    public CalibrationReport calibrationReport() {
        Map<String, CategoryStats> stats = new LinkedHashMap<>();

        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(taskSuccessRates.keySet());
        allCategories.addAll(tokenCalibration.keySet());

        allCategories.forEach(cat -> {
            List<Long> history = executionHistory.getOrDefault(cat, List.of());
            stats.put(cat, new CategoryStats(
                    cat,
                    taskSuccessRates.getOrDefault(cat, 0.5),
                    tokenCalibration.getOrDefault(cat, 1.0),
                    preferredMode.getOrDefault(cat, "SEQUENTIAL"),
                    history.size(),
                    history.stream().mapToLong(l -> l).average().orElse(0)));
        });

        return new CalibrationReport(stats, Instant.now());
    }

    /**
     * Resets all learned parameters. Use when switching projects.
     */
    public void resetLearning() {
        tokenCalibration.clear();
        preferredMode.clear();
        taskSuccessRates.clear();
        executionHistory.clear();
        log.info("[adaptive-planner] learning reset");
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void updateLearning(String category) {
        // Pull recent episodes for this category and update learned params
        List<EpisodicMemory.Episode> relevant = episodic.findRelevant(category, 20);
        if (relevant.size() < MIN_DATA_POINTS) return;

        long successes = relevant.stream().filter(EpisodicMemory.Episode::success).count();
        double rate = (double) successes / relevant.size();
        taskSuccessRates.put(category, rate);

        // Determine preferred mode: if avg tool count > 5, PARALLEL tends to be faster
        double avgTools = relevant.stream()
                .mapToInt(e -> e.toolsUsed().size()).average().orElse(0);
        if (relevant.size() >= MIN_DATA_POINTS) {
            preferredMode.put(category, avgTools > 5 ? "PARALLEL" : "SEQUENTIAL");
        }

        log.debug("[adaptive-planner] updated learning for '{}': success={:.0f}% mode={}",
                category, rate * 100, preferredMode.get(category));
    }

    private Optional<HierarchicalTaskPlanner.Plan> findBestHistoricalPlan(String task, String cat) {
        return versionStore.findBest(task)
                .map(PlanVersionStore.PlanVersion::plan);
    }

    private HierarchicalTaskPlanner.Plan adaptFromHistory(
            HierarchicalTaskPlanner.Plan hist, String category) {
        // Keep the historical plan but update its mode to the currently preferred one
        String mode = preferredMode.getOrDefault(category, "SEQUENTIAL");
        HierarchicalTaskPlanner.ExecutionMode newMode;
        try { newMode = HierarchicalTaskPlanner.ExecutionMode.valueOf(mode); }
        catch (IllegalArgumentException e) { newMode = hist.mode(); }

        return new HierarchicalTaskPlanner.Plan(
                hist.id(), "adapted-" + hist.name(), hist.goal(),
                hist.tasks(), newMode, hist.estimatedTokens(),
                averageActualDuration(category, hist.estimatedDurationMs()),
                taskSuccessRates.getOrDefault(category, hist.actualSuccessRate()),
                hist.actualDurationMs(), hist.version() + 1, Instant.now());
    }

    private HierarchicalTaskPlanner.Plan applyCalibration(
            HierarchicalTaskPlanner.Plan plan, String category) {
        double cal = tokenCalibration.getOrDefault(category, 1.0);
        if (Math.abs(cal - 1.0) < CALIBRATION_TRIGGER) return plan; // no correction needed

        int calibratedTokens = (int)(plan.estimatedTokens() * cal);
        log.debug("[adaptive-planner] calibrating tokens: {} → {} (factor={:.2f})",
                plan.estimatedTokens(), calibratedTokens, cal);

        return new HierarchicalTaskPlanner.Plan(
                plan.id(), plan.name(), plan.goal(), plan.tasks(),
                plan.mode(), calibratedTokens, plan.estimatedDurationMs(),
                plan.actualSuccessRate(), plan.actualDurationMs(),
                plan.version(), plan.createdAt());
    }

    private HierarchicalTaskPlanner.ExecutionMode recommendMode(
            String category, HierarchicalTaskPlanner.Plan plan) {
        String preferred = preferredMode.get(category);
        if (preferred == null) return plan.mode();
        try { return HierarchicalTaskPlanner.ExecutionMode.valueOf(preferred); }
        catch (IllegalArgumentException e) { return plan.mode(); }
    }

    private double computeConfidence(String category) {
        List<Long> history = executionHistory.getOrDefault(category, List.of());
        if (history.isEmpty()) return 0.0;
        // Confidence grows with data points, asymptotically approaching 1.0
        return 1.0 - Math.exp(-history.size() / 10.0);
    }

    private long averageActualDuration(String category, long fallback) {
        List<Long> history = executionHistory.getOrDefault(category, List.of());
        if (history.isEmpty()) return fallback;
        return (long) history.stream().mapToLong(l -> l).average().orElse(fallback);
    }

    private String classifyTask(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("test") || lower.contains("coverage")) return "testing";
        if (lower.contains("security") || lower.contains("vuln"))   return "security";
        if (lower.contains("refactor") || lower.contains("clean"))  return "refactoring";
        if (lower.contains("review") || lower.contains("audit"))    return "code-review";
        if (lower.contains("document") || lower.contains("doc"))    return "documentation";
        if (lower.contains("fix") || lower.contains("bug"))         return "bug-fix";
        if (lower.contains("implement") || lower.contains("add"))   return "feature";
        return "general";
    }

    private String buildExplanation(boolean warmStart, String category,
                                     double confidence, HierarchicalTaskPlanner.ExecutionMode mode,
                                     HierarchicalTaskPlanner.Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("AdaptivePlan[").append(category).append("]: ");
        if (warmStart) sb.append("warm-started from historical success. ");
        sb.append(String.format("confidence=%.0f%%, mode=%s, steps=%d",
                confidence * 100, mode, plan.tasks().size()));
        double cal = tokenCalibration.getOrDefault(category, 1.0);
        if (Math.abs(cal - 1.0) >= CALIBRATION_TRIGGER) {
            sb.append(String.format(", token_correction=%.0f%%", (cal - 1.0) * 100));
        }
        return sb.toString();
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record AdaptedPlan(
            HierarchicalTaskPlanner.Plan         plan,
            HierarchicalTaskPlanner.ExecutionMode recommendedMode,
            String                               category,
            double                               confidence,
            boolean                              isWarmStart,
            String                               explanation,
            Instant                              createdAt
    ) {
        public boolean isHighConfidence() { return confidence >= 0.7; }
    }

    public record CategoryStats(
            String   category,
            double   successRate,
            double   tokenCalibrationFactor,
            String   preferredMode,
            int      dataPoints,
            double   avgDurationMs
    ) {
        public String summary() {
            return String.format("[%s] success=%.0f%% cal=%.2fx mode=%s n=%d avgMs=%.0f",
                    category, successRate*100, tokenCalibrationFactor,
                    preferredMode, dataPoints, avgDurationMs);
        }
    }

    public record CalibrationReport(
            Map<String, CategoryStats> categoryStats,
            Instant                    generatedAt
    ) {
        public String summary() {
            return categoryStats.values().stream()
                    .map(CategoryStats::summary)
                    .collect(Collectors.joining("\n", "Calibration Report:\n", ""));
        }
    }
}
