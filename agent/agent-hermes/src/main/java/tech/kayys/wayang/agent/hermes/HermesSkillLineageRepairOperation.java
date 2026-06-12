package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dry-run operation preview selected from a backend readiness assessment.
 */
public record HermesSkillLineageRepairOperation(
        String operationId,
        String intentId,
        String backendId,
        String storageFamily,
        String command,
        String targetType,
        String target,
        boolean dryRun,
        boolean commandSupported,
        boolean mutationSupported,
        boolean mutationReady,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairOperation {
        intentId = HermesText.oneLineOr(intentId, "intent");
        backendId = HermesText.oneLineOr(backendId, "");
        storageFamily = HermesText.oneLineOr(storageFamily, backendId.isBlank() ? "none" : backendId);
        command = HermesText.oneLineOr(command, "review-learned-skill-lineage");
        targetType = HermesText.oneLineOr(targetType, "catalog");
        target = HermesText.oneLineOr(target, "");
        commandSupported = commandSupported || mutationSupported;
        mutationReady = commandSupported && mutationSupported;
        status = HermesText.oneLineOr(status, status(commandSupported, mutationReady));
        operationId = HermesText.oneLineOr(operationId, operationId(intentId, backendId, command));
        reason = HermesText.oneLineOr(reason, reason(status));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairOperation from(
            HermesSkillLineageRepairBackendAssessment assessment) {
        HermesSkillLineageRepairBackendAssessment resolved = assessment == null
                ? new HermesSkillLineageRepairBackendAssessment(
                        "intent",
                        "review-learned-skill-lineage",
                        "catalog",
                        "",
                        "",
                        false,
                        false,
                        "unsupported",
                        java.util.List.of())
                : assessment;
        HermesSkillLineageRepairBackendProbe selected = selectedProbe(resolved);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("assessmentStatus", resolved.status());
        values.put("selectedBackend", resolved.selectedBackend());
        if (selected != null) {
            values.put("probe", selected.toMetadata());
            values.put("probeMetadata", selected.metadata());
        }
        return new HermesSkillLineageRepairOperation(
                "",
                resolved.intentId(),
                selected == null ? resolved.selectedBackend() : selected.backendId(),
                storageFamily(selected),
                resolved.command(),
                resolved.targetType(),
                resolved.target(),
                true,
                selected != null && selected.commandSupported(),
                selected != null && selected.mutationSupported(),
                false,
                "",
                "",
                values);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operationId", operationId);
        values.put("intentId", intentId);
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("command", command);
        values.put("targetType", targetType);
        values.put("target", target);
        values.put("dryRun", dryRun);
        values.put("commandSupported", commandSupported);
        values.put("mutationSupported", mutationSupported);
        values.put("mutationReady", mutationReady);
        values.put("status", status);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static HermesSkillLineageRepairBackendProbe selectedProbe(
            HermesSkillLineageRepairBackendAssessment assessment) {
        if (assessment.selectedBackend().isBlank()) {
            return null;
        }
        return assessment.probes().stream()
                .filter(probe -> assessment.selectedBackend().equals(probe.backendId()))
                .findFirst()
                .orElse(null);
    }

    private static String storageFamily(HermesSkillLineageRepairBackendProbe probe) {
        if (probe == null) {
            return "none";
        }
        Object value = probe.metadata().get("storageFamily");
        return value == null
                ? HermesSkillLineageRepairBackendProfile.familyFor(probe.backendId())
                : HermesText.oneLine(String.valueOf(value));
    }

    private static String operationId(String intentId, String backendId, String command) {
        String backend = backendId == null || backendId.isBlank() ? "no-backend" : backendId;
        return "operation-%s-%s-%s".formatted(intentId, backend, command);
    }

    private static String status(boolean commandSupported, boolean mutationReady) {
        if (mutationReady) {
            return "mutation-ready";
        }
        return commandSupported ? "preview-only" : "unsupported";
    }

    private static String reason(String status) {
        return switch (status) {
            case "mutation-ready" -> "operation can be handed to a future mutating repair adapter";
            case "preview-only" -> "operation is selectable for dry-run review but mutation is not enabled";
            default -> "no configured backend can prepare this repair operation";
        };
    }
}
