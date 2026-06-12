package tech.kayys.wayang.agent.orchestration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reflexion orchestration strategy — iterative self-evaluation and improvement.
 *
 * <p>Inspired by the <em>Reflexion</em> paper (Shinn et al., 2023). The agent
 * produces an initial answer, evaluates it against a set of criteria (or the
 * user-provided rubric), generates actionable reflections, and then attempts
 * an improved answer. This loop repeats until the evaluation passes or the
 * maximum reflection iterations are reached.</p>
 *
 * <h2>Algorithm</h2>
 * <pre>
 * attempt = 0
 * answer  = initial_answer(prompt)
 * loop:
 *   evaluation = evaluate(prompt, answer, rubric)
 *   if evaluation.passed OR attempt >= max_reflections: break
 *   reflection = reflect(prompt, answer, evaluation)
 *   answer     = revise(prompt, answer, reflection)
 *   attempt++
 * return answer
 * </pre>
 *
 * <h2>When to use</h2>
 * <ul>
 *   <li>Creative writing with quality criteria (tone, length, style)</li>
 *   <li>Code generation with test-based feedback</li>
 *   <li>Factual responses requiring self-consistency checks</li>
 *   <li>Structured output that must conform to a schema</li>
 * </ul>
 *
 * <h2>Request parameters</h2>
 * <ul>
 *   <li>{@code rubric} – optional evaluation rubric injected into the evaluator prompt</li>
 *   <li>{@code max_reflections} – max iterations (default 3)</li>
 *   <li>{@code evaluation_threshold} – "pass" keyword the evaluator must output (default "PASS")</li>
 * </ul>
 */
@ApplicationScoped
public class ReflexionOrchestrator implements AgentOrchestrator {

    private static final Logger LOG = Logger.getLogger(ReflexionOrchestrator.class);

    @Inject InferenceService inferenceService;
    @Inject SkillRegistry skillRegistry;

    @Override
    public String strategyId() { return "reflexion"; }

    // ── Main entry ───────────────────────────────────────────────────────────

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        String runId = UUID.randomUUID().toString();
        Instant start = Instant.now();

        int maxReflections = getIntParam(request, "max_reflections", 3);
        String rubric = getStrParam(request, "rubric", "");
        String passWord = getStrParam(request, "evaluation_threshold", "PASS");

        return generateInitialAnswer(request, runId)
                .chain(initial -> reflectionLoop(request, runId, initial, rubric, passWord, maxReflections, 0, new ArrayList<>()))
                .map(ctx -> AgentResponse.builder()
                        .runId(runId)
                        .requestId(request.requestId())
                        .answer(ctx.finalAnswer())
                        .totalSteps(ctx.steps())
                        .successful(true)
                        .strategy(strategyId())
                        .durationMs(Duration.between(start, Instant.now()).toMillis())
                        .build())
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "Reflexion run %s failed", runId);
                    return AgentResponse.builder()
                            .runId(runId).requestId(request.requestId())
                            .answer("").successful(false).error(err.getMessage())
                            .strategy(strategyId())
                            .durationMs(Duration.between(start, Instant.now()).toMillis())
                            .build();
                })
                .invoke(() -> {});
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            String runId = UUID.randomUUID().toString();
            emitter.emit(AgentEvent.runStart(runId, strategyId()));
            execute(request).subscribe().with(
                    resp -> {
                        emitter.emit(AgentEvent.finalAnswer(runId, resp.totalSteps(), resp.answer()));
                        emitter.emit(AgentEvent.runComplete(runId, resp.totalSteps(), resp.successful(), resp.durationMs()));
                        emitter.complete();
                    },
                    err -> {
                        emitter.emit(AgentEvent.log(runId, 0, "ERROR: " + err.getMessage()));
                        emitter.complete();
                    });
        });
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        throw new UnsupportedOperationException("Reflexion does not support step-by-step execution. Use execute().");
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return state.getPhase() == AgentState.Phase.COMPLETE || state.getPhase() == AgentState.Phase.FAILED;
    }

    @Override
    public String getSystemPromptFragment() {
        return """
                You are a self-improving reasoning assistant. You produce answers,
                evaluate them critically, reflect on improvements, and revise until
                the answer meets quality criteria. Be direct and rigorous in evaluation.
                """;
    }

    // ── Reflexion steps ────────────────────────────────────────────────────────

    /** Generate the first-pass answer. */
    private Uni<String> generateInitialAnswer(AgentRequest request, String runId) {
        InferenceRequest ir = InferenceRequest.builder()
                .requestId(runId + "-initial")
                .model(request.modelId() != null ? request.modelId() : "default")
                .message(Message.system(getSystemPromptFragment()))
                .message(Message.user(request.prompt()))
                .parameter("max_tokens", 512)
                .parameter("temperature", 0.7)
                .metadata("tenantId", request.tenantId())
                .build();

        return inferenceService.inferAsync(ir).map(r -> r.getContent());
    }

    /** Evaluate whether the current answer meets criteria. Returns evaluation text. */
    private Uni<String> evaluate(AgentRequest request, String runId, int step,
                                  String answer, String rubric) {
        String evalPrompt = """
                Original question: %s
                
                Current answer:
                %s
                
                Evaluation criteria: %s
                
                Evaluate the answer above. Identify specific flaws, gaps, or inaccuracies.
                End your evaluation with exactly one of: PASS or FAIL.
                """.formatted(request.prompt(), answer,
                rubric.isBlank() ? "accuracy, completeness, and clarity" : rubric);

        InferenceRequest ir = InferenceRequest.builder()
                .requestId(runId + "-eval-" + step)
                .model(request.modelId() != null ? request.modelId() : "default")
                .message(Message.system("You are a strict evaluator. Be concise."))
                .message(Message.user(evalPrompt))
                .parameter("max_tokens", 256)
                .parameter("temperature", 0.2)
                .metadata("tenantId", request.tenantId())
                .build();

        return inferenceService.inferAsync(ir).map(r -> r.getContent());
    }

    /** Generate a reflection — actionable critique for improving the answer. */
    private Uni<String> reflect(AgentRequest request, String runId, int step,
                                 String answer, String evaluation) {
        String reflectPrompt = """
                Question: %s
                
                Answer to improve:
                %s
                
                Evaluation feedback:
                %s
                
                Provide a concise, actionable reflection: what should be changed and why?
                """.formatted(request.prompt(), answer, evaluation);

        InferenceRequest ir = InferenceRequest.builder()
                .requestId(runId + "-reflect-" + step)
                .model(request.modelId() != null ? request.modelId() : "default")
                .message(Message.system("You are a critical thinking assistant."))
                .message(Message.user(reflectPrompt))
                .parameter("max_tokens", 256)
                .parameter("temperature", 0.4)
                .metadata("tenantId", request.tenantId())
                .build();

        return inferenceService.inferAsync(ir).map(r -> r.getContent());
    }

    /** Revise the answer using the reflection. */
    private Uni<String> revise(AgentRequest request, String runId, int step,
                                String answer, String reflection) {
        String revisePrompt = """
                Original question: %s
                
                Previous answer:
                %s
                
                Reflection / improvement notes:
                %s
                
                Write an improved answer that addresses all the issues above.
                """.formatted(request.prompt(), answer, reflection);

        InferenceRequest ir = InferenceRequest.builder()
                .requestId(runId + "-revise-" + step)
                .model(request.modelId() != null ? request.modelId() : "default")
                .message(Message.system(getSystemPromptFragment()))
                .message(Message.user(revisePrompt))
                .parameter("max_tokens", 512)
                .parameter("temperature", 0.6)
                .metadata("tenantId", request.tenantId())
                .build();

        return inferenceService.inferAsync(ir).map(r -> r.getContent());
    }

    // ── Recursion ──────────────────────────────────────────────────────────────

    private Uni<ReflexionContext> reflectionLoop(
            AgentRequest request, String runId, String currentAnswer,
            String rubric, String passWord, int maxReflections,
            int attempt, List<String> history) {

        if (attempt >= maxReflections) {
            LOG.infof("Reflexion run %s: max iterations (%d) reached", runId, maxReflections);
            return Uni.createFrom().item(new ReflexionContext(currentAnswer, attempt * 3));
        }

        return evaluate(request, runId, attempt, currentAnswer, rubric)
                .chain(evaluation -> {
                    history.add("Attempt " + (attempt + 1) + " evaluation: " + evaluation);

                    if (evaluation.toUpperCase().contains(passWord.toUpperCase())) {
                        LOG.infof("Reflexion run %s: PASSED at attempt %d", runId, attempt + 1);
                        return Uni.createFrom().item(new ReflexionContext(currentAnswer, (attempt + 1) * 3));
                    }

                    // Failed — reflect and revise
                    return reflect(request, runId, attempt, currentAnswer, evaluation)
                            .chain(reflection -> revise(request, runId, attempt, currentAnswer, reflection))
                            .chain(revised -> reflectionLoop(
                                    request, runId, revised, rubric, passWord,
                                    maxReflections, attempt + 1, history));
                });
    }

    private record ReflexionContext(String finalAnswer, int steps) {}

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int getIntParam(AgentRequest req, String key, int def) {
        Object v = req.parameters() != null ? req.parameters().get(key) : null;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private String getStrParam(AgentRequest req, String key, String def) {
        Object v = req.parameters() != null ? req.parameters().get(key) : null;
        return v instanceof String s ? s : def;
    }
}
