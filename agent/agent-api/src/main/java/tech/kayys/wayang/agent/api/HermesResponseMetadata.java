package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesMetadataKeys;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared extraction helpers for typed Hermes API responses backed by metadata maps.
 */
final class HermesResponseMetadata {

    private HermesResponseMetadata() {
    }

    static boolean bool(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    static String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    static List<String> strings(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .toList();
    }

    static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> values)) {
            return Map.of();
        }
        Map<String, Object> mapped = new LinkedHashMap<>();
        values.forEach((key, entryValue) -> {
            if (key != null) {
                mapped.put(String.valueOf(key), entryValue);
            }
        });
        return Map.copyOf(mapped);
    }

    static List<Map<String, Object>> objectMaps(Object value) {
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(HermesResponseMetadata::objectMap)
                .filter(map -> !map.isEmpty())
                .toList();
    }

    static Map<String, Long> longMap(Object value) {
        if (!(value instanceof Map<?, ?> values)) {
            return Map.of();
        }
        Map<String, Long> mapped = new LinkedHashMap<>();
        values.forEach((key, entryValue) -> {
            if (key != null) {
                mapped.put(String.valueOf(key), longValue(entryValue));
            }
        });
        return Map.copyOf(mapped);
    }

    static Map<String, Object> learningAuditRetentionObservation(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> direct = objectMap(
                metadata.get(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION));
        if (!direct.isEmpty()) {
            return direct;
        }
        return objectMap(objectMap(metadata.get("diagnostics"))
                .get(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION));
    }

    static Map<String, Object> learningAuditRetentionStatus(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> auditStatus = objectMap(metadata.get("learningAuditRetentionStatus"));
        if (!auditStatus.isEmpty()) {
            return auditStatus;
        }
        Map<String, Object> retentionStatus = objectMap(metadata.get("retentionStatus"));
        if (!retentionStatus.isEmpty()) {
            return retentionStatus;
        }
        return flattenedLearningAuditRetentionStatus(metadata);
    }

    private static Map<String, Object> flattenedLearningAuditRetentionStatus(Map<String, Object> metadata) {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "ledgerType", firstPresent(metadata, "ledgerType", "retentionLedgerType"));
        put(values, "bounded", firstPresent(metadata, "bounded", "retentionBounded"));
        put(values, "recordCount", firstPresent(metadata, "recordCount", "retentionRecordCount"));
        put(values, "maxEntries", firstPresent(metadata, "maxEntries", "retentionMaxEntries"));
        put(values, "remainingEntries", firstPresent(metadata, "remainingEntries", "retentionRemainingEntries"));
        put(values, "overflowEntries", firstPresent(metadata, "overflowEntries", "retentionOverflowEntries"));
        put(values, "utilizationPercent", firstPresent(
                metadata,
                "utilizationPercent",
                "retentionUtilizationPercent"));
        put(values, "nearCapacity", firstPresent(metadata, "nearCapacity", "retentionNearCapacity"));
        put(values, "atCapacity", firstPresent(metadata, "atCapacity", "retentionAtCapacity"));
        put(values, "status", firstPresent(metadata, "retentionState"));
        put(values, "severity", firstPresent(metadata, "retentionSeverity"));
        put(values, "priority", firstPresent(metadata, "retentionPriority"));
        put(values, "requiresAttention", firstPresent(metadata, "retentionRequiresAttention"));
        put(values, "attention", firstPresent(metadata, "retentionAttention"));
        put(values, "recommendedActions", firstPresent(metadata, "retentionRecommendedActions"));
        put(values, "retentionPolicy", firstPresent(metadata, "retentionPolicy"));
        return Map.copyOf(values);
    }

    private static Object firstPresent(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (values.containsKey(key) && values.get(key) != null) {
                return values.get(key);
            }
        }
        return null;
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }
}
