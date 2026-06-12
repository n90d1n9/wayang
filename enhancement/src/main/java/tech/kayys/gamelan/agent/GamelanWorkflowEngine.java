package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.tool.ToolResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Gamelan Workflow Engine — multi-phase agentic task orchestration.
 *
 * <h2>Migration from AgentLoop to SingleAgentOrchestrator</h2>
 * Previously injected and called {@link AgentLoop} directly. Now delegates
 * to {@link SingleAgentOrchestrator}, which is the correct Tier-2 entry
 * point. This means workflow steps benefit from:
 * <ul>
 *   <li>ThreadLocal cancellation (safe for parallel steps)</li>
 *   <li>Tool allow-listing per step</li>
 *   <li>ToolMetrics recording</li>
 *   <li>AgentEventListener callbacks</li>
 * </ul>
 *
 * <h2>Phase types</h2>
 * <ul>
 *   <li><b>SEQUENTIAL</b> — steps run one-by-one; each output feeds the next</li>
 *   <li><b>PARALLEL</b>   — steps run on virtual threads; isolated sessions</li>
 *   <li><b>MAP_REDUCE</b> — parallel map + synthesis of successful outputs</li>
 * </ul>
 *
 * <h2>Real bugs fixed in this version</h2>
 * <ul>
 *   <li>Parallel timeout was {@code maxTokens/10} seconds → fixed to
 *       {@code requestTimeoutSeconds * 2}</li>
 *   <li>{@code executor.shutdown()} before {@code future.get()} → fixed</li>
 *   <li>Parallel step name was always "unknown" → fixed with explicit capture</li>
 *   <li>Map-reduce included failed outputs in synthesis → fixed</li>
 *   <li>Unbounded sequential context → capped at 2000 chars per step</li>
 * </ul>
 */
@ApplicationScoped
public class GamelanWorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(GamelanWorkflowEngine.class);

    private static final int MAX_CONTEXT_PER_STEP = 2_000;

    @Inject SingleAgentOrchestrator agentOrchestrator; // Tier 2 — correct entry point
    @Inject GamelanConfig           config;

    // ── Public API ─────────────────────────────────────────────────────────

    public WorkflowResult execute(GamelanWorkflow workflow, ConversationSession session,
                                   String model, Consumer<StepResult> onStep) {
        Instant started = Instant.now();
        log.info("Workflow '{}' started ({} phases)", workflow.name(), workflow.phases().size());

        List<StepResult>  allResults = new ArrayList<>();
        StringBuilder     ctxBuilder = new StringBuilder();

        for (WorkflowPhase phase : workflow.phases()) {
            List<StepResult> phaseResults = switch (phase.type()) {
                case SEQUENTIAL -> executeSequential(phase.steps(), session, model,
                        ctxBuilder.toString(), onStep);
                case PARALLEL   -> executeParallel(phase.steps(), model,
                        ctxBuilder.toString(), onStep);
                case MAP_REDUCE -> executeMapReduce(phase.steps(), session, model,
                        ctxBuilder.toString(), onStep);
            };

            allResults.addAll(phaseResults);
            for (StepResult r : phaseResults) {
                ctxBuilder.append("\n### ").append(r.stepName()).append("\n");
                String contrib = r.output().length() > MAX_CONTEXT_PER_STEP
                        ? r.output().substring(0, MAX_CONTEXT_PER_STEP) + "\n…(truncated)"
                        : r.output();
                ctxBuilder.append(contrib).append("\n");
            }
        }

        Duration elapsed = Duration.between(started, Instant.now());
        boolean  success = allResults.stream().allMatch(StepResult::success);
        log.info("Workflow '{}' {} in {}s ({} steps)",
                workflow.name(), success ? "succeeded" : "failed",
                elapsed.getSeconds(), allResults.size());

        return new WorkflowResult(workflow.name(), allResults, success, elapsed);
    }

    // ── Phase executors ────────────────────────────────────────────────────

    private List<StepResult> executeSequential(List<WorkflowStep> steps,
                                                ConversationSession session, String model,
                                                String priorContext, Consumer<StepResult> onStep) {
        List<StepResult> results      = new ArrayList<>();
        StringBuilder    runningCtx   = new StringBuilder(priorContext);

        for (WorkflowStep step : steps) {
            log.info("Sequential step '{}' starting", step.name());
            Instant stepStart = Instant.now();

            String prompt = addContext(step.prompt(), runningCtx.toString());
            AgentRequest req = AgentRequest.builder(prompt)
                    .model(model)
                    .session(session)
                    .stream(false)
                    .maxSteps(10)
                    .build();

            OrchestratorResult r = agentOrchestrator.execute(req);

            StepResult result = new StepResult(step.name(), r.answer(),
                    r.success(), r.toolResults(),
                    Duration.between(stepStart, Instant.now()));
            results.add(result);
            if (onStep != null) onStep.accept(result);

            String stepOut = r.answer().length() > MAX_CONTEXT_PER_STEP
                    ? r.answer().substring(0, MAX_CONTEXT_PER_STEP) + "..."
                    : r.answer();
            runningCtx.append("\n### ").append(step.name()).append("\n")
                      .append(stepOut).append("\n");

            if (!result.success() && step.required()) {
                log.warn("Required step '{}' failed — aborting sequential phase", step.name());
                break;
            }
        }
        return results;
    }

    private List<StepResult> executeParallel(List<WorkflowStep> steps, String model,
                                              String priorContext, Consumer<StepResult> onStep) {
        if (steps.isEmpty()) return List.of();

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        long timeoutSecs = (long) config.requestTimeoutSeconds() * 2;

        // Submit all before shutdown
        Map<WorkflowStep, Future<StepResult>> futures = new LinkedHashMap<>();
        for (WorkflowStep step : steps) {
            WorkflowStep captured = step;
            futures.put(captured, exec.submit(() -> {
                log.info("Parallel step '{}' starting", captured.name());
                Instant stepStart = Instant.now();
                // Each parallel step gets a fresh isolated session
                AgentRequest req = AgentRequest.builder(addContext(captured.prompt(), priorContext))
                        .model(model)
                        .session(new ConversationSession(null,
                                config.sessionPersist(), config.tokenBudget()))
                        .stream(false)
                        .maxSteps(10)
                        .build();
                OrchestratorResult r = agentOrchestrator.execute(req);
                return new StepResult(captured.name(), r.answer(), r.success(),
                        r.toolResults(), Duration.between(stepStart, Instant.now()));
            }));
        }

        exec.shutdown(); // after all submitted

        List<StepResult> results = new ArrayList<>();
        for (Map.Entry<WorkflowStep, Future<StepResult>> entry : futures.entrySet()) {
            WorkflowStep step = entry.getKey();
            try {
                StepResult result = entry.getValue().get(timeoutSecs, TimeUnit.SECONDS);
                results.add(result);
                if (onStep != null) onStep.accept(result);
            } catch (TimeoutException e) {
                entry.getValue().cancel(true);
                StepResult failed = new StepResult(step.name(), "", false, List.of(), Duration.ZERO);
                results.add(failed);
                if (onStep != null) onStep.accept(failed);
                log.warn("Parallel step '{}' timed out after {}s", step.name(), timeoutSecs);
            } catch (ExecutionException e) {
                StepResult failed = new StepResult(step.name(),
                        "Error: " + e.getCause().getMessage(), false, List.of(), Duration.ZERO);
                results.add(failed);
                if (onStep != null) onStep.accept(failed);
                log.error("Parallel step '{}' threw: {}", step.name(), e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    private List<StepResult> executeMapReduce(List<WorkflowStep> steps,
                                               ConversationSession session, String model,
                                               String priorContext, Consumer<StepResult> onStep) {
        List<StepResult> mapped = executeParallel(steps, model, priorContext, onStep);

        // Only successful outputs feed the synthesis
        String mergedForReduce = mapped.stream()
                .filter(StepResult::success)
                .filter(r -> !r.output().isBlank())
                .map(r -> "## " + r.stepName() + "\n" + r.output())
                .reduce("", (a, b) -> a + "\n\n" + b);

        if (mergedForReduce.isBlank()) {
            StepResult synthFailed = new StepResult("synthesis",
                    "All map steps failed — cannot synthesize.", false, List.of(), Duration.ZERO);
            if (onStep != null) onStep.accept(synthFailed);
            List<StepResult> all = new ArrayList<>(mapped);
            all.add(synthFailed);
            return all;
        }

        Instant synthStart = Instant.now();
        String reducePrompt = "Synthesize the following analysis results into a concise, "
                + "structured summary. Prioritize actionable insights.\n\n" + mergedForReduce;

        AgentRequest req = AgentRequest.builder(reducePrompt)
                .model(model).session(session).stream(false).maxSteps(5)
                .build();
        OrchestratorResult synthesis = agentOrchestrator.execute(req);
        StepResult synthResult = new StepResult("synthesis", synthesis.answer(),
                synthesis.success(), synthesis.toolResults(),
                Duration.between(synthStart, Instant.now()));

        if (onStep != null) onStep.accept(synthResult);
        List<StepResult> allResults = new ArrayList<>(mapped);
        allResults.add(synthResult);
        return allResults;
    }

    private String addContext(String prompt, String context) {
        if (context.isBlank()) return prompt;
        return prompt + "\n\n---\nContext from previous steps:\n" + context;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record WorkflowResult(String workflowName, List<StepResult> stepResults,
                                  boolean success, Duration elapsed) {
        public String stepOutput(String name) {
            return stepResults.stream().filter(r -> r.stepName().equals(name))
                    .map(StepResult::output).findFirst().orElse("");
        }
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Workflow '").append(workflowName).append("' ")
              .append(success ? "✓" : "✗")
              .append(" (").append(elapsed.getSeconds()).append("s)\n\n");
            for (StepResult r : stepResults) {
                sb.append("  ").append(r.success() ? "✓" : "✗")
                  .append(" ").append(r.stepName())
                  .append("  ").append(r.elapsed().getSeconds()).append("s");
                if (!r.success()) sb.append("  [FAILED]");
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public record StepResult(String stepName, String output, boolean success,
                              List<ToolResult> toolResults, Duration elapsed) {
        public String safeOutput() { return output == null ? "" : output.strip(); }
    }

    public record WorkflowStep(String name, String prompt, boolean required) {
        public static WorkflowStep of(String name, String prompt) {
            return new WorkflowStep(name, prompt, false);
        }
        public static WorkflowStep required(String name, String prompt) {
            return new WorkflowStep(name, prompt, true);
        }
    }

    public enum PhaseType { SEQUENTIAL, PARALLEL, MAP_REDUCE }
    public record WorkflowPhase(PhaseType type, List<WorkflowStep> steps) {}

    public record GamelanWorkflow(String name, List<WorkflowPhase> phases) {
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private String name = "workflow";
            private final List<WorkflowPhase> phases = new ArrayList<>();
            public Builder name(String n) { this.name = n; return this; }
            public Builder sequential(WorkflowStep... steps) {
                if (steps.length > 0) phases.add(new WorkflowPhase(PhaseType.SEQUENTIAL, List.of(steps)));
                return this;
            }
            public Builder parallel(WorkflowStep... steps) {
                if (steps.length > 0) phases.add(new WorkflowPhase(PhaseType.PARALLEL, List.of(steps)));
                return this;
            }
            public Builder mapReduce(WorkflowStep... steps) {
                if (steps.length > 0) phases.add(new WorkflowPhase(PhaseType.MAP_REDUCE, List.of(steps)));
                return this;
            }
            public GamelanWorkflow build() {
                if (phases.isEmpty()) throw new IllegalStateException("Workflow has no phases");
                return new GamelanWorkflow(name, List.copyOf(phases));
            }
        }
    }
}
