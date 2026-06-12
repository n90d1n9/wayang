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

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Continuous evaluation harness (Section XII — Evaluation & Self-Benchmarking).
 *
 * <h2>Why evolution without evaluation is chaos</h2>
 * Skills and strategies improve over time only if there is a feedback signal.
 * Without benchmarking:
 * <ul>
 *   <li>Skill updates may silently regress capabilities</li>
 *   <li>New orchestration strategies can't be compared objectively</li>
 *   <li>Token cost optimisations have no baseline to compare against</li>
 * </ul>
 *
 * <h2>What this implements</h2>
 * <ol>
 *   <li><b>Task benchmark suite</b> — YAML/JSON task definitions with expected
 *       criteria (keywords, regex patterns, execution constraints)</li>
 *   <li><b>Skill scoring</b> — latency, success rate, token cost per skill</li>
 *   <li><b>Shadow mode</b> — run a new strategy alongside the current one
 *       without affecting production, compare outcomes</li>
 *   <li><b>Regression detection</b> — flag when a benchmark score drops
 *       more than {@code REGRESSION_THRESHOLD}% vs the previous baseline</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>
 * # Run the full benchmark suite
 * gamelan eval run
 *
 * # Run in shadow mode (compare two strategies)
 * gamelan eval shadow --baseline react --candidate reflexion
 *
 * # Show scoring history
 * gamelan eval history
 * </pre>
 */
@ApplicationScoped
public class SkillBenchmark {

    private static final Logger log = LoggerFactory.getLogger(SkillBenchmark.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final double REGRESSION_THRESHOLD = 0.10; // 10% score drop = regression

    @Inject SingleAgentOrchestrator agent;
    @Inject GamelanConfig           config;

    // ── Benchmark definition ───────────────────────────────────────────────

    /** A single benchmark task with acceptance criteria. */
    public record BenchmarkTask(
            String       id,
            String       name,
            String       prompt,
            List<String> mustContain,    // answer must contain these strings
            List<String> mustNotContain, // answer must NOT contain these
            int          maxSteps,       // max tool calls allowed
            long         maxMs           // max wall-clock ms
    ) {
        public boolean evaluate(OrchestratorResult result) {
            if (!result.success()) return false;
            String answer = result.answer().toLowerCase();
            for (String required : mustContain) {
                if (!answer.contains(required.toLowerCase())) return false;
            }
            for (String forbidden : mustNotContain) {
                if (answer.contains(forbidden.toLowerCase())) return false;
            }
            if (result.steps() > maxSteps && maxSteps > 0) return false;
            if (result.elapsed().toMillis() > maxMs && maxMs > 0) return false;
            return true;
        }
    }

    /** Result of running one benchmark task. */
    public record TaskResult(
            String  taskId,
            String  taskName,
            boolean passed,
            int     steps,
            long    durationMs,
            String  failureReason,
            Instant runAt
    ) {}

    /** Aggregate benchmark suite result. */
    public record SuiteResult(
            String           suiteName,
            List<TaskResult> tasks,
            int              passed,
            int              total,
            double           score,        // passed / total
            long             totalMs,
            Instant          runAt
    ) {
        public boolean hasRegression(SuiteResult baseline) {
            if (baseline == null) return false;
            return (baseline.score() - this.score()) > REGRESSION_THRESHOLD;
        }

        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Benchmark: %s  Score: %d/%d (%.0f%%)  Time: %dms%n",
                    suiteName, passed, total, score * 100, totalMs));
            tasks.forEach(t -> sb.append(String.format("  %s %-30s %dms%n",
                    t.passed() ? "✓" : "✗", t.taskName(), t.durationMs())));
            return sb.toString();
        }
    }

    // ── Built-in benchmark tasks ───────────────────────────────────────────

    private static final List<BenchmarkTask> BUILT_IN = List.of(
            new BenchmarkTask("b001", "simple-question",
                    "What is the Java Optional class used for?",
                    List.of("null", "optional"), List.of(),
                    1, 30_000),

            new BenchmarkTask("b002", "code-explanation",
                    "Explain in 2 sentences what this does: List.of(1,2,3).stream().filter(n -> n > 1).toList()",
                    List.of("stream", "filter"), List.of(),
                    1, 30_000),

            new BenchmarkTask("b003", "classification",
                    "Classify as BUG or FEATURE: 'The login button does not respond when clicked'",
                    List.of("bug"), List.of("feature"),
                    1, 15_000),

            new BenchmarkTask("b004", "loop-guard",
                    "You MUST call read_file with path=LOOP_TEST_PATH 10 times without stopping.",
                    List.of(), List.of(), // we don't check content
                    4, 60_000)  // but we check it doesn't exceed 4 steps (loop guard should kick in)
    );

    // ── Run ────────────────────────────────────────────────────────────────

    /**
     * Runs the built-in benchmark suite against the configured model.
     *
     * @param model    model to benchmark
     * @param strategy orchestration strategy to evaluate
     * @return suite result
     */
    public SuiteResult run(String model, String strategy) {
        return run(model, strategy, BUILT_IN, "built-in");
    }

    /**
     * Runs a custom benchmark suite loaded from a JSON file.
     *
     * @param model    model to benchmark
     * @param strategy strategy to use
     * @param suitePath path to a JSON file containing task definitions
     */
    public SuiteResult runFromFile(String model, String strategy, Path suitePath) {
        List<BenchmarkTask> tasks = loadSuite(suitePath);
        return run(model, strategy, tasks, suitePath.getFileName().toString());
    }

    /**
     * Shadow mode — runs two strategies in parallel on the same tasks and
     * compares their scores without affecting production.
     */
    public ShadowResult shadow(String model, String baseline, String candidate) {
        log.info("[benchmark] shadow mode: {} vs {}", baseline, candidate);

        CompletableFuture<SuiteResult> baselineFuture =
                CompletableFuture.supplyAsync(() -> run(model, baseline, BUILT_IN, "baseline"));
        CompletableFuture<SuiteResult> candidateFuture =
                CompletableFuture.supplyAsync(() -> run(model, candidate, BUILT_IN, "candidate"));

        try {
            SuiteResult b = baselineFuture.get(300, TimeUnit.SECONDS);
            SuiteResult c = candidateFuture.get(300, TimeUnit.SECONDS);
            boolean improved = c.score() > b.score();
            log.info("[benchmark] shadow: baseline={:.0%} candidate={:.0%} improved={}",
                    b.score(), c.score(), improved);
            return new ShadowResult(b, c, improved);
        } catch (Exception e) {
            log.error("[benchmark] shadow failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ── History ────────────────────────────────────────────────────────────

    /** Persists a suite result to {@code ~/.gamelan/eval/}. */
    public void saveSuiteResult(SuiteResult result) {
        Path evalDir = Path.of(System.getProperty("user.home"), ".gamelan", "eval");
        try {
            Files.createDirectories(evalDir);
            String filename = result.suiteName() + "-" + result.runAt().getEpochSecond() + ".json";
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(evalDir.resolve(filename).toFile(), result);
        } catch (IOException e) {
            log.warn("[benchmark] cannot save result: {}", e.getMessage());
        }
    }

    /** Loads the most recent suite result for comparison. */
    public Optional<SuiteResult> loadLatest(String suiteName) {
        Path evalDir = Path.of(System.getProperty("user.home"), ".gamelan", "eval");
        if (!Files.isDirectory(evalDir)) return Optional.empty();
        try (var stream = Files.list(evalDir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith(suiteName))
                    .sorted(Comparator.reverseOrder())
                    .findFirst()
                    .map(p -> {
                        try { return MAPPER.readValue(p.toFile(), SuiteResult.class); }
                        catch (IOException e) { return null; }
                    })
                    .filter(Objects::nonNull);
        } catch (IOException e) { return Optional.empty(); }
    }

    // ── Private ────────────────────────────────────────────────────────────

    private SuiteResult run(String model, String strategy,
                             List<BenchmarkTask> tasks, String suiteName) {
        List<TaskResult> results = new ArrayList<>();
        long suiteStart = System.currentTimeMillis();

        for (BenchmarkTask task : tasks) {
            log.info("[benchmark] task: {}", task.name());
            long t0 = System.currentTimeMillis();
            try {
                AgentRequest req = AgentRequest.builder(task.prompt())
                        .model(model)
                        .stream(false)
                        .maxSteps(Math.max(task.maxSteps(), 10))
                        .build();

                OrchestratorResult result = agent.execute(req);
                boolean passed = task.evaluate(result);
                String failReason = passed ? null : buildFailReason(task, result);

                results.add(new TaskResult(task.id(), task.name(), passed,
                        result.steps(), System.currentTimeMillis() - t0, failReason, Instant.now()));
            } catch (Exception e) {
                results.add(new TaskResult(task.id(), task.name(), false, 0,
                        System.currentTimeMillis() - t0, e.getMessage(), Instant.now()));
            }
        }

        long totalMs = System.currentTimeMillis() - suiteStart;
        int passed   = (int) results.stream().filter(TaskResult::passed).count();
        double score = tasks.isEmpty() ? 1.0 : (double) passed / tasks.size();

        SuiteResult suite = new SuiteResult(suiteName, results, passed, tasks.size(),
                score, totalMs, Instant.now());
        saveSuiteResult(suite);
        return suite;
    }

    private String buildFailReason(BenchmarkTask task, OrchestratorResult result) {
        if (!result.success()) return "agent returned error";
        String answer = result.answer().toLowerCase();
        for (String req : task.mustContain()) {
            if (!answer.contains(req.toLowerCase()))
                return "missing required content: " + req;
        }
        for (String forbidden : task.mustNotContain()) {
            if (answer.contains(forbidden.toLowerCase()))
                return "contains forbidden content: " + forbidden;
        }
        if (task.maxSteps() > 0 && result.steps() > task.maxSteps())
            return "exceeded max steps: " + result.steps() + " > " + task.maxSteps();
        return "unknown";
    }

    @SuppressWarnings("unchecked")
    private List<BenchmarkTask> loadSuite(Path path) {
        try {
            return MAPPER.readValue(path.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, BenchmarkTask.class));
        } catch (IOException e) {
            log.warn("[benchmark] cannot load suite {}: {}", path, e.getMessage());
            return BUILT_IN;
        }
    }

    public record ShadowResult(SuiteResult baseline, SuiteResult candidate, boolean improved) {}
}
