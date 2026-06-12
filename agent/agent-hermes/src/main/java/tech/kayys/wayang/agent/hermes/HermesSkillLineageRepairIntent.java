package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backend-neutral, non-executing repair intent for learned-skill lineage.
 */
public record HermesSkillLineageRepairIntent(
        String intentId,
        String command,
        String sourceAction,
        String targetType,
        String target,
        List<String> candidateBackends,
        boolean permittedByPolicy,
        boolean mutationSupported,
        boolean approvalRequired,
        boolean automatic,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairIntent {
        intentId = HermesText.oneLineOr(intentId, "intent");
        command = HermesText.oneLineOr(command, "review-learned-skill-lineage");
        sourceAction = HermesText.oneLineOr(sourceAction, "review-lineage");
        targetType = HermesText.oneLineOr(targetType, "catalog");
        target = HermesText.oneLineOr(target, "");
        candidateBackends = HermesText.distinctOneLineList(candidateBackends);
        reason = HermesText.oneLineOr(reason, "learned-skill lineage repair intent prepared");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairIntent from(
            int index,
            HermesSkillLineageRemediationAction action,
            HermesSkillLineageRemediationPolicy policy) {
        HermesSkillLineageRemediationAction resolved = action == null
                ? new HermesSkillLineageRemediationAction(
                        "review-lineage",
                        "info",
                        false,
                        false,
                        "catalog",
                        "learned-skills",
                        "learned-skill lineage review recommended",
                        Map.of())
                : action;
        HermesSkillLineageRemediationPolicy effectivePolicy = policy == null
                ? HermesSkillLineageRemediationPolicy.dryRun()
                : policy;
        Map<String, Object> values = new LinkedHashMap<>(resolved.metadata());
        values.put("policyMode", effectivePolicy.mode());
        values.put("sourceSeverity", resolved.severity());
        values.put("sourceRequired", resolved.required());
        return new HermesSkillLineageRepairIntent(
                intentId(index, resolved.action()),
                command(resolved.action()),
                resolved.action(),
                resolved.targetType(),
                resolved.target(),
                candidateBackends(resolved.targetType()),
                effectivePolicy.permits(resolved),
                false,
                effectivePolicy.approvalRequired(),
                effectivePolicy.automaticMutationAllowed() && resolved.automatic(),
                resolved.reason(),
                values);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("intentId", intentId);
        values.put("command", command);
        values.put("sourceAction", sourceAction);
        values.put("targetType", targetType);
        values.put("target", target);
        values.put("candidateBackends", candidateBackends);
        values.put("permittedByPolicy", permittedByPolicy);
        values.put("mutationSupported", mutationSupported);
        values.put("approvalRequired", approvalRequired);
        values.put("automatic", automatic);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static String intentId(int index, String action) {
        return "intent-%03d-%s".formatted(Math.max(index, 1), normalize(action));
    }

    private static String command(String action) {
        return switch (normalize(action)) {
            case "repair-orphaned-lineage-root" -> "restore-lineage-root-definition";
            case "inspect-learned-skill-storage-consistency" -> "reconcile-learned-skill-store-indexes";
            case "review-refined-skill-quality" -> "review-refined-skill-quality";
            case "distill-first-successful-complex-workflow" -> "bootstrap-learned-skill-library";
            default -> "review-learned-skill-lineage";
        };
    }

    private static List<String> candidateBackends(String targetType) {
        String normalized = normalize(targetType);
        if ("catalog".equals(normalized)) {
            return List.of("database", "file-system", "object-storage");
        }
        if ("lineage-root".equals(normalized)) {
            return List.of("database", "file-system", "object-storage");
        }
        return List.of("database", "file-system", "object-storage");
    }

    private static String normalize(String value) {
        return HermesText.oneLineOr(value, "").toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
