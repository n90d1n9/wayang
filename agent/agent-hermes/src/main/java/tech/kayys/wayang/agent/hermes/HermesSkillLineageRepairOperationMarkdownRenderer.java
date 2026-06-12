package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Renders backend repair-operation dry-runs into a stable reviewer-facing markdown preview.
 */
public final class HermesSkillLineageRepairOperationMarkdownRenderer {

    public String render(HermesSkillLineageRemediationExecution execution) {
        HermesSkillLineageRemediationExecution resolved = execution == null
                ? HermesSkillLineageRemediationExecution.dryRun(
                        HermesSkillLineageRemediationPlan.none(),
                        HermesSkillStoreConsistencyReport.empty())
                : execution;
        HermesSkillLineageRepairBackendPlan backendPlan = resolved.repairBackendPlan();
        StringBuilder builder = new StringBuilder();
        builder.append("# Hermes Skill-Lineage Backend Operation Preview\n\n");
        builder.append("Status: `").append(resolved.status()).append("`\n");
        builder.append("Policy mode: `").append(resolved.policy().mode()).append("`\n");
        builder.append("Operation count: ").append(backendPlan.operationCount()).append('\n');
        builder.append("Mutation-ready operations: ").append(backendPlan.mutationReadyOperationCount()).append('\n');
        builder.append("Preview-only operations: ").append(backendPlan.previewOnlyOperationCount()).append('\n');
        builder.append("Unsupported operations: ").append(backendPlan.unsupportedOperationCount()).append('\n');
        builder.append("Mutation supported: ").append(backendPlan.mutationSupported()).append('\n');
        builder.append("Approval required: ").append(resolved.approvalRequired()).append("\n\n");

        List<HermesSkillLineageRepairOperation> operations = backendPlan.operations();
        if (operations.isEmpty()) {
            builder.append(noOperationMessage(resolved)).append('\n');
            return builder.toString().trim();
        }

        builder.append("## Operations\n\n");
        for (int index = 0; index < operations.size(); index++) {
            appendOperation(builder, index + 1, operations.get(index));
        }
        builder.append("## Review Notes\n");
        builder.append("- This preview is dry-run only; no backend store writes were performed.\n");
        if (resolved.approvalRequired()) {
            builder.append("- Approval is required before a future mutating repair adapter may apply these operations.\n");
        }
        return builder.toString().trim();
    }

    private static void appendOperation(
            StringBuilder builder,
            int number,
            HermesSkillLineageRepairOperation operation) {
        builder.append(number)
                .append(". `")
                .append(operation.command())
                .append("` via `")
                .append(operation.backendId())
                .append("` -> `")
                .append(operation.target())
                .append("`\n");
        builder.append("   - Operation: `").append(operation.operationId()).append("`\n");
        builder.append("   - Intent: `").append(operation.intentId()).append("`\n");
        builder.append("   - Storage family: `").append(operation.storageFamily()).append("`\n");
        builder.append("   - Target type: `").append(operation.targetType()).append("`\n");
        builder.append("   - Dry-run: ").append(operation.dryRun()).append('\n');
        builder.append("   - Backend status: `").append(operation.status()).append("`\n");
        builder.append("   - Mutation: ").append(mutationLabel(operation)).append('\n');
        builder.append("   - Reason: ")
                .append(HermesText.oneLine(operation.reason()))
                .append("\n\n");
    }

    private static String mutationLabel(HermesSkillLineageRepairOperation operation) {
        if (operation.mutationReady()) {
            return "ready";
        }
        if (operation.commandSupported()) {
            return "preview-only";
        }
        return "unsupported";
    }

    private static String noOperationMessage(HermesSkillLineageRemediationExecution execution) {
        if (execution.proposedActionCount() == 0) {
            return "No backend repair operations are pending.";
        }
        if (!execution.policy().mutationAllowed()) {
            return "Repair actions are visible, but the current policy is dry-run only.";
        }
        return "No repair intents selected a configured backend operation.";
    }
}
