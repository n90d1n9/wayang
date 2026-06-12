package tech.kayys.gamelan.evolution.avo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.evaluation.BenchmarkHarness;
import tech.kayys.gamelan.evolution.pareto.ParetoFrontier;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Agentic Variation Operator (AVO) — the agent as the "mutation operator" in evolutionary search.
 *
 * <h2>What is AVO</h2>
 * Classical evolutionary algorithms use random mutation operators (bit-flip, crossover).
 * AVO replaces random mutation with targeted LLM-guided variation:
 * <ul>
 *   <li>The agent analyzes which aspects of the current solution are weakest</li>
 *   <li>It proposes specific, targeted improvements — not random changes</li>
 *   <li>Each variation is grounded in evidence from the execution trace</li>
 *   <li>Multiple variation strategies are explored in parallel (population = N variants)</li>
 * </ul>
 *
 * <h2>AVO vs Standard EvoSkill</h2>
 * EvoSkill: Analyze failures → Propose ONE fix → Benchmark → Accept/Reject<br>
 * AVO: Analyze failures → Generate N variations with DIFFERENT strategies →
 *      Benchmark ALL → Build Pareto frontier → Select non-dominated solutions →
 *      Crossover best aspects of top solutions → Next generation
 *
 * <h2>Variation Strategies</h2>
 * The AVO generates variations along different axes:
 * <ol>
 *   <li><b>Instruction precision</b>: make vague instructions concrete with examples</li>
 *   <li><b>Tool sequence</b>: reorder or add/remove tool steps</li>
 *   <li><b>Constraint tightening</b>: add explicit constraints for common failure modes</li>
 *   <li><b>Example injection</b>: add worked examples of correct behavior</li>
 *   <li><b>Decomposition</b>: split complex instructions into simpler sub-steps</li>
 *   <li><b>Crossover</b>: combine the best elements of two high-performing variants</li>
 * </ol>
 *
 * <h2>Real-world analogy</h2>
 * This is inspired by the 2024 research showing that an LLM-powered AVO
 * running for 7 days autonomously found GPU kernel optimizations that beat
 * hand-tuned code — by treating the LLM not as the evaluator but as the
 * mutation generator, with real execution as the fitness function.
 */
@ApplicationScoped
public class AgenticVariationOperator {

    private static final Logger log = LoggerFactory.getLogger(AgenticVariationOperator.class);

    private static final int POPULATION_SIZE       = 5;  // variants per generation
    private static final int MAX_GENERATIONS       = 10;
    private static final int MAX_CONCURRENT_EVALS  = 3;
    private static final double CONVERGENCE_DELTA  = 0.02; // stop if improvement < 2%

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Inject GollekSdk        sdk;
    @Inject GamelanConfig    config;
    @Inject EpisodicMemory   episodic;
    @Inject BenchmarkHarness benchmark;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Runs the full AVO evolutionary loop for a skill.
     *
     * @param skill         the skill to evolve
     * @param suite         the benchmark suite to evaluate variants
     * @param dryRun        if true, generates variants but does not apply
     * @param progressListener called after each generation with the current frontier
     * @return the evolution result including the best variant found
     */
    public AVOResult evolve(Skill skill, BenchmarkHarness.BenchmarkSuite suite,
                             boolean dryRun,
                             java.util.function.Consumer<GenerationResult> progressListener) {
        log.info("[AVO] starting evolution for skill '{}': {} generations × {} variants",
                skill.name(), MAX_GENERATIONS, POPULATION_SIZE);
        Instant start = Instant.now();

        ParetoFrontier frontier = new ParetoFrontier(skill.name());
        List<GenerationResult> generations = new ArrayList<>();

        // Evaluate the baseline
        ParetoFrontier.ParetoPoint baseline = evaluateVariant(
                skill.name() + "-v0", skill.instructions(), suite);
        frontier.add(baseline);
        log.info("[AVO] baseline: quality={:.3f} latency={}ms tokens={}",
                baseline.qualityScore(), baseline.latencyMs(), baseline.tokenCost());

        String currentBestInstructions = skill.instructions();
        double lastBestQuality = baseline.qualityScore();

        for (int gen = 1; gen <= MAX_GENERATIONS; gen++) {
            log.info("[AVO] generation {}/{}", gen, MAX_GENERATIONS);

            // Generate population of variants
            List<Variant> variants = generatePopulation(
                    skill, currentBestInstructions, frontier, gen);

            // Evaluate all variants (with concurrency limit)
            List<EvaluatedVariant> evaluated = evaluatePopulation(variants, suite, gen);

            // Update Pareto frontier
            List<ParetoFrontier.ParetoPoint> newFrontierPoints = new ArrayList<>();
            for (EvaluatedVariant ev : evaluated) {
                ParetoFrontier.AddResult result = frontier.add(ev.point());
                if (result.accepted()) {
                    newFrontierPoints.add(ev.point());
                    if (ev.point().qualityScore() > lastBestQuality) {
                        currentBestInstructions = ev.variant().instructions();
                        lastBestQuality = ev.point().qualityScore();
                    }
                }
            }

            GenerationResult genResult = new GenerationResult(
                    gen, variants, evaluated, newFrontierPoints, frontier.frontier(),
                    Duration.between(start, Instant.now()));

            generations.add(genResult);
            if (progressListener != null) progressListener.accept(genResult);

            log.info("[AVO] gen {} complete: frontier={} points, best_quality={:.3f}",
                    gen, frontier.frontierSize(), lastBestQuality);

            // Convergence check: stop if improvement is negligible
            if (gen > 2 && (lastBestQuality - baseline.qualityScore()) < CONVERGENCE_DELTA) {
                log.info("[AVO] converged at generation {} (Δ < {})", gen, CONVERGENCE_DELTA);
                break;
            }
        }

        // Select the best variant from the Pareto frontier
        Optional<ParetoFrontier.ParetoPoint> best = frontier.bestWeighted(
                ParetoFrontier.ObjectiveWeights.qualityFirst());

        boolean improved = best.isPresent() &&
                best.get().qualityScore() > baseline.qualityScore() + CONVERGENCE_DELTA;

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[AVO] complete: improved={} baseline={:.3f} best={:.3f} elapsed={}",
                improved, baseline.qualityScore(),
                best.map(ParetoFrontier.ParetoPoint::qualityScore).orElse(0.0),
                elapsed);

        return new AVOResult(skill.name(), baseline, frontier, generations,
                best.orElse(baseline), improved, dryRun, elapsed);
    }

    // ── Variant generation ─────────────────────────────────────────────────

    /**
     * Generates a population of diverse variants using different variation strategies.
     * Each variant targets a different weakness in the current best solution.
     */
    private List<Variant> generatePopulation(Skill skill, String currentInstructions,
                                              ParetoFrontier frontier, int generation) {
        List<VariationStrategy> strategies = selectStrategies(generation, frontier);
        List<Variant> variants = new ArrayList<>();

        for (int i = 0; i < Math.min(POPULATION_SIZE, strategies.size()); i++) {
            VariationStrategy strategy = strategies.get(i);
            String variantId = skill.name() + "-v" + generation + "." + (i+1) + "-" + strategy.code();
            try {
                String newInstructions = applyVariation(strategy, skill, currentInstructions, frontier);
                if (!newInstructions.isBlank() && !newInstructions.equals(currentInstructions)) {
                    variants.add(new Variant(variantId, newInstructions, strategy,
                            generation, Instant.now()));
                }
            } catch (Exception e) {
                log.debug("[AVO] variant generation failed for {}: {}", strategy, e.getMessage());
            }
        }

        // Always include a crossover variant in later generations
        if (generation > 2 && frontier.frontierSize() >= 2) {
            List<ParetoFrontier.ParetoPoint> frontierPoints = frontier.frontier();
            try {
                String crossover = applyCrossover(skill,
                        frontierPoints.get(0), frontierPoints.get(1), currentInstructions);
                if (!crossover.isBlank()) {
                    variants.add(new Variant(
                            skill.name() + "-v" + generation + ".x-crossover",
                            crossover, VariationStrategy.CROSSOVER, generation, Instant.now()));
                }
            } catch (Exception e) {
                log.debug("[AVO] crossover failed: {}", e.getMessage());
            }
        }

        return variants;
    }

    private List<VariationStrategy> selectStrategies(int generation, ParetoFrontier frontier) {
        // Early generations: explore broadly
        // Later generations: focus on weaknesses identified by frontier analysis
        if (generation <= 3) {
            return List.of(
                    VariationStrategy.PRECISION,
                    VariationStrategy.TOOL_SEQUENCE,
                    VariationStrategy.CONSTRAINT_TIGHTENING,
                    VariationStrategy.EXAMPLE_INJECTION,
                    VariationStrategy.DECOMPOSITION);
        }

        // Find the primary bottleneck from the frontier
        List<VariationStrategy> focused = new ArrayList<>();
        frontier.bestOn(ParetoFrontier.Objective.LATENCY_MS).ifPresent(p -> {
            if (p.latencyMs() > 5000) focused.add(VariationStrategy.TOOL_SEQUENCE);
        });
        frontier.bestOn(ParetoFrontier.Objective.QUALITY_SCORE).ifPresent(p -> {
            if (p.qualityScore() < 0.7) {
                focused.add(VariationStrategy.EXAMPLE_INJECTION);
                focused.add(VariationStrategy.CONSTRAINT_TIGHTENING);
            }
        });
        focused.add(VariationStrategy.PRECISION);
        focused.add(VariationStrategy.CROSSOVER);
        return focused;
    }

    private String applyVariation(VariationStrategy strategy, Skill skill,
                                   String currentInstructions, ParetoFrontier frontier)
            throws Exception {
        String strategyPrompt = buildVariationPrompt(strategy, skill, currentInstructions);

        InferenceResponse resp = sdk.createCompletion(
                InferenceRequest.builder()
                        .model(config.defaultModel())
                        .systemPrompt("You are a skill instruction optimizer. Output ONLY the new instruction text.")
                        .messages(List.of(Message.user(strategyPrompt)))
                        .temperature(0.4 + (0.1 * strategy.ordinal())) // vary temperature by strategy
                        .maxTokens(1500)
                        .streaming(false)
                        .build());
        return resp.getContent() != null ? resp.getContent().strip() : "";
    }

    private String buildVariationPrompt(VariationStrategy strategy, Skill skill,
                                         String currentInstructions) {
        String failureContext = getFailureContext(skill.name());
        return switch (strategy) {
            case PRECISION -> """
                    Rewrite these skill instructions to be MORE PRECISE.
                    Replace vague phrases ('check the code', 'analyze carefully') with
                    exact actions ('read file X, look for pattern Y, apply fix Z').
                    Add concrete examples for the 2 most common use cases.
                    
                    Skill: %s
                    Current: %s
                    Failures: %s
                    """.formatted(skill.name(), currentInstructions, failureContext);

            case TOOL_SEQUENCE -> """
                    Rewrite these instructions to optimize the tool call sequence.
                    Reduce redundant reads. Batch related operations.
                    Add explicit "read before write" and "verify after apply" steps.
                    
                    Skill: %s
                    Current: %s
                    Failures: %s
                    """.formatted(skill.name(), currentInstructions, failureContext);

            case CONSTRAINT_TIGHTENING -> """
                    Rewrite these instructions adding explicit CONSTRAINTS for failure cases.
                    For each identified failure pattern, add: "If X, then Y. Never do Z."
                    Add error handling steps for the top-3 failure modes.
                    
                    Skill: %s
                    Current: %s
                    Failure patterns: %s
                    """.formatted(skill.name(), currentInstructions, failureContext);

            case EXAMPLE_INJECTION -> """
                    Rewrite these instructions adding 2-3 CONCRETE EXAMPLES.
                    Format: "For example: input=X, correct output=Y, avoid=Z"
                    Examples should cover: happy path, edge case, common error recovery.
                    
                    Skill: %s
                    Current: %s
                    """.formatted(skill.name(), currentInstructions);

            case DECOMPOSITION -> """
                    Rewrite these instructions by DECOMPOSING complex steps into simpler sub-steps.
                    Each step should be: one action, one tool, one clear success criterion.
                    Add numbered steps with explicit "IF condition THEN step, ELSE fallback".
                    
                    Skill: %s
                    Current: %s
                    """.formatted(skill.name(), currentInstructions);

            default -> "Improve these instructions: " + currentInstructions;
        };
    }

    private String applyCrossover(Skill skill, ParetoFrontier.ParetoPoint pointA,
                                   ParetoFrontier.ParetoPoint pointB, String currentInstructions)
            throws Exception {
        String prompt = """
                Combine the best aspects of these two skill instruction versions.
                Version A is better on quality (%s). Version B is faster (%s).
                Create a version that captures the precision of A with the efficiency of B.
                
                Current best instructions (use as base):
                %s
                
                Produce combined instructions that are both precise AND efficient.
                """.formatted(
                        String.format("%.2f", pointA.qualityScore()),
                        pointB.latencyMs() + "ms",
                        currentInstructions);

        InferenceResponse resp = sdk.createCompletion(
                InferenceRequest.builder()
                        .model(config.defaultModel())
                        .systemPrompt("Combine instruction variants. Output ONLY the new instructions.")
                        .messages(List.of(Message.user(prompt)))
                        .temperature(0.3).maxTokens(1500).streaming(false).build());
        return resp.getContent() != null ? resp.getContent().strip() : "";
    }

    // ── Evaluation ─────────────────────────────────────────────────────────

    private List<EvaluatedVariant> evaluatePopulation(List<Variant> variants,
                                                        BenchmarkHarness.BenchmarkSuite suite,
                                                        int generation) {
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore semaphore  = new Semaphore(MAX_CONCURRENT_EVALS);
        List<Future<EvaluatedVariant>> futures = new ArrayList<>();

        for (Variant variant : variants) {
            futures.add(exec.submit(() -> {
                semaphore.acquire();
                try {
                    ParetoFrontier.ParetoPoint point = evaluateVariant(
                            variant.id(), variant.instructions(), suite);
                    return new EvaluatedVariant(variant, point);
                } finally {
                    semaphore.release();
                }
            }));
        }
        exec.shutdown();

        List<EvaluatedVariant> results = new ArrayList<>();
        for (Future<EvaluatedVariant> f : futures) {
            try {
                results.add(f.get(120, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("[AVO] evaluation failed: {}", e.getMessage());
            }
        }
        return results;
    }

    /**
     * Evaluates a variant by running the benchmark suite and measuring metrics.
     * In production, this runs real tasks. Here we use the BenchmarkHarness.
     */
    private ParetoFrontier.ParetoPoint evaluateVariant(String version, String instructions,
                                                        BenchmarkHarness.BenchmarkSuite suite) {
        Instant evalStart = Instant.now();
        BenchmarkHarness.BenchmarkReport report = benchmark.evaluate(
                suite.skillName() + "-" + version.replace("/","-"));

        long latency    = Duration.between(evalStart, Instant.now()).toMillis();
        int  tokenEst   = instructions.length() / 4 * suite.tasks().size(); // rough estimate
        int  toolCalls  = report.results().stream()
                .mapToInt(r -> (int)(r.output().split("<tool_call").length - 1))
                .sum();

        return new ParetoFrontier.ParetoPoint(
                version, report.avgScore(), report.successRate(),
                latency, tokenEst, Math.max(1, toolCalls),
                "AVO-" + Instant.now().getEpochSecond(), Instant.now());
    }

    private String getFailureContext(String skillName) {
        List<EpisodicMemory.Episode> failures = episodic.recentFailures(5).stream()
                .filter(e -> e.toolsUsed().stream()
                        .anyMatch(t -> t.contains(skillName)))
                .toList();
        if (failures.isEmpty()) return "(no recent failures)";
        return failures.stream()
                .map(e -> "- " + truncate(e.result(), 100))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum VariationStrategy {
        PRECISION              ("prec", "Make instructions more precise and concrete"),
        TOOL_SEQUENCE          ("tools","Optimize tool call sequence and flow"),
        CONSTRAINT_TIGHTENING  ("cstr", "Add explicit constraints for failure modes"),
        EXAMPLE_INJECTION      ("exmp", "Inject concrete worked examples"),
        DECOMPOSITION          ("deco", "Decompose complex steps into atomic sub-steps"),
        CROSSOVER              ("xovr", "Combine best aspects of top frontier solutions");

        private final String code;
        private final String description;
        VariationStrategy(String code, String desc) { this.code = code; this.description = desc; }
        public String code()        { return code; }
        public String description() { return description; }
    }

    public record Variant(
            String            id,
            String            instructions,
            VariationStrategy strategy,
            int               generation,
            Instant           createdAt
    ) {}

    public record EvaluatedVariant(
            Variant                  variant,
            ParetoFrontier.ParetoPoint point
    ) {}

    public record GenerationResult(
            int                            generationNumber,
            List<Variant>                  variants,
            List<EvaluatedVariant>         evaluated,
            List<ParetoFrontier.ParetoPoint> newFrontierPoints,
            List<ParetoFrontier.ParetoPoint> frontier,
            Duration                        elapsed
    ) {
        public int improvedCount() { return newFrontierPoints.size(); }
        public boolean hasImprovement() { return !newFrontierPoints.isEmpty(); }
        public String summary() {
            return String.format("Gen %d: %d variants → %d improved frontier | %dms",
                    generationNumber, variants.size(), improvedCount(), elapsed.toMillis());
        }
    }

    public record AVOResult(
            String                         skillName,
            ParetoFrontier.ParetoPoint     baseline,
            ParetoFrontier                 frontier,
            List<GenerationResult>         generations,
            ParetoFrontier.ParetoPoint     bestFound,
            boolean                        improved,
            boolean                        dryRun,
            Duration                       elapsed
    ) {
        public double qualityImprovement() {
            return bestFound.qualityScore() - baseline.qualityScore();
        }
        public double latencyImprovement() {
            return 1.0 - (double) bestFound.latencyMs() / Math.max(1, baseline.latencyMs());
        }
        public int generationsRun() { return generations.size(); }

        public String summary() {
            return String.format(
                    "AVO[%s]: %d gens | improved=%b | Δquality=+%.3f | Δlatency=%+.0f%% | %dms%s",
                    skillName, generationsRun(), improved,
                    qualityImprovement(), latencyImprovement() * 100,
                    elapsed.toMillis(), dryRun ? " [DRY RUN]" : "");
        }
    }
}
