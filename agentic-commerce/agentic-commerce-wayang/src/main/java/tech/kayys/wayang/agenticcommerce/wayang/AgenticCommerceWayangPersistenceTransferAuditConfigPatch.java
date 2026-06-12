package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Machine-readable config patch suggested by transfer audit remediation.
 */
public record AgenticCommerceWayangPersistenceTransferAuditConfigPatch(
        String operation,
        String path,
        Object value,
        Map<String, Object> attributes) {

    public static final String OPERATION_REPLACE = "replace";

    public AgenticCommerceWayangPersistenceTransferAuditConfigPatch {
        operation = AgenticCommerceWayangMaps.required(operation, "config patch operation");
        path = AgenticCommerceWayangMaps.required(path, "config patch path");
        value = AgenticCommerceWayangMaps.copyValue(value);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatch replace(
            String path,
            Object value) {
        return replace(path, value, Map.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigPatch replace(
            String path,
            Object value,
            Map<String, Object> attributes) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfigPatch(
                OPERATION_REPLACE,
                path,
                value,
                attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", operation);
        values.put("path", path);
        values.put("value", value);
        values.put("attributes", attributes);
        return AgenticCommerceWayangMaps.copy(values);
    }

    public Map<String, Object> applyTo(Map<?, ?> config) {
        if (!OPERATION_REPLACE.equals(operation)) {
            throw new IllegalArgumentException("Unsupported transfer audit config patch operation: " + operation);
        }
        Map<String, Object> patched = mutableCopy(config);
        replacePath(patched, pathSegments(path), value);
        synchronizeRetentionAliases(patched, path, value);
        return AgenticCommerceWayangMaps.copy(patched);
    }

    public static Map<String, Object> applyAll(
            Map<?, ?> config,
            List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches) {
        Map<String, Object> patched = mutableCopy(config);
        if (patches == null || patches.isEmpty()) {
            return AgenticCommerceWayangMaps.copy(patched);
        }
        for (AgenticCommerceWayangPersistenceTransferAuditConfigPatch patch : patches) {
            if (patch != null) {
                patched = mutableCopy(patch.applyTo(patched));
            }
        }
        return AgenticCommerceWayangMaps.copy(patched);
    }

    private static List<String> pathSegments(String path) {
        String normalized = AgenticCommerceWayangMaps.text(path);
        if (!normalized.startsWith("$.") || normalized.length() <= 2) {
            throw new IllegalArgumentException("Unsupported transfer audit config patch path: " + path);
        }
        List<String> segments = List.of(normalized.substring(2).split("\\.")).stream()
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Unsupported transfer audit config patch path: " + path);
        }
        return segments;
    }

    private static void replacePath(
            Map<String, Object> values,
            List<String> segments,
            Object replacement) {
        Map<String, Object> target = values;
        for (int index = 0; index < segments.size() - 1; index++) {
            String segment = segments.get(index);
            Object child = target.get(segment);
            Map<String, Object> childMap = child instanceof Map<?, ?> map
                    ? mutableCopy(map)
                    : new LinkedHashMap<>();
            target.put(segment, childMap);
            target = childMap;
        }
        target.put(segments.get(segments.size() - 1), AgenticCommerceWayangMaps.copyValue(replacement));
    }

    private static void synchronizeRetentionAliases(
            Map<String, Object> values,
            String path,
            Object replacement) {
        if ("$.maxTrails".equals(path)) {
            replaceNestedRetentionValue(values, "maxTrails", replacement);
        } else if ("$.retentionPolicy.maxTrails".equals(path) && values.containsKey("maxTrails")) {
            values.put("maxTrails", AgenticCommerceWayangMaps.copyValue(replacement));
        }
    }

    private static void replaceNestedRetentionValue(
            Map<String, Object> values,
            String key,
            Object replacement) {
        Object retention = values.get("retentionPolicy");
        if (retention instanceof Map<?, ?> retentionMap) {
            Map<String, Object> patchedRetention = mutableCopy(retentionMap);
            patchedRetention.put(key, AgenticCommerceWayangMaps.copyValue(replacement));
            values.put("retentionPolicy", patchedRetention);
        }
    }

    private static Map<String, Object> mutableCopy(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(String.valueOf(key), mutableValue(value));
            }
        });
        return copy;
    }

    private static Object mutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return mutableCopy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceWayangMaps::copyValue)
                    .toList();
        }
        return value;
    }
}
