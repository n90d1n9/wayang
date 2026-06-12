package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistable approval grant for one lineage repair mutation request.
 */
public record HermesSkillLineageRepairApproval(
        String approvalId,
        String action,
        String idempotencyKey,
        String backendId,
        String storageFamily,
        boolean active,
        String approvedBy,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairApproval {
        approvalId = HermesText.oneLineOr(approvalId, "");
        action = HermesSkillLineageRepairAdapterCapabilities.normalize(action, "");
        idempotencyKey = HermesText.oneLineOr(idempotencyKey, "");
        backendId = HermesSkillLineageRepairAdapterCapabilities.normalize(backendId, "");
        storageFamily = HermesSkillLineageRepairAdapterCapabilities.normalize(storageFamily, "");
        approvedBy = HermesText.oneLineOr(approvedBy, "unknown");
        reason = HermesText.oneLineOr(reason, "lineage repair mutation approved");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairApproval approved(
            String approvalId,
            String action,
            String idempotencyKey) {
        return new HermesSkillLineageRepairApproval(
                approvalId,
                action,
                idempotencyKey,
                "",
                "",
                true,
                "operator",
                "lineage repair mutation approved",
                Map.of());
    }

    public static HermesSkillLineageRepairApproval fromMetadata(Map<String, ?> values) {
        Map<String, ?> source = values == null ? Map.of() : values;
        return new HermesSkillLineageRepairApproval(
                text(source.get("approvalId")),
                text(source.get("action")),
                text(source.get("idempotencyKey")),
                text(source.get("backendId")),
                text(source.get("storageFamily")),
                booleanValue(source.get("active"), true),
                text(source.get("approvedBy")),
                text(source.get("reason")),
                objectMap(source.get("metadata")));
    }

    public boolean matches(HermesSkillLineageRepairAdapterDispatchRequest request) {
        if (!active || request == null) {
            return false;
        }
        if (!approvalId.equals(request.approvalId())) {
            return false;
        }
        if (!action.isBlank() && !action.equals(request.action())) {
            return false;
        }
        if (!idempotencyKey.isBlank() && !idempotencyKey.equals(request.idempotencyKey())) {
            return false;
        }
        if (!backendId.isBlank() && !matchesBackend(request)) {
            return false;
        }
        return storageFamily.isBlank() || matchesStorageFamily(request);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("approvalId", approvalId);
        values.put("action", action);
        values.put("idempotencyKey", idempotencyKey);
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("active", active);
        values.put("approvedBy", approvedBy);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private boolean matchesBackend(HermesSkillLineageRepairAdapterDispatchRequest request) {
        return backendId.equals(request.selection().query().backendId())
                || request.selection().batches().stream()
                        .anyMatch(batch -> backendId.equals(batch.backendId()));
    }

    private boolean matchesStorageFamily(HermesSkillLineageRepairAdapterDispatchRequest request) {
        return storageFamily.equals(request.selection().query().storageFamily())
                || request.selection().batches().stream()
                        .anyMatch(batch -> storageFamily.equals(batch.storageFamily()));
    }

    private static String text(Object value) {
        return value == null ? "" : HermesText.oneLine(String.valueOf(value));
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return fallback;
        }
        return switch (HermesConfigValues.normalizeKey(String.valueOf(value))) {
            case "true", "yes", "y", "1", "on", "enabled" -> true;
            case "false", "no", "n", "0", "off", "disabled" -> false;
            default -> fallback;
        };
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> {
            if (key != null && mapValue != null) {
                result.put(String.valueOf(key), mapValue);
            }
        });
        return Map.copyOf(result);
    }
}
