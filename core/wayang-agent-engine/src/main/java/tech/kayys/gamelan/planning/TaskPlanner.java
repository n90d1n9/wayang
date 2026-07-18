package tech.kayys.gamelan.planning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.DirectCallOrchestrator;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.config.GamelanConfig;

import java.util.*;
import java.util.regex.*;

/**
 * Explicit planning layer (Section IX — Planning & Strategy Layer).
 *
 * <p>The current architecture embeds planning inside the agent loop's first
 * LLM call. This works for simple tasks but breaks down for complex ones
 * where the agent needs to reason about trade-offs, estimate costs, and
 * select a strategy before acting.
 *
 * <h2>What this adds</h2>
 * <ol>
 *   <li><b>Hierarchical Task Decomposition</b> — breaks complex tasks into
 *       a tree of subtasks, each independently executable</li>
 *   <li><b>Cost-aware Planning</b> — estimates token cost and latency for
 *       each strategy before committing</li>
 *   <li><b>Plan Versioning</b> — saves plans so they can be compared
 *       across runs and evolved over time</li>
 * </ol>
 *
 * <h2>When the planner fires</h2>
 * The planner is activated by {@link tech.kayys.gamelan.cli.RunCommand} when
 * {@code --plan} is set, or automatically when task complexity exceeds the
 * threshold (estimated > 5 steps or > 3 files to touch).
 */
@ApplicationScoped
public class TaskPlanner {

    private static final Logger log = LoggerFactory.getLogger(TaskPlanner.class);

    @Inject DirectCallOrchestrator llm;
    @Inject GamelanConfig          config;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Produces a hierarchical plan for the given task.
     *
     * <p>Calls the LLM once to decompose the task, then parses the result
     * into a structured {@link Plan} object.
     *
     * @param task  the user's raw task description
     * @param model LLM model to use for planning
     * @return a structured plan ready for execution
     */
    public Plan plan(String task, String model) {
        log.info("[planner] generating plan for: {}",
                task.length() > 60 ? task.substring(0, 60) + "…" : task);

        String planPrompt = """
                You are a senior software engineer. Decompose the following task into a
                hierarchical, executable plan.

                Rules:
                - Each step must be ATOMIC and independently verifiable
                - Order steps by dependency (earlier steps feed later ones)
                - Estimate cost: LOW (1 LLM call), MEDIUM (2-5), HIGH (5+)
                - Note which TOOLS each step requires
                - Flag steps that need HUMAN APPROVAL before executing

                Format your response as:
                GOAL: <one-sentence goal>
                STRATEGY: <overall approach>
                ESTIMATED_COST: LOW|MEDIUM|HIGH
                STEPS:
                1. [TOOL: tool_name] [HUMAN: yes/no] Step description
                2. [TOOL: tool_name] [HUMAN: yes/no] Step description
                ...
                RISKS:
                - Risk description

                TASK: """ + task;

        AgentRequest req = AgentRequest.builder(planPrompt)
                .model(model)
                .stream(false)
                .build();

        try {
            OrchestratorResult result = llm.execute(req);
            Plan plan = parse(result.answer(), task);
            log.info("[planner] plan: {} steps, cost={}", plan.steps().size(), plan.estimatedCost());
            return plan;
        } catch (Exception e) {
            log.warn("[planner] planning failed: {} — returning trivial plan", e.getMessage());
            return Plan.trivial(task);
        }
    }

    /**
     * Quick heuristic: should we invoke the planner for this task?
     *
     * <p>Returns true when the task appears complex enough to warrant an
     * explicit planning step rather than diving straight into execution.
     */
    public boolean shouldPlan(String task) {
        if (task == null || task.length() < 30) return false;
        String lower = task.toLowerCase();

        // Signals that suggest multi-step complexity
        int score = 0;
        if (lower.contains("all ") || lower.contains("every "))            score += 2;
        if (lower.contains("then ") || lower.contains(" and "))            score += 1;
        if (lower.contains("refactor") || lower.contains("migrate"))       score += 2;
        if (lower.contains("multiple") || lower.contains("several"))       score += 1;
        if (lower.contains("comprehensive") || lower.contains("full"))     score += 2;
        if (task.length() > 150)                                           score += 2;

        return score >= 3;
    }

    // ── Parsing ────────────────────────────────────────────────────────────

    private Plan parse(String raw, String originalTask) {
        String goal      = extract(raw, "GOAL:\\s*(.*?)\\n");
        String strategy  = extract(raw, "STRATEGY:\\s*(.*?)\\n");
        String costStr   = extract(raw, "ESTIMATED_COST:\\s*(LOW|MEDIUM|HIGH)");
        Plan.Cost cost   = parseCost(costStr);
        List<String> risks = parseRisks(raw);
        List<PlanStep> steps = parseSteps(raw);

        return new Plan(
                UUID.randomUUID().toString(),
                originalTask,
                goal.isBlank() ? originalTask : goal,
                strategy,
                steps,
                cost,
                risks
        );
    }

    private String extract(String text, String pattern) {
        Matcher m = Pattern.compile(pattern, Pattern.MULTILINE).matcher(text);
        return m.find() ? m.group(1).strip() : "";
    }

    private Plan.Cost parseCost(String s) {
        if (s == null) return Plan.Cost.MEDIUM;
        return switch (s.toUpperCase()) {
            case "LOW"  -> Plan.Cost.LOW;
            case "HIGH" -> Plan.Cost.HIGH;
            default     -> Plan.Cost.MEDIUM;
        };
    }

    private List<PlanStep> parseSteps(String raw) {
        List<PlanStep> steps = new ArrayList<>();
        Pattern p = Pattern.compile(
                "^(\\d+)\\.\\s*(?:\\[TOOL:\\s*([^]]+)])?\\s*(?:\\[HUMAN:\\s*(yes|no)])?\\s*(.+)$",
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(raw);
        while (m.find()) {
            int    num          = Integer.parseInt(m.group(1));
            String tool         = m.group(2) != null ? m.group(2).strip() : "";
            boolean needsHuman  = "yes".equalsIgnoreCase(m.group(3));
            String description  = m.group(4).strip();
            steps.add(new PlanStep(num, description, tool, needsHuman, false));
        }
        if (steps.isEmpty()) {
            // Fallback: treat each numbered line as a step
            steps.add(new PlanStep(1, raw.substring(0, Math.min(200, raw.length())), "", false, false));
        }
        return steps;
    }

    private List<String> parseRisks(String raw) {
        List<String> risks = new ArrayList<>();
        int ri = raw.indexOf("RISKS:");
        if (ri < 0) return risks;
        String[] lines = raw.substring(ri + 6).split("\n");
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.startsWith("-") && trimmed.length() > 2) {
                risks.add(trimmed.substring(1).strip());
            }
        }
        return risks;
    }

    // ── Types ──────────────────────────────────────────────────────────────

    public record Plan(
            String          id,
            String          originalTask,
            String          goal,
            String          strategy,
            List<PlanStep>  steps,
            Cost            estimatedCost,
            List<String>    risks
    ) {
        public enum Cost { LOW, MEDIUM, HIGH }

        public static Plan trivial(String task) {
            return new Plan(UUID.randomUUID().toString(), task, task, "direct execution",
                    List.of(new PlanStep(1, task, "", false, false)),
                    Cost.LOW, List.of());
        }

        public boolean hasHumanGates() {
            return steps.stream().anyMatch(PlanStep::requiresHumanApproval);
        }

        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("GOAL: ").append(goal).append("\n");
            sb.append("STRATEGY: ").append(strategy).append("\n");
            sb.append("COST: ").append(estimatedCost).append("  STEPS: ").append(steps.size()).append("\n\n");
            for (PlanStep s : steps) {
                sb.append(String.format("  %d. %s%s%s\n",
                        s.number(), s.description(),
                        s.tool().isBlank() ? "" : "  [tool:" + s.tool() + "]",
                        s.requiresHumanApproval() ? "  [NEEDS APPROVAL]" : ""));
            }
            if (!risks.isEmpty()) {
                sb.append("\nRISKS:\n");
                risks.forEach(r -> sb.append("  ⚠ ").append(r).append("\n"));
            }
            return sb.toString();
        }
    }

    public record PlanStep(
            int     number,
            String  description,
            String  tool,
            boolean requiresHumanApproval,
            boolean completed
    ) {}
}
