package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result envelope for previewing learned-skill lineage remediation.
 */
public record HermesSkillLineageRemediationExecution(
        boolean dryRun,
        boolean mutationAllowed,
        boolean mutationSupported,
        boolean approvalRequired,
        String status,
        String strategy,
        boolean required,
        int proposedActionCount,
        int policyPermittedActionCount,
        int executedActionCount,
        int skippedActionCount,
        HermesSkillLineageRemediationPolicy policy,
        HermesSkillLineageRepairIntentPlan repairIntentPlan,
        HermesSkillLineageRepairBackendPlan repairBackendPlan,
        HermesSkillLineageRemediationPlan remediationPlan,
        HermesSkillStoreConsistencyReport consistencyReport) {

    public HermesSkillLineageRemediationExecution {
        policy = policy == null ? HermesSkillLineageRemediationPolicy.dryRun() : policy;
        remediationPlan = remediationPlan == null ? HermesSkillLineageRemediationPlan.none() : remediationPlan;
        consistencyReport = consistencyReport == null ? HermesSkillStoreConsistencyReport.empty() : consistencyReport;
        repairIntentPlan = repairIntentPlan == null
                ? HermesSkillLineageRepairIntentPlan.from(remediationPlan, policy)
                : repairIntentPlan;
        repairBackendPlan = repairBackendPlan == null
                ? HermesSkillLineageRepairBackendRegistry.defaultRegistry().assess(repairIntentPlan)
                : repairBackendPlan;
        strategy = HermesText.oneLineOr(strategy, remediationPlan.strategy());
        required = required || remediationPlan.required();
        proposedActionCount = remediationPlan.actionCount();
        policyPermittedActionCount = repairIntentPlan.permittedActionCount();
        mutationAllowed = mutationAllowed && policyPermittedActionCount > 0;
        mutationSupported = mutationSupported && repairBackendPlan.mutationSupported();
        approvalRequired = approvalRequired || policy.approvalRequired();
        executedActionCount = mutationAllowed && mutationSupported ? Math.max(executedActionCount, 0) : 0;
        skippedActionCount = Math.max(proposedActionCount - executedActionCount, 0);
        status = HermesText.oneLineOr(status, status(dryRun, required, proposedActionCount, mutationAllowed));
    }

    public static HermesSkillLineageRemediationExecution dryRun(
            HermesSkillLineageRemediationPlan remediationPlan,
            HermesSkillStoreConsistencyReport consistencyReport) {
        return dryRun(remediationPlan, consistencyReport, HermesSkillLineageRemediationPolicy.dryRun());
    }

    public static HermesSkillLineageRemediationExecution dryRun(
            HermesSkillLineageRemediationPlan remediationPlan,
            HermesSkillStoreConsistencyReport consistencyReport,
            HermesSkillLineageRemediationPolicy policy) {
        return dryRun(
                remediationPlan,
                consistencyReport,
                policy,
                HermesSkillLineageRepairBackendRegistry.defaultRegistry());
    }

    public static HermesSkillLineageRemediationExecution dryRun(
            HermesSkillLineageRemediationPlan remediationPlan,
            HermesSkillStoreConsistencyReport consistencyReport,
            HermesSkillLineageRemediationPolicy policy,
            HermesSkillLineageRepairBackendRegistry backendRegistry) {
        HermesSkillLineageRemediationPolicy effectivePolicy =
                policy == null ? HermesSkillLineageRemediationPolicy.dryRun() : policy;
        HermesSkillLineageRemediationPlan effectivePlan =
                remediationPlan == null ? HermesSkillLineageRemediationPlan.none() : remediationPlan;
        HermesSkillLineageRepairIntentPlan repairIntentPlan =
                HermesSkillLineageRepairIntentPlan.from(effectivePlan, effectivePolicy);
        HermesSkillLineageRepairBackendRegistry effectiveRegistry = backendRegistry == null
                ? HermesSkillLineageRepairBackendRegistry.defaultRegistry()
                : backendRegistry;
        HermesSkillLineageRepairBackendPlan repairBackendPlan = effectiveRegistry.assess(repairIntentPlan);
        return new HermesSkillLineageRemediationExecution(
                true,
                effectivePolicy.mutationAllowed() && repairIntentPlan.permittedActionCount() > 0,
                repairBackendPlan.mutationSupported(),
                effectivePolicy.approvalRequired(),
                "",
                effectivePlan.strategy(),
                effectivePlan.required(),
                0,
                0,
                0,
                0,
                effectivePolicy,
                repairIntentPlan,
                repairBackendPlan,
                remediationPlan,
                consistencyReport);
    }

    public HermesSkillLineageRepairOperationHandoff repairOperationHandoff() {
        return HermesSkillLineageRepairOperationHandoff.from(this);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dryRun", dryRun);
        values.put("mutationAllowed", mutationAllowed);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("status", status);
        values.put("strategy", strategy);
        values.put("required", required);
        values.put("proposedActionCount", proposedActionCount);
        values.put("policyPermittedActionCount", policyPermittedActionCount);
        values.put("executedActionCount", executedActionCount);
        values.put("skippedActionCount", skippedActionCount);
        values.put("policy", policy.toMetadata());
        values.put("repairIntentPlan", repairIntentPlan.toMetadata());
        values.put("repairBackendPlan", repairBackendPlan.toMetadata());
        values.put("repairOperationHandoff", repairOperationHandoff().toMetadata());
        values.put("remediationPlan", remediationPlan.toMetadata());
        values.put("consistencyReport", consistencyReport.toMetadata());
        return Map.copyOf(values);
    }

    private static String status(boolean dryRun, boolean required, int actionCount, boolean mutationAllowed) {
        if (dryRun && required && mutationAllowed) {
            return "dry-run-repair-policy-ready";
        }
        if (dryRun && required) {
            return "dry-run-repair-required";
        }
        if (dryRun && mutationAllowed) {
            return "dry-run-policy-ready";
        }
        if (dryRun && actionCount > 0) {
            return "dry-run-advisory";
        }
        if (dryRun) {
            return "dry-run-noop";
        }
        return required ? "repair-required" : actionCount > 0 ? "advisory" : "noop";
    }
}
