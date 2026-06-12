package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline Orchestrator — composable linear sequence of agent steps.
 *
 * <p>Unlike {@link tech.kayys.gamelan.agent.GamelanWorkflowEngine} which runs
 * multi-phase workflows defined at the call site, the pipeline orchestrator
 * lets you define a reusable sequence of prompt templates that transform
 * input progressively — each step's output becomes the next step's input.
 *
 * <h2>When to use over the workflow engine</h2>
 * <ul>
 *   <li>Fixed transformation chains: extract → summarise → classify</li>
 *   <li>Code generation pipelines: analyse → plan → implement → verify</li>
 *   <li>Data processing: parse → validate → transform → format</li>
 *   <li>When each step needs the full output of the previous step</li>
 * </ul>
 *
 * <h2>Template variables</h2>
 * Step prompts may include {@code {input}} which is replaced with the
 * original task, and {@code {prev}} which is replaced with the previous
 * step's output.
 *
 * <pre>{@code
 * var pipeline = Pipeline.of(
 *   "Extract all TODO comments from this codebase: {input}",
 *   "Prioritise the TODOs from previous output by severity: {prev}",
 *   "Generate GitHub issues JSON for the top 5: {prev}"
 * );
 * OrchestratorResult result = pipeline.execute(
 *   AgentRequest.builder("src/").build(),
 *   pipelineOrchestrator);
 * }</pre>
 */
@ApplicationScoped
public class PipelineOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    @Inject SingleAgentOrchestrator agentStep;
    @Inject GamelanConfig           config;

    @Override public String strategyId()  { return "pipeline"; }
    @Override public String displayName() { return "Pipeline (sequential transformation)"; }

    @Override
    public String description() {
        return "Composable linear pipeline: each step's output feeds the next. "
                + "Use for fixed transformation chains like extract→summarise→classify.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        // Extract pipeline steps from params
        @SuppressWarnings("unchecked")
        List<String> steps = request.param("steps", List.of());

        if (steps.isEmpty()) {
            // Fall back to single-step with the task
            return agentStep.execute(request);
        }

        return execute(request, steps);
    }

    /**
     * Execute a pipeline defined as a list of prompt templates.
     *
     * @param request base request (supplies model, session, stream, allowedTools)
     * @param steps   list of prompt templates; may use {@code {input}} and {@code {prev}}
     * @return the final step's result
     */
    public OrchestratorResult execute(AgentRequest request, List<String> steps) {
        Instant start = Instant.now();
        String original = request.task();
        String prev = "";
        List<tech.kayys.gamelan.tool.ToolResult> allTools = new ArrayList<>();

        log.info("[pipeline] starting {} steps", steps.size());

        OrchestratorResult lastResult = null;
        for (int i = 0; i < steps.size(); i++) {
            String stepPrompt = steps.get(i)
                    .replace("{input}", original)
                    .replace("{prev}", prev);

            log.info("[pipeline] step {}/{}", i + 1, steps.size());

            AgentRequest stepReq = AgentRequest.builder(stepPrompt)
                    .model(request.model())
                    .session(new ConversationSession(null,
                            config.sessionPersist(), config.tokenBudget()))
                    .stream(request.stream() && i == steps.size() - 1) // stream last step only
                    .maxSteps(request.maxSteps())
                    .allowedTools(request.allowedTools())
                    .build();

            lastResult = agentStep.execute(stepReq);
            if (!lastResult.success()) {
                log.warn("[pipeline] step {} failed — aborting", i + 1);
                return OrchestratorResult.failure(strategyId(),
                        "Pipeline aborted at step " + (i + 1) + ": " + lastResult.error(),
                        Duration.between(start, Instant.now()));
            }

            prev = lastResult.answer();
            allTools.addAll(lastResult.toolResults());
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[pipeline] completed {} steps in {}ms", steps.size(), elapsed.toMillis());

        return OrchestratorResult.ok(
                prev, strategyId(), steps.size(), allTools, elapsed);
    }

    // ── Fluent builder for pipelines ───────────────────────────────────────

    /**
     * Defines a reusable pipeline as a list of prompt templates.
     *
     * <pre>{@code
     * Pipeline.of("analyse {input}", "summarise {prev}", "classify {prev}")
     *         .execute(request, pipelineOrchestrator);
     * }</pre>
     */
    public static final class Pipeline {
        private final List<String> steps;

        private Pipeline(List<String> steps) { this.steps = List.copyOf(steps); }

        public static Pipeline of(String... steps) { return new Pipeline(List.of(steps)); }

        public static Builder builder()           { return new Builder(); }

        public OrchestratorResult execute(AgentRequest base, PipelineOrchestrator orch) {
            return orch.execute(base, steps);
        }

        public static final class Builder {
            private final List<String> steps = new ArrayList<>();
            public Builder step(String promptTemplate) { steps.add(promptTemplate); return this; }
            public Pipeline build()                    { return new Pipeline(steps); }
        }
    }
}
