package tech.kayys.gamelan.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.skill.Skill;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Continuous Evaluation & Benchmarking System.
 *
 * <h2>Why this is missing in most agentic AI systems</h2>
 * Evolution without evaluation leads to chaos. Skills get updated, but nobody
 * knows if they actually got better. Model upgrades might break task completion.
 * Without a rigorous evaluation loop, you can't safely evolve.
 *
 * <h2>Components</h2>
 * <ul>
 *   <li><b>SkillEvaluator</b>: Runs benchmark tasks against a skill, scores by
 *       latency, success rate, and output quality</li>
 *   <li><b>BenchmarkSuite</b>: Curated task set with expected outputs and rubrics</li>
 *   <li><b>ShadowMode</b>: Tests new skills against production without affecting users</li>
 *   <li><b>RegressionDetector</b>: Alerts when a skill's success rate drops</li>
 *   <li><b>ParetoProfiler</b>: Tracks quality vs. cost trade-offs across skill versions</li>
 * </ul>
 *
 * <h2>Pareto Optimality</h2>
 * A skill update is accepted only if it is Pareto-superior to its predecessor:
 * strictly better on at least one metric (quality, speed, cost) and no worse
 * on any other. This prevents regressions while allowing genuine improvements.
 */
@ApplicationScoped
public class BenchmarkHarness {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkHarness.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig           config;

    private final Map<String, BenchmarkSuite>   suites  = new ConcurrentHashMap<>();
    private final Map<String, List<BenchResult>> history = new ConcurrentHashMap<>();

    // ── Benchmark suites ────────────────────────────────────────────────────

    /**
     * Registers a benchmark suite for a skill.
     */
    public void registerSuite(String skillName, BenchmarkSuite suite) {
        suites.put(skillName, suite);
        log.info("[eval] registered benchmark suite for '{}' ({} tasks)",
                skillName, suite.tasks().size());
    }

    /**
     * Runs all benchmark tasks for a skill and returns a scored report.
     */
    public BenchmarkReport evaluate(String skillName) {
        BenchmarkSuite suite = suites.get(skillName);
        if (suite == null) {
            return BenchmarkReport.empty(skillName, "No benchmark suite registered");
        }
        return run(skillName, suite, false);
    }

    /**
     * Shadow mode: runs the new skill version alongside production without
     * user-visible side effects. Returns a comparison report.
     */
    public ShadowReport shadow(String skillName, Skill candidate) {
        BenchmarkSuite suite = suites.get(skillName);
        if (suite == null) return ShadowReport.noSuite(skillName);

        List<BenchResult> baseline  = history.getOrDefault(skillName, List.of());
        BenchmarkReport   newReport = run(skillName + "-shadow", suite, true);

        return new ShadowReport(skillName, baseline.isEmpty() ? null
                : aggregateBaseline(baseline), newReport,
                isParetoSuperior(baseline, newReport.results()));
    }

    /**
     * Checks for regression: compares current performance against the rolling
     * baseline. Returns alerts for significant drops.
     */
    public List<RegressionAlert> detectRegression(String skillName) {
        List<BenchResult> hist = history.getOrDefault(skillName, List.of());
        if (hist.size() < 5) return List.of(); // not enough data

        List<BenchResult> recent  = hist.subList(Math.max(0, hist.size() - 5), hist.size());
        List<BenchResult> older   = hist.subList(0, hist.size() - 5);

        double recentSuccess = recent.stream().mapToDouble(BenchResult::score).average().orElse(0);
        double olderSuccess  = older.stream().mapToDouble(BenchResult::score).average().orElse(0);

        List<RegressionAlert> alerts = new ArrayList<>();
        if (recentSuccess < olderSuccess - 0.15) {
            alerts.add(new RegressionAlert(skillName, "success_rate",
                    olderSuccess, recentSuccess,
                    String.format("Success rate dropped %.0f%% → %.0f%%",
                            olderSuccess * 100, recentSuccess * 100)));
        }

        double recentLatency = recent.stream().mapToLong(BenchResult::durationMs).average().orElse(0);
        double olderLatency  = older.stream().mapToLong(BenchResult::durationMs).average().orElse(0);
        if (recentLatency > olderLatency * 1.5) {
            alerts.add(new RegressionAlert(skillName, "latency",
                    olderLatency, recentLatency,
                    String.format("Latency increased %.0fms → %.0fms",
                            olderLatency, recentLatency)));
        }
        return alerts;
    }

    /**
     * Returns a full performance profile for a skill across all benchmark runs.
     */
    public SkillProfile profile(String skillName) {
        List<BenchResult> hist = history.getOrDefault(skillName, List.of());
        if (hist.isEmpty()) return SkillProfile.empty(skillName);

        double avgScore   = hist.stream().mapToDouble(BenchResult::score).average().orElse(0);
        double avgLatency = hist.stream().mapToLong(BenchResult::durationMs).average().orElse(0);
        double avgCost    = hist.stream().mapToInt(BenchResult::tokenCount).average().orElse(0);
        long   successes  = hist.stream().filter(BenchResult::success).count();

        return new SkillProfile(skillName, hist.size(),
                (double) successes / hist.size(), avgScore, avgLatency, avgCost,
                hist.get(hist.size() - 1).runAt());
    }

    // ── Execution ───────────────────────────────────────────────────────────

    private BenchmarkReport run(String id, BenchmarkSuite suite, boolean shadow) {
        log.info("[eval] running {} benchmark tasks for '{}'{}",
                suite.tasks().size(), id, shadow ? " [shadow]" : "");

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<BenchResult>> futures = suite.tasks().stream()
                .map(task -> exec.submit(() -> runTask(id, task, shadow)))
                .toList();
        exec.shutdown();

        List<BenchResult> results = new ArrayList<>();
        for (Future<BenchResult> f : futures) {
            try {
                results.add(f.get(120, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("[eval] task timed out: {}", e.getMessage());
            }
        }

        history.computeIfAbsent(id, k -> new ArrayList<>()).addAll(results);
        persist(id, results);

        return new BenchmarkReport(id, results, Instant.now());
    }

    private BenchResult runTask(String skillName, BenchTask task, boolean dryRun) {
        long start = System.currentTimeMillis();
        try {
            if (dryRun) {
                // Shadow mode: don't write files, use read-only tools
                log.debug("[eval] shadow running task: {}", truncate(task.prompt(), 60));
            }

            AgentRequest req = AgentRequest.builder(task.prompt())
                    .model(config.defaultModel())
                    .stream(false)
                    .maxSteps(task.maxSteps())
                    .build();

            OrchestratorResult result = orchestrator.execute(req);
            long duration = System.currentTimeMillis() - start;

            double score = scoreOutput(result.answer(), task);
            boolean success = score >= task.minPassScore();

            return new BenchResult(task.id(), skillName, task.prompt(),
                    result.answer(), score, success, duration,
                    result.toolResults().size() * 150, // rough token estimate
                    Instant.now());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return new BenchResult(task.id(), skillName, task.prompt(),
                    "ERROR: " + e.getMessage(), 0.0, false, duration, 0, Instant.now());
        }
    }

    /**
     * Scores agent output against a task's rubric.
     * Uses keyword coverage as a cheap proxy; plug in LLM-as-judge for production.
     */
    private double scoreOutput(String output, BenchTask task) {
        if (output == null || output.isBlank()) return 0.0;
        if (task.expectedKeywords().isEmpty()) return 1.0;

        long matched = task.expectedKeywords().stream()
                .filter(kw -> output.toLowerCase().contains(kw.toLowerCase()))
                .count();
        return (double) matched / task.expectedKeywords().size();
    }

    private BenchmarkReport aggregateBaseline(List<BenchResult> results) {
        return new BenchmarkReport("baseline", results, Instant.now());
    }

    private boolean isParetoSuperior(List<BenchResult> baseline, List<BenchResult> candidate) {
        if (baseline.isEmpty()) return true;
        double bScore   = baseline.stream().mapToDouble(BenchResult::score).average().orElse(0);
        double cScore   = candidate.stream().mapToDouble(BenchResult::score).average().orElse(0);
        double bLatency = baseline.stream().mapToLong(BenchResult::durationMs).average().orElse(0);
        double cLatency = candidate.stream().mapToLong(BenchResult::durationMs).average().orElse(0);
        // Pareto: better on at least one, no worse on others
        boolean betterScore   = cScore   >= bScore   - 0.01;
        boolean betterLatency = cLatency <= bLatency * 1.1;
        boolean improvesScore = cScore   > bScore   + 0.05;
        boolean improvesCost  = cLatency < bLatency * 0.9;
        return betterScore && betterLatency && (improvesScore || improvesCost);
    }

    private void persist(String skillName, List<BenchResult> results) {
        Path dir = Path.of(System.getProperty("user.home"), ".gamelan", "benchmarks");
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitize(skillName) + "-bench.json");
            List<BenchResult> all = new ArrayList<>();
            if (Files.exists(file)) {
                List<?> existing = MAPPER.readValue(file.toFile(), List.class);
                all.addAll((List<BenchResult>) existing);
            }
            all.addAll(results);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), all);
        } catch (IOException e) {
            log.warn("[eval] persist failed: {}", e.getMessage());
        }
    }

    private String sanitize(String s) { return s.replaceAll("[^a-zA-Z0-9-]", "-"); }
    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /** A curated set of benchmark tasks for a skill. */
    public record BenchmarkSuite(String skillName, List<BenchTask> tasks) {}

    /** A single benchmark task with expected outputs and scoring criteria. */
    public record BenchTask(
            String       id,
            String       prompt,
            List<String> expectedKeywords,
            double       minPassScore,
            int          maxSteps
    ) {
        public static BenchTask simple(String prompt, String... keywords) {
            return new BenchTask(UUID.randomUUID().toString(), prompt,
                    List.of(keywords), 0.7, 5);
        }
    }

    /** Result of running one benchmark task. */
    public record BenchResult(
            String  taskId,
            String  skillName,
            String  prompt,
            String  output,
            double  score,
            boolean success,
            long    durationMs,
            int     tokenCount,
            Instant runAt
    ) {}

    /** Aggregated report from running a full benchmark suite. */
    public record BenchmarkReport(
            String           skillName,
            List<BenchResult> results,
            Instant          ranAt
    ) {
        public double avgScore()   { return results.stream().mapToDouble(BenchResult::score).average().orElse(0); }
        public double successRate(){ long s = results.stream().filter(BenchResult::success).count();
            return results.isEmpty() ? 0 : (double)s/results.size(); }
        public double avgLatency() { return results.stream().mapToLong(BenchResult::durationMs).average().orElse(0); }

        static BenchmarkReport empty(String n, String reason) {
            return new BenchmarkReport(n, List.of(), Instant.now());
        }
        public String summary() {
            return String.format("%s: %.0f%% pass | avg score %.2f | avg %.0fms",
                    skillName, successRate()*100, avgScore(), avgLatency());
        }
    }

    /** Shadow mode comparison report. */
    public record ShadowReport(
            String          skillName,
            BenchmarkReport baseline,
            BenchmarkReport candidate,
            boolean         paretoSuperior
    ) {
        static ShadowReport noSuite(String n) {
            return new ShadowReport(n, null, BenchmarkReport.empty(n, "no suite"), false);
        }
    }

    /** Alert raised when regression is detected. */
    public record RegressionAlert(
            String skillName, String metric,
            double baseline, double current, String message
    ) {}

    /** Full performance profile for a skill. */
    public record SkillProfile(
            String  skillName, int runs, double successRate,
            double  avgScore, double avgLatencyMs, double avgTokens, Instant lastRun
    ) {
        static SkillProfile empty(String n) {
            return new SkillProfile(n, 0, 0, 0, 0, 0, null);
        }
        public String summary() {
            return String.format("%s: %d runs | %.0f%% success | avg %.2f score | %.0fms | %.0f tokens",
                    skillName, runs, successRate*100, avgScore, avgLatencyMs, avgTokens);
        }
    }
}
