package tech.kayys.gamelan.agent.multimodel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * MultiModelEnsemble — parallel execution across multiple models with result synthesis.
 *
 * <h2>From the OPENDEV paper (§2.2.5, §3.1)</h2>
 * The paper considers but rejects ensemble execution as the primary routing strategy:
 * "ensemble execution (maximum quality but prohibitive latency and cost)." However, the paper's
 * compound AI systems principle (Zaharia et al.) argues that state-of-the-art results are
 * increasingly achieved by systems that compose multiple models.
 *
 * <h2>When ensemble adds value</h2>
 * Ensemble is selectively useful for:
 * <ul>
 *   <li>Security review — cross-validate vulnerability findings across models</li>
 *   <li>Critical edits — at least 2/N models must agree before applying</li>
 *   <li>Code review — majority-vote on quality/correctness</li>
 *   <li>Ambiguous requirements — gather diverse interpretations</li>
 * </ul>
 *
 * <h2>Synthesis strategies</h2>
 * <pre>
 * MAJORITY_VOTE  — use the answer that appears most frequently (for classification)
 * BEST_SCORE     — use the answer with the highest confidence score
 * LLM_SYNTHESIS  — ask a judge model to synthesize all answers into one coherent response
 * FIRST_SUCCESS  — return the first model that completes within timeout (fastest)
 * ALL_AGREE      — only proceed if all models return equivalent answers
 * </pre>
 */
@ApplicationScoped
public class MultiModelEnsemble {

    private static final Logger log = LoggerFactory.getLogger(MultiModelEnsemble.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig           config;
    @Inject AgentTelemetry          telemetry;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Runs the same prompt against all specified models in parallel and synthesizes results.
     *
     * @param prompt   the prompt to send to all models
     * @param models   list of model identifiers to query
     * @param strategy how to synthesize the results
     * @return an EnsembleResult containing individual answers + the synthesized result
     */
    public EnsembleResult run(String prompt, List<String> models, SynthesisStrategy strategy) {
        if (models == null || models.isEmpty()) {
            return EnsembleResult.empty("no models configured");
        }

        Instant start = Instant.now();
        log.info("[ensemble] running {} models, strategy={}", models.size(), strategy);
        telemetry.count("ensemble.run." + strategy.name().toLowerCase());

        // FIRST_SUCCESS — race all models, return first to complete
        if (strategy == SynthesisStrategy.FIRST_SUCCESS) {
            return raceModels(prompt, models, start);
        }

        // All other strategies: run all in parallel, collect results
        List<ModelAnswer> answers = runParallel(prompt, models);
        String synthesized = synthesize(answers, strategy, prompt);
        Duration elapsed = Duration.between(start, Instant.now());

        log.info("[ensemble] {} answers, synthesized in {}ms", answers.size(), elapsed.toMillis());
        telemetry.recordLatency("ensemble.latency", elapsed.toMillis());

        return new EnsembleResult(answers, synthesized, strategy, elapsed,
                answers.stream().allMatch(ModelAnswer::success));
    }

    /**
     * Majority-vote classification: each model returns one of the provided labels.
     *
     * @param prompt  the classification prompt
     * @param models  models to query
     * @param labels  valid output labels (e.g., ["safe", "unsafe"])
     * @return the label with the most votes, or empty if no majority
     */
    public Optional<String> classify(String prompt, List<String> models, List<String> labels) {
        String classifyPrompt = prompt + "\n\nReply with EXACTLY one of: " +
                String.join(", ", labels) + ". No other output.";
        EnsembleResult result = run(classifyPrompt, models, SynthesisStrategy.MAJORITY_VOTE);
        return Optional.ofNullable(result.synthesized()).filter(s -> !s.isBlank());
    }

    /**
     * Consensus check: returns true only if all models agree on the answer.
     * Useful for critical safety decisions.
     */
    public boolean allAgree(String prompt, List<String> models, double similarityThreshold) {
        List<ModelAnswer> answers = runParallel(prompt, models);
        if (answers.stream().anyMatch(a -> !a.success())) return false;
        List<String> texts = answers.stream().map(ModelAnswer::answer).toList();
        // Simple agreement: check that all answers contain the same key token
        return texts.stream().allMatch(t -> textsAreEquivalent(t, texts.get(0), similarityThreshold));
    }

    // ── Private ────────────────────────────────────────────────────────────

    private List<ModelAnswer> runParallel(String prompt, List<String> models) {
        List<CompletableFuture<ModelAnswer>> futures = models.stream()
                .map(model -> CompletableFuture.supplyAsync(() -> callModel(model, prompt)))
                .toList();
        return futures.stream().map(f -> {
            try { return f.get(60, TimeUnit.SECONDS); }
            catch (Exception e) { return ModelAnswer.failed(models.get(0), e.getMessage()); }
        }).toList();
    }

    private EnsembleResult raceModels(String prompt, List<String> models, Instant start) {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        try {
            CompletableFuture<ModelAnswer>[] futures = models.stream()
                    .map(model -> CompletableFuture.supplyAsync(() -> callModel(model, prompt), pool))
                    .toArray(CompletableFuture[]::new);

            ModelAnswer first = CompletableFuture.anyOf(futures)
                    .thenApply(o -> (ModelAnswer) o)
                    .get(30, TimeUnit.SECONDS);

            Duration elapsed = Duration.between(start, Instant.now());
            return new EnsembleResult(List.of(first), first.answer(),
                    SynthesisStrategy.FIRST_SUCCESS, elapsed, first.success());
        } catch (Exception e) {
            return EnsembleResult.empty("race failed: " + e.getMessage());
        } finally {
            pool.shutdownNow();
        }
    }

    private ModelAnswer callModel(String model, String prompt) {
        Instant s = Instant.now();
        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(model)
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            Duration d = Duration.between(s, Instant.now());
            return new ModelAnswer(model, result.success() ? result.answer() : "",
                    result.success(), d, null);
        } catch (Exception e) {
            return ModelAnswer.failed(model, e.getMessage());
        }
    }

    private String synthesize(List<ModelAnswer> answers, SynthesisStrategy strategy, String prompt) {
        List<ModelAnswer> successful = answers.stream().filter(ModelAnswer::success).toList();
        if (successful.isEmpty()) return "";

        return switch (strategy) {
            case MAJORITY_VOTE -> majorityVote(successful);
            case BEST_SCORE    -> bestScore(successful);
            case LLM_SYNTHESIS -> llmSynthesize(successful, prompt);
            case ALL_AGREE     -> allAgreeResult(successful);
            default            -> successful.get(0).answer();
        };
    }

    private String majorityVote(List<ModelAnswer> answers) {
        Map<String, Long> counts = answers.stream()
                .collect(Collectors.groupingBy(
                        a -> a.answer().strip().toLowerCase().split("\\s+")[0],
                        Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(answers.get(0).answer());
    }

    private String bestScore(List<ModelAnswer> answers) {
        // Heuristic: prefer longer, more detailed answers (proxy for confidence)
        return answers.stream()
                .max(Comparator.comparingInt(a -> a.answer().length()))
                .map(ModelAnswer::answer).orElse("");
    }

    private String llmSynthesize(List<ModelAnswer> answers, String originalPrompt) {
        String answersText = IntStream.range(0, answers.size())
                .mapToObj(i -> "Model " + (i + 1) + " (" + answers.get(i).model() + "):\n" +
                               answers.get(i).answer())
                .collect(Collectors.joining("\n\n---\n\n"));
        String synthPrompt = "Original question: " + originalPrompt +
                "\n\nMultiple model answers:\n" + answersText +
                "\n\nSynthesize these answers into one accurate, comprehensive response. " +
                "Highlight any disagreements. Be concise.";
        try {
            OrchestratorResult r = orchestrator.execute(
                    AgentRequest.builder(synthPrompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, 2000))
                            .stream(false).maxSteps(1).build());
            return r.success() ? r.answer() : answers.get(0).answer();
        } catch (Exception e) { return answers.get(0).answer(); }
    }

    private String allAgreeResult(List<ModelAnswer> answers) {
        boolean agree = answers.stream()
                .allMatch(a -> textsAreEquivalent(a.answer(), answers.get(0).answer(), 0.8));
        if (agree) return answers.get(0).answer();
        return "[MODELS DISAGREE] " +
                answers.stream().map(a -> a.model() + ": " + a.answer().substring(0, Math.min(50, a.answer().length())))
                        .collect(Collectors.joining(" | "));
    }

    private boolean textsAreEquivalent(String a, String b, double threshold) {
        if (a == null || b == null) return false;
        String aLower = a.strip().toLowerCase();
        String bLower = b.strip().toLowerCase();
        if (aLower.equals(bLower)) return true;
        // Simple word overlap (Jaccard)
        Set<String> aWords = Set.of(aLower.split("\\W+"));
        Set<String> bWords = Set.of(bLower.split("\\W+"));
        Set<String> inter = new HashSet<>(aWords); inter.retainAll(bWords);
        Set<String> union = new HashSet<>(aWords); union.addAll(bWords);
        return union.isEmpty() ? false : (double) inter.size() / union.size() >= threshold;
    }

    private static final java.util.stream.IntStream IntStream =
            java.util.stream.IntStream.range(0, 0);

    // ── Data types ─────────────────────────────────────────────────────────

    public enum SynthesisStrategy { MAJORITY_VOTE, BEST_SCORE, LLM_SYNTHESIS, FIRST_SUCCESS, ALL_AGREE }

    public record ModelAnswer(String model, String answer, boolean success, Duration latency, String error) {
        static ModelAnswer failed(String model, String error) {
            return new ModelAnswer(model, "", false, Duration.ZERO, error);
        }
    }

    public record EnsembleResult(
            List<ModelAnswer> answers,
            String            synthesized,
            SynthesisStrategy strategy,
            Duration          elapsed,
            boolean           allSucceeded
    ) {
        static EnsembleResult empty(String reason) {
            return new EnsembleResult(List.of(), "", SynthesisStrategy.FIRST_SUCCESS,
                    Duration.ZERO, false);
        }
        public int successCount() { return (int) answers.stream().filter(ModelAnswer::success).count(); }
    }
}
