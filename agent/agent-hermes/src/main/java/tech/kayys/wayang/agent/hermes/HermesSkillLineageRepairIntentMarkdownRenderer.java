package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Renders repair-intent dry-runs into a stable reviewer-facing markdown preview.
 */
public final class HermesSkillLineageRepairIntentMarkdownRenderer {

    public String render(HermesSkillLineageRemediationExecution execution) {
        HermesSkillLineageRemediationExecution resolved = execution == null
                ? HermesSkillLineageRemediationExecution.dryRun(
                        HermesSkillLineageRemediationPlan.none(),
                        HermesSkillStoreConsistencyReport.empty())
                : execution;
        HermesSkillLineageRepairIntentPlan intentPlan = resolved.repairIntentPlan();
        StringBuilder builder = new StringBuilder();
        builder.append("# Hermes Skill-Lineage Repair Preview\n\n");
        builder.append("Status: `").append(resolved.status()).append("`\n");
        builder.append("Policy mode: `").append(resolved.policy().mode()).append("`\n");
        builder.append("Strategy: `").append(resolved.strategy()).append("`\n");
        builder.append("Proposed actions: ").append(resolved.proposedActionCount()).append('\n');
        builder.append("Policy-permitted intents: ").append(intentPlan.intentCount()).append('\n');
        builder.append("Mutation supported: ").append(resolved.mutationSupported()).append('\n');
        builder.append("Approval required: ").append(resolved.approvalRequired()).append("\n\n");

        if (intentPlan.intents().isEmpty()) {
            builder.append(noIntentMessage(resolved)).append('\n');
            return builder.toString().trim();
        }

        builder.append("## Intents\n\n");
        List<HermesSkillLineageRepairIntent> intents = intentPlan.intents();
        for (int index = 0; index < intents.size(); index++) {
            appendIntent(builder, index + 1, intents.get(index));
        }
        builder.append("## Review Notes\n");
        builder.append("- These intents are preview-only; executed actions remain ")
                .append(resolved.executedActionCount())
                .append(".\n");
        if (resolved.approvalRequired()) {
            builder.append("- Approval is required before any future mutating adapter may apply this plan.\n");
        }
        return builder.toString().trim();
    }

    private static void appendIntent(
            StringBuilder builder,
            int number,
            HermesSkillLineageRepairIntent intent) {
        builder.append(number)
                .append(". `")
                .append(intent.command())
                .append("` -> `")
                .append(intent.target())
                .append("`\n");
        builder.append("   - Intent: `").append(intent.intentId()).append("`\n");
        builder.append("   - Source action: `").append(intent.sourceAction()).append("`\n");
        builder.append("   - Target type: `").append(intent.targetType()).append("`\n");
        builder.append("   - Candidate backends: ")
                .append(intent.candidateBackends().isEmpty()
                        ? "none"
                        : String.join(", ", intent.candidateBackends()))
                .append('\n');
        builder.append("   - Approval: ")
                .append(intent.approvalRequired() ? "required" : "not required")
                .append('\n');
        builder.append("   - Mutation: ")
                .append(intent.mutationSupported() ? "supported" : "preview-only")
                .append('\n');
        builder.append("   - Reason: ")
                .append(HermesText.oneLine(intent.reason()))
                .append("\n\n");
    }

    private static String noIntentMessage(HermesSkillLineageRemediationExecution execution) {
        if (execution.proposedActionCount() == 0) {
            return "No repair intents are pending.";
        }
        if (!execution.policy().mutationAllowed()) {
            return "Repair actions are visible in the remediation plan, but the current policy is dry-run only.";
        }
        return "No remediation actions matched the current policy filters.";
    }
}
