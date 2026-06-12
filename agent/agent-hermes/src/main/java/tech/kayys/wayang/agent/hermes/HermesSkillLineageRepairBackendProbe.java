package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single backend capability probe for a repair intent.
 */
public record HermesSkillLineageRepairBackendProbe(
        String backendId,
        boolean candidate,
        boolean commandSupported,
        boolean mutationSupported,
        String status,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairBackendProbe {
        backendId = HermesText.oneLineOr(backendId, "backend");
        commandSupported = candidate && commandSupported;
        mutationSupported = commandSupported && mutationSupported;
        status = HermesText.oneLineOr(status, status(candidate, commandSupported, mutationSupported));
        reason = HermesText.oneLineOr(reason, reason(status));
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairBackendProbe from(
            HermesSkillLineageRepairBackend backend,
            HermesSkillLineageRepairIntent intent) {
        return from(backend, intent, Map.of());
    }

    public static HermesSkillLineageRepairBackendProbe from(
            HermesSkillLineageRepairBackend backend,
            HermesSkillLineageRepairIntent intent,
            Map<String, Object> metadata) {
        String backendId = backend == null
                ? "backend"
                : HermesSkillLineageRepairBackend.normalizeBackendId(backend.backendId());
        boolean candidate = intent != null && candidate(intent.candidateBackends(), backendId);
        boolean commandSupported = candidate && backend != null && backend.supports(intent);
        boolean mutationSupported = commandSupported && backend.mutationSupported();
        return new HermesSkillLineageRepairBackendProbe(
                backendId,
                candidate,
                commandSupported,
                mutationSupported,
                "",
                "",
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendId", backendId);
        values.put("candidate", candidate);
        values.put("commandSupported", commandSupported);
        values.put("mutationSupported", mutationSupported);
        values.put("status", status);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static String status(boolean candidate, boolean commandSupported, boolean mutationSupported) {
        if (!candidate) {
            return "not-candidate";
        }
        if (mutationSupported) {
            return "mutation-ready";
        }
        return commandSupported ? "preview-only" : "unsupported";
    }

    private static String reason(String status) {
        return switch (status) {
            case "mutation-ready" -> "backend can handle this repair command with mutation support";
            case "preview-only" -> "backend can inspect this repair command but mutation is not enabled";
            case "unsupported" -> "backend does not support this repair command";
            default -> "backend is not listed as a candidate for this intent";
        };
    }

    private static boolean candidate(List<String> candidateBackends, String backendId) {
        if (candidateBackends == null || backendId == null || backendId.isBlank()) {
            return false;
        }
        List<String> normalized = candidateBackends.stream()
                .map(HermesSkillLineageRepairBackend::normalizeBackendId)
                .toList();
        if (normalized.contains(backendId)) {
            return true;
        }
        return normalized.contains("object-storage")
                && List.of("s3", "minio", "rustfs", "r2", "gcs", "azure-blob").contains(backendId);
    }
}
