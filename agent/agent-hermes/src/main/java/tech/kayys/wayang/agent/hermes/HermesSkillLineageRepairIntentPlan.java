package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-executing command plan derived from policy-permitted remediation actions.
 */
public record HermesSkillLineageRepairIntentPlan(
        boolean dryRun,
        boolean mutationSupported,
        boolean approvalRequired,
        int proposedActionCount,
        int permittedActionCount,
        int intentCount,
        List<HermesSkillLineageRepairIntent> intents) {

    public HermesSkillLineageRepairIntentPlan {
        intents = HermesCollections.copyNonNull(intents);
        intentCount = intents.size();
        permittedActionCount = intentCount;
        proposedActionCount = Math.max(proposedActionCount, intentCount);
        mutationSupported = false;
        approvalRequired = approvalRequired || intents.stream()
                .anyMatch(HermesSkillLineageRepairIntent::approvalRequired);
    }

    public static HermesSkillLineageRepairIntentPlan from(
            HermesSkillLineageRemediationPlan remediationPlan,
            HermesSkillLineageRemediationPolicy policy) {
        HermesSkillLineageRemediationPlan resolvedPlan = remediationPlan == null
                ? HermesSkillLineageRemediationPlan.none()
                : remediationPlan;
        HermesSkillLineageRemediationPolicy effectivePolicy = policy == null
                ? HermesSkillLineageRemediationPolicy.dryRun()
                : policy;
        if (!effectivePolicy.mutationAllowed() || effectivePolicy.maxActionsPerRun() == 0) {
            return new HermesSkillLineageRepairIntentPlan(
                    true,
                    false,
                    effectivePolicy.approvalRequired(),
                    resolvedPlan.actionCount(),
                    0,
                    0,
                    List.of());
        }
        List<HermesSkillLineageRepairIntent> intents = new java.util.ArrayList<>();
        for (HermesSkillLineageRemediationAction action : resolvedPlan.actions()) {
            if (intents.size() >= effectivePolicy.maxActionsPerRun()) {
                break;
            }
            if (effectivePolicy.permits(action)) {
                intents.add(HermesSkillLineageRepairIntent.from(intents.size() + 1, action, effectivePolicy));
            }
        }
        return new HermesSkillLineageRepairIntentPlan(
                true,
                false,
                effectivePolicy.approvalRequired(),
                resolvedPlan.actionCount(),
                0,
                0,
                intents);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dryRun", dryRun);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("proposedActionCount", proposedActionCount);
        values.put("permittedActionCount", permittedActionCount);
        values.put("intentCount", intentCount);
        values.put("intents", intents.stream()
                .map(HermesSkillLineageRepairIntent::toMetadata)
                .toList());
        return Map.copyOf(values);
    }
}
