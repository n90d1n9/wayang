package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentState;

import java.util.List;

/**
 * Renders runtime prompts for Hermes-learned procedural skills.
 */
public final class HermesSkillPromptRenderer {

    public String initialPrompt(HermesLearningSignal signal, HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        StringBuilder builder = new StringBuilder();
        builder.append("You are executing a learned Hermes procedural skill.\n\n");
        builder.append("## When to Use\n");
        builder.append("Use this skill for tasks similar to: ").append(HermesText.oneLine(signal.task())).append("\n\n");
        builder.append("## Procedure\n");
        List<AgentState.ReasoningStep> steps = signal.steps().stream()
                .limit(effectiveConfig.maxSkillProcedureSteps())
                .toList();
        if (steps.isEmpty()) {
            builder.append("1. Analyze the request and identify reusable context.\n");
            builder.append("2. Execute the needed tools or skills.\n");
            builder.append("3. Verify the answer before returning it.\n");
        } else {
            for (int i = 0; i < steps.size(); i++) {
                appendStep(builder, i + 1, steps.get(i));
            }
        }
        builder.append("\n## Verification\n");
        builder.append("- Compare the final answer against the original request.\n");
        builder.append("- Re-run or inspect any tool output that carried the main evidence.\n");
        if (!signal.answer().isBlank()) {
            builder.append("- Previous successful outcome: ").append(HermesText.oneLine(signal.answer())).append('\n');
        }
        return builder.toString().trim();
    }

    public String refinementPrompt(HermesLearningSignal signal) {
        return "\n\n## Latest Refinement\n"
                + "- Request: " + HermesText.oneLine(signal.task()) + "\n"
                + "- Verification: " + HermesText.oneLine(signal.answer()) + "\n";
    }

    private static void appendStep(StringBuilder builder, int number, AgentState.ReasoningStep step) {
        builder.append(number).append(". ");
        if (step.action() != null && step.action().skillId() != null) {
            builder.append("Use `").append(step.action().skillId()).append("`");
            if (step.action().rationale() != null && !step.action().rationale().isBlank()) {
                builder.append(" because ").append(HermesText.oneLine(step.action().rationale()));
            }
            builder.append('.');
        } else if (step.thought() != null && !step.thought().isBlank()) {
            builder.append(HermesText.oneLine(step.thought()));
        } else {
            builder.append("Continue the workflow with the next verified action.");
        }
        builder.append('\n');
    }
}
