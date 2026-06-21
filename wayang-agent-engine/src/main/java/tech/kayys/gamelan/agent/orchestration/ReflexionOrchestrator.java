package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Tier 2 variant — Reflexion (iterative self-evaluation and improvement).
 *
 * <p>Inspired by <em>Reflexion: Language Agents with Verbal Reinforcement
 * Learning</em> (Shinn et al., 2023). The agent produces an initial answer,
 * evaluates it against criteria, reflects on what to improve, and revises.
 * This loop repeats until the evaluator says PASS or max reflections is reached.
 *
 * <h2>Algorithm</h2>
 * <pre>
 * answer = generate(task)
 * for attempt in range(max_reflections):
 *     eval = evaluate(task, answer, rubric)
 *     if "PASS" in eval: break
 *     reflection = reflect(task, answer, eval)
 *     answer = revise(task, answer, reflection)
 * return answer
 * </pre>
 *
 * <h2>When to use over plain ReAct</h2>
 * <ul>
 *   <li>Creative writing with quality criteria (tone, length, style)</li>
 *   <li>Code generation where you want self-consistency checking</li>
 *   <li>Structured output that must conform to a schema or rubric</li>
 *   <li>When first-pass quality is known to be unreliable</li>
 * </ul>
 *
 * <h2>Request parameters</h2>
 * <ul>
 *   <li>{@code rubric} — evaluation criteria (default: accuracy and clarity)</li>
 *   <li>{@code max_reflections} — max revision iterations (default: 3)</li>
 *   <li>{@code pass_keyword} — keyword evaluator must output to stop (default: PASS)</li>
 * </ul>
 */
@ApplicationScoped
public class ReflexionOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReflexionOrchestrator.class);

    @Inject GollekSdk     sdk;
    @Inject GamelanConfig config;

    @Override public String strategyId()    { return "reflexion"; }
    @Override public String displayName()   { return "Reflexion (iterative self-improvement)"; }
    @Override public boolean supportsTools(){ return false; } // focuses on self-improvement, not tool use

    @Override
    public String description() {
        return "Generate → evaluate → reflect → revise loop. "
                + "Use for quality-sensitive tasks: creative writing, schema validation, self-consistency.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        Instant start         = Instant.now();
        String  model         = resolveModel(request);
        int     maxReflections = request.param("max_reflections", 3);
        String  rubric        = request.param("rubric", "");
        String  passKeyword   = request.param("pass_keyword", "PASS");

        log.info("[reflexion] model={} maxReflections={}", model, maxReflections);

        try {
            // Step 1: Initial answer
            String answer = generate(model, "You are a helpful assistant.", request.task(),
                    request.systemExtra(), 0.7);
            log.debug("[reflexion] initial answer len={}", answer.length());

            // Step 2: Evaluate → reflect → revise loop
            int stepsUsed = 1;
            for (int attempt = 0; attempt < maxReflections; attempt++) {
                String evaluation = evaluate(model, request.task(), answer, rubric, attempt);
                stepsUsed++;

                if (evaluation.toUpperCase().contains(passKeyword.toUpperCase())) {
                    log.info("[reflexion] PASSED at attempt {}", attempt + 1);
                    break;
                }

                String reflection = reflect(model, request.task(), answer, evaluation, attempt);
                answer = revise(model, request.task(), answer, reflection, attempt);
                stepsUsed += 2;
                log.debug("[reflexion] revised at attempt {} len={}", attempt + 1, answer.length());
            }

            return OrchestratorResult.ok(answer, strategyId(), stepsUsed,
                    List.of(), Duration.between(start, Instant.now()));

        } catch (Exception e) {
            log.error("[reflexion] failed: {}", e.getMessage());
            return OrchestratorResult.failure(strategyId(), e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
    }

    // ── LLM steps ──────────────────────────────────────────────────────────

    private String generate(String model, String system, String task,
                             String extra, double temp) throws SdkException {
        String prompt = task + (extra.isBlank() ? "" : "\n\n" + extra);
        return call(model, system, prompt, temp, 1024);
    }

    private String evaluate(String model, String task, String answer,
                             String rubric, int attempt) throws SdkException {
        String criteria = rubric.isBlank() ? "accuracy, completeness, and clarity" : rubric;
        String prompt = """
                Original task: %s

                Current answer:
                %s

                Evaluation criteria: %s

                Evaluate the answer. Identify specific flaws, gaps, or inaccuracies.
                End your evaluation with exactly one word on its own line: PASS or FAIL.
                """.formatted(task, answer, criteria);
        return call(model, "You are a strict evaluator. Be concise and rigorous.", prompt, 0.2, 256);
    }

    private String reflect(String model, String task, String answer,
                            String evaluation, int attempt) throws SdkException {
        String prompt = """
                Task: %s

                Answer to improve:
                %s

                Evaluation:
                %s

                Write concise, actionable reflection: exactly what should change and why.
                """.formatted(task, answer, evaluation);
        return call(model, "You are a critical thinking assistant.", prompt, 0.4, 256);
    }

    private String revise(String model, String task, String answer,
                           String reflection, int attempt) throws SdkException {
        String prompt = """
                Original task: %s

                Previous answer:
                %s

                Improvement notes:
                %s

                Write an improved answer that addresses all the issues above.
                """.formatted(task, answer, reflection);
        return call(model, "You are a helpful assistant.", prompt, 0.6, 1024);
    }

    private String call(String model, String system, String userMsg,
                        double temp, int maxTokens) throws SdkException {
        InferenceResponse resp = sdk.createCompletion(
                InferenceRequest.builder()
                        .model(model)
                        .message(Message.system(system))
                        .messages(List.of(Message.user(userMsg)))
                        .temperature(temp)
                        .maxTokens(maxTokens)
                        .streaming(false)
                        .build());
        return resp.getContent() != null ? resp.getContent() : "";
    }

    private String resolveModel(AgentRequest request) {
        return (request.model() != null && !request.model().isBlank())
                ? request.model() : config.defaultModel();
    }
}
