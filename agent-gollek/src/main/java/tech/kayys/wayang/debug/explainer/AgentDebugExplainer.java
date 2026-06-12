package tech.kayys.gamelan.debug.explainer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.execution.dag.DagExecutionEngine;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.*;
import java.util.*;
import java.util.stream.*;

/**
 * AgentDebugExplainer — produces human-readable explanations for agent failures and decisions.
 *
 * <h2>The debuggability gap</h2>
 * When an agent fails, the developer sees a wall of LLM output and tool results.
 * Understanding WHY it failed requires:
 * <ul>
 *   <li>Knowing which decision led to the failure</li>
 *   <li>Understanding what information was (and wasn't) in context</li>
 *   <li>Seeing the counterfactual: what should have happened</li>
 *   <li>Tracing the causal chain from task to failure</li>
 * </ul>
 *
 * <h2>Explanation modes</h2>
 * <ol>
 *   <li><b>Failure analysis</b>: root cause, contributing factors, suggested fix</li>
 *   <li><b>Decision trace</b>: step-by-step narration of what the agent did and why</li>
 *   <li><b>Context diff</b>: what context was present vs what was needed</li>
 *   <li><b>Similar failures</b>: past episodes with the same failure pattern</li>
 *   <li><b>Counterfactual</b>: "if X had been in context, the agent would have..."</li>
 * </ol>
 *
 * <h2>Output formats</h2>
 * <ul>
 *   <li>Terminal: ANSI-formatted with collapsible sections</li>
 *   <li>Markdown: for issue reports and documentation</li>
 *   <li>JSON: for programmatic consumption by dashboards</li>
 * </ul>
 */
@ApplicationScoped
public class AgentDebugExplainer {

    private static final Logger log = LoggerFactory.getLogger(AgentDebugExplainer.class);

    @Inject GollekSdk      sdk;
    @Inject GamelanConfig  config;
    @Inject EpisodicMemory episodic;
    @Inject AgentTelemetry telemetry;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Produces a comprehensive failure report for a failed orchestrator result.
     *
     * @param task   the original task that failed
     * @param result the failed orchestrator result
     * @return a structured failure report
     */
    public FailureReport explainFailure(String task, OrchestratorResult result) {
        log.info("[debug] generating failure report for task: {}", truncate(task, 60));

        // 1. Classify the failure type
        FailureType type = classifyFailure(result);

        // 2. Extract contributing factors
        List<ContributingFactor> factors = extractFactors(task, result);

        // 3. Find similar past failures
        List<EpisodicMemory.Episode> similar = findSimilarFailures(task, type);

        // 4. Generate LLM explanation
        String llmExplanation = generateExplanation(task, result, type, factors);

        // 5. Suggest remediation
        List<RemediationStep> steps = suggestRemediation(type, factors, result);

        // 6. Build timeline
        List<TimelineEvent> timeline = buildTimeline(task, result);

        return new FailureReport(task, type, llmExplanation, factors, steps,
                similar, timeline, Instant.now());
    }

    /**
     * Explains a DAG execution result — which nodes failed, why, and the impact.
     */
    public DagExplanation explainDag(DagExecutionEngine.DagResult dagResult) {
        List<NodeExplanation> nodeExplanations = dagResult.nodeResults().values().stream()
                .map(nr -> {
                    FailureType type = nr.success() ? FailureType.NONE
                            : classifyNodeFailure(nr.error());
                    String advice = nr.success() ? "" : adviceForType(type, nr.error());
                    return new NodeExplanation(nr.nodeId(), nr.success(),
                            nr.output(), nr.error(), type, advice, nr.elapsed());
                })
                .toList();

        // Compute cascade: which nodes were skipped because of which failures
        Map<String, List<String>> cascadeMap = computeCascade(dagResult);

        String summary = buildDagSummary(dagResult, nodeExplanations, cascadeMap);

        return new DagExplanation(dagResult.dag().nodes().size(),
                dagResult.successCount(), dagResult.failureCount(),
                nodeExplanations, cascadeMap, summary, dagResult.elapsed());
    }

    /**
     * Produces a decision trace — a step-by-step narration of the agent's reasoning.
     *
     * @param llmOutputs ordered list of LLM responses from the agent loop
     * @param toolNames  ordered list of tool calls made
     */
    public DecisionTrace traceDecisions(String task, List<String> llmOutputs,
                                         List<String> toolNames) {
        List<DecisionStep> steps = new ArrayList<>();

        for (int i = 0; i < llmOutputs.size(); i++) {
            String output = llmOutputs.get(i);
            String toolUsed = i < toolNames.size() ? toolNames.get(i) : null;
            DecisionType type = classifyDecision(output, toolUsed);
            String rationale = extractRationale(output);
            steps.add(new DecisionStep(i + 1, type, rationale, toolUsed,
                    output.length() > 200 ? output.substring(0, 200) + "…" : output));
        }

        String narrative = generateNarrative(task, steps);
        return new DecisionTrace(task, steps, narrative, steps.size());
    }

    /**
     * Diffs two plan versions to explain what changed and why.
     */
    public PlanDiffExplanation explainPlanDiff(HierarchicalTaskPlanner.Plan before,
                                                HierarchicalTaskPlanner.Plan after,
                                                String reason) {
        Set<String> beforeTasks = before.tasks().stream()
                .map(HierarchicalTaskPlanner.TaskNode::task).collect(java.util.stream.Collectors.toSet());
        Set<String> afterTasks = after.tasks().stream()
                .map(HierarchicalTaskPlanner.TaskNode::task).collect(java.util.stream.Collectors.toSet());

        Set<String> added   = new HashSet<>(afterTasks); added.removeAll(beforeTasks);
        Set<String> removed = new HashSet<>(beforeTasks); removed.removeAll(afterTasks);
        boolean modeChanged = before.mode() != after.mode();
        int tokenDelta      = after.estimatedTokens() - before.estimatedTokens();

        String explanation = buildPlanDiffExplanation(added, removed, modeChanged,
                before, after, tokenDelta, reason);

        return new PlanDiffExplanation(before, after, added, removed,
                modeChanged, tokenDelta, reason, explanation);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private FailureType classifyFailure(OrchestratorResult result) {
        String error = result.error() != null ? result.error().toLowerCase() : "";
        String answer = result.answer() != null ? result.answer().toLowerCase() : "";

        if (error.contains("timeout") || answer.contains("timed out")) return FailureType.TIMEOUT;
        if (error.contains("connection") || error.contains("network"))  return FailureType.NETWORK;
        if (error.contains("not found") || error.contains("no such"))   return FailureType.NOT_FOUND;
        if (error.contains("permission") || error.contains("access"))   return FailureType.PERMISSION;
        if (answer.contains("[llm_error]") || error.contains("llm"))    return FailureType.LLM_ERROR;
        if (answer.contains("stopped") || answer.contains("loop"))      return FailureType.LOOP_GUARD;
        if (error.contains("context") || error.contains("token"))       return FailureType.CONTEXT_OVERFLOW;
        return FailureType.UNKNOWN;
    }

    private FailureType classifyNodeFailure(String error) {
        if (error == null) return FailureType.UNKNOWN;
        String e = error.toLowerCase();
        if (e.contains("timeout"))   return FailureType.TIMEOUT;
        if (e.contains("[skipped]")) return FailureType.CASCADE;
        if (e.contains("not found")) return FailureType.NOT_FOUND;
        return FailureType.UNKNOWN;
    }

    private List<ContributingFactor> extractFactors(String task, OrchestratorResult result) {
        List<ContributingFactor> factors = new ArrayList<>();

        if (result.steps() > 8) {
            factors.add(new ContributingFactor("HIGH_ITERATION_COUNT",
                    "Task required " + result.steps() + " iterations — may indicate unclear task or missing context",
                    Severity.MEDIUM));
        }

        if (!result.toolResults().isEmpty()) {
            long toolErrors = result.toolResults().stream()
                    .filter(t -> !t.isSuccess()).count();
            if (toolErrors > 0) {
                factors.add(new ContributingFactor("TOOL_ERRORS",
                        toolErrors + " tool call(s) failed during execution",
                        Severity.HIGH));
            }
        }

        if (task.length() > 1000) {
            factors.add(new ContributingFactor("LONG_TASK",
                    "Task description is very long (" + task.length() + " chars) — consider decomposing",
                    Severity.LOW));
        }

        return factors;
    }

    private List<EpisodicMemory.Episode> findSimilarFailures(String task, FailureType type) {
        return episodic.recentFailures(20).stream()
                .filter(e -> !e.success())
                .filter(e -> type != FailureType.UNKNOWN ||
                        classifyNodeFailure(e.result()) == type)
                .limit(3)
                .toList();
    }

    private String generateExplanation(String task, OrchestratorResult result,
                                        FailureType type, List<ContributingFactor> factors) {
        String prompt = String.format("""
                An AI coding assistant failed to complete this task:
                Task: %s
                Failure type: %s
                Error: %s
                Contributing factors: %s
                
                In 2-3 sentences, explain WHY this likely failed and what the developer should do.
                Be specific and actionable. Avoid generic advice.
                """,
                truncate(task, 300), type,
                truncate(result.error() != null ? result.error() : result.answer(), 200),
                factors.stream().map(ContributingFactor::name)
                        .collect(Collectors.joining(", ")));
        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt("You explain AI agent failures concisely and specifically.")
                            .messages(List.of(Message.user(prompt)))
                            .temperature(0.2).maxTokens(200).streaming(false).build());
            return resp.getContent() != null ? resp.getContent().strip() : type.defaultExplanation();
        } catch (Exception e) {
            return type.defaultExplanation();
        }
    }

    private List<RemediationStep> suggestRemediation(FailureType type,
                                                       List<ContributingFactor> factors,
                                                       OrchestratorResult result) {
        List<RemediationStep> steps = new ArrayList<>();
        switch (type) {
            case TIMEOUT -> {
                steps.add(new RemediationStep(1, "Increase timeout", "gamelan config set request.timeout.seconds 180", Priority.HIGH));
                steps.add(new RemediationStep(2, "Decompose task", "Use 'gamelan workflow' for complex multi-step tasks", Priority.MEDIUM));
            }
            case NOT_FOUND -> {
                steps.add(new RemediationStep(1, "Verify path", "Run: gamelan run 'list all files in src/' to check structure", Priority.HIGH));
                steps.add(new RemediationStep(2, "Check cwd", "Ensure Gamelan is running from the project root", Priority.MEDIUM));
            }
            case CONTEXT_OVERFLOW -> {
                steps.add(new RemediationStep(1, "Increase budget", "gamelan config set token.budget 12000", Priority.HIGH));
                steps.add(new RemediationStep(2, "Clear history", "Use /compact or /clear in REPL", Priority.MEDIUM));
            }
            case LOOP_GUARD -> {
                steps.add(new RemediationStep(1, "Clarify task", "The task may be ambiguous — add more specifics", Priority.HIGH));
                steps.add(new RemediationStep(2, "Check tools", "Verify the required tools are available: gamelan skill list", Priority.LOW));
            }
            default -> steps.add(new RemediationStep(1, "Retry with verbose", "gamelan run --verbose '" + "retry task" + "'", Priority.MEDIUM));
        }
        return steps;
    }

    private List<TimelineEvent> buildTimeline(String task, OrchestratorResult result) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent(0, "Task received", truncate(task, 100), EventCategory.INFO));
        if (result.steps() > 0) {
            events.add(new TimelineEvent(1, "Iterations", result.steps() + " reasoning loops", EventCategory.INFO));
        }
        if (!result.toolResults().isEmpty()) {
            events.add(new TimelineEvent(2, "Tool calls", result.toolResults().size() + " tool executions", EventCategory.INFO));
        }
        events.add(new TimelineEvent(3, result.success() ? "Completed" : "Failed",
                result.success() ? "Task succeeded" : (result.error() != null ? result.error() : "Unknown error"),
                result.success() ? EventCategory.SUCCESS : EventCategory.ERROR));
        return events;
    }

    private Map<String, List<String>> computeCascade(DagExecutionEngine.DagResult dagResult) {
        Map<String, List<String>> cascade = new LinkedHashMap<>();
        dagResult.nodeResults().values().stream()
                .filter(nr -> !nr.success() && nr.output() != null && nr.output().contains("[SKIPPED]"))
                .forEach(nr -> {
                    // Find which failed node caused this skip
                    dagResult.dag().nodes().stream()
                            .filter(n -> n.id().equals(nr.nodeId()))
                            .flatMap(n -> n.dependencies().stream())
                            .forEach(dep -> cascade.computeIfAbsent(dep, k -> new ArrayList<>())
                                    .add(nr.nodeId()));
                });
        return cascade;
    }

    private String buildDagSummary(DagExecutionEngine.DagResult result,
                                    List<NodeExplanation> nodes,
                                    Map<String, List<String>> cascade) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("DAG executed %d/%d nodes successfully in %dms.%n",
                result.successCount(), result.dag().nodes().size(), result.elapsed().toMillis()));
        if (!cascade.isEmpty()) {
            sb.append("Cascade failures: ");
            cascade.forEach((parent, children) ->
                    sb.append(parent).append("→[").append(String.join(",", children)).append("] "));
        }
        return sb.toString();
    }

    private DecisionType classifyDecision(String output, String tool) {
        if (tool != null)              return DecisionType.TOOL_CALL;
        if (output.contains("STOP"))   return DecisionType.STOP;
        if (output.contains("?"))      return DecisionType.QUESTION;
        if (output.length() > 200)     return DecisionType.ANALYSIS;
        return DecisionType.RESPONSE;
    }

    private String extractRationale(String output) {
        String[] lines = output.split("\n");
        return Arrays.stream(lines)
                .filter(l -> l.contains("because") || l.contains("since") ||
                        l.contains("I'll") || l.contains("I need") || l.contains("First"))
                .findFirst()
                .orElse(lines.length > 0 ? lines[0] : output)
                .strip();
    }

    private String generateNarrative(String task, List<DecisionStep> steps) {
        return "Agent processed '" + truncate(task, 60) + "' in " + steps.size() + " steps: " +
                steps.stream().map(s -> s.type().toString().toLowerCase())
                        .collect(Collectors.joining(" → "));
    }

    private String adviceForType(FailureType type, String error) {
        return switch (type) {
            case TIMEOUT  -> "Increase timeout or simplify the step's task";
            case NOT_FOUND-> "Verify the file/resource path exists";
            case CASCADE  -> "Fix the prerequisite step's failure";
            default       -> error != null ? error : "Check logs for details";
        };
    }

    private String buildPlanDiffExplanation(Set<String> added, Set<String> removed,
                                              boolean modeChanged,
                                              HierarchicalTaskPlanner.Plan before,
                                              HierarchicalTaskPlanner.Plan after,
                                              int tokenDelta, String reason) {
        StringBuilder sb = new StringBuilder("Plan updated");
        if (reason != null) sb.append(" (").append(reason).append(")");
        sb.append(":\n");
        if (!added.isEmpty()) sb.append("+ Added: ").append(added).append("\n");
        if (!removed.isEmpty()) sb.append("- Removed: ").append(removed).append("\n");
        if (modeChanged) sb.append("~ Mode: ").append(before.mode()).append("→").append(after.mode()).append("\n");
        sb.append("~ Tokens: ").append(String.format("%+d", tokenDelta));
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum FailureType {
        NONE, TIMEOUT, NETWORK, NOT_FOUND, PERMISSION, LLM_ERROR,
        LOOP_GUARD, CONTEXT_OVERFLOW, CASCADE, UNKNOWN;

        public String defaultExplanation() {
            return switch (this) {
                case TIMEOUT          -> "The operation timed out. Try increasing the timeout or breaking the task into smaller steps.";
                case NOT_FOUND        -> "A required file or resource was not found. Verify the path and ensure you're running from the project root.";
                case LLM_ERROR        -> "The LLM engine returned an error. Check connectivity and model availability.";
                case LOOP_GUARD       -> "The agent repeated the same action too many times. Clarify the task or check for missing dependencies.";
                case CONTEXT_OVERFLOW -> "The conversation exceeded the token budget. Use /compact or increase the token budget.";
                default               -> "An unexpected error occurred. Check the verbose logs for details.";
            };
        }
    }

    public enum Severity   { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Priority   { LOW, MEDIUM, HIGH }
    public enum DecisionType { TOOL_CALL, STOP, QUESTION, ANALYSIS, RESPONSE }
    public enum EventCategory { INFO, SUCCESS, WARNING, ERROR }

    public record ContributingFactor(String name, String description, Severity severity) {}
    public record RemediationStep(int order, String action, String command, Priority priority) {}
    public record TimelineEvent(int sequence, String event, String detail, EventCategory category) {}

    public record FailureReport(
            String                    task,
            FailureType               type,
            String                    explanation,
            List<ContributingFactor>  factors,
            List<RemediationStep>     remediation,
            List<EpisodicMemory.Episode> similarFailures,
            List<TimelineEvent>       timeline,
            Instant                   generatedAt
    ) {
        public String markdown() {
            StringBuilder sb = new StringBuilder("## Agent Failure Report\n\n");
            sb.append("**Task**: ").append(task).append("\n");
            sb.append("**Type**: ").append(type).append("\n\n");
            sb.append("### Explanation\n").append(explanation).append("\n\n");
            sb.append("### Contributing Factors\n");
            factors.forEach(f -> sb.append("- **").append(f.name()).append("** [").append(f.severity()).append("]: ").append(f.description()).append("\n"));
            sb.append("\n### Remediation Steps\n");
            remediation.forEach(r -> sb.append(r.order()).append(". **").append(r.action()).append("**: `").append(r.command()).append("`\n"));
            return sb.toString();
        }
    }

    public record DagExplanation(
            int                     totalNodes,
            int                     successCount,
            int                     failureCount,
            List<NodeExplanation>   nodes,
            Map<String, List<String>> cascadeMap,
            String                  summary,
            Duration                elapsed
    ) {}

    public record NodeExplanation(
            String   nodeId,
            boolean  success,
            String   output,
            String   error,
            FailureType failureType,
            String   advice,
            Duration elapsed
    ) {}

    public record DecisionStep(
            int          stepNumber,
            DecisionType type,
            String       rationale,
            String       toolUsed,
            String       outputExcerpt
    ) {}

    public record DecisionTrace(
            String             task,
            List<DecisionStep> steps,
            String             narrative,
            int                totalSteps
    ) {}

    public record PlanDiffExplanation(
            HierarchicalTaskPlanner.Plan before,
            HierarchicalTaskPlanner.Plan after,
            Set<String>                  tasksAdded,
            Set<String>                  tasksRemoved,
            boolean                      modeChanged,
            int                          tokenDelta,
            String                       reason,
            String                       explanation
    ) {}
}
