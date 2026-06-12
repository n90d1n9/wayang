package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Retention rules shared by durable transfer audit journals.
 */
public record AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
        int maxTrails,
        long maxBytes,
        Map<String, Object> attributes) {

    public static final long UNLIMITED_BYTES = 0L;
    public static final String ISSUE_INVALID_MAX_TRAILS = "invalid_retention_count";

    public AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy {
        maxTrails = maxTrails < 1
                ? InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS
                : maxTrails;
        maxBytes = Math.max(UNLIMITED_BYTES, maxBytes);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy defaults() {
        return ofMaxTrails(InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy ofMaxTrails(int maxTrails) {
        return new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                maxTrails,
                UNLIMITED_BYTES,
                Map.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy fromMap(
            Map<?, ?> values,
            int defaultMaxTrails) {
        Map<String, Object> resolved = mergedRetentionValues(values);
        IntParseReport maxTrailsParseReport = firstIntReport(
                resolved,
                defaultMaxTrails,
                "maxTrails",
                "maxEvents",
                "retention",
                "limit",
                "capacity");
        AgenticCommerceWayangByteSizes.ParseReport maxBytesParseReport =
                AgenticCommerceWayangByteSizes.parseReport(
                        AgenticCommerceWayangMaps.first(
                                resolved,
                                "maxBytes",
                                "byteLimit",
                                "maxJournalBytes",
                                "maxSizeBytes",
                                "journalMaxBytes"),
                        UNLIMITED_BYTES);
        Map<String, Object> attributes = new LinkedHashMap<>(attributesFrom(resolved));
        if (maxTrailsParseReport.invalid()) {
            attributes.put("maxTrailsParse", maxTrailsParseReport.toMap());
        }
        if (maxBytesParseReport.invalid()) {
            attributes.put("maxBytesParse", maxBytesParseReport.toMap());
        }
        return new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                maxTrailsParseReport.value(),
                maxBytesParseReport.bytes(),
                attributes);
    }

    public boolean byteLimited() {
        return maxBytes > UNLIMITED_BYTES;
    }

    public boolean maxTrailsParseInvalid() {
        return maxTrailsParse().containsKey("issue");
    }

    public Map<String, Object> maxTrailsParse() {
        Object value = attributes.get("maxTrailsParse");
        return value instanceof Map<?, ?> map ? AgenticCommerceWayangMaps.copy(map) : Map.of();
    }

    public boolean maxBytesParseInvalid() {
        return maxBytesParse().containsKey("issue");
    }

    public Map<String, Object> maxBytesParse() {
        Object value = attributes.get("maxBytesParse");
        return value instanceof Map<?, ?> map ? AgenticCommerceWayangMaps.copy(map) : Map.of();
    }

    public List<String> retainLines(List<String> lines) {
        List<String> retained = retainByCount(lines, maxTrails);
        if (!byteLimited() || retained.isEmpty()) {
            return retained;
        }
        List<String> byteRetained = new ArrayList<>();
        long retainedBytes = 0L;
        for (int index = retained.size() - 1; index >= 0; index--) {
            String line = retained.get(index);
            long lineBytes = bytesOnDisk(line);
            if (!byteRetained.isEmpty() && retainedBytes + lineBytes > maxBytes) {
                break;
            }
            byteRetained.add(0, line);
            retainedBytes += lineBytes;
            if (retainedBytes >= maxBytes) {
                break;
            }
        }
        return byteRetained.isEmpty()
                ? List.of(retained.get(retained.size() - 1))
                : List.copyOf(byteRetained);
    }

    public AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment assess(
            List<String> sampleLines,
            int expectedRetainedTrailCount) {
        return AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment.from(
                this,
                sampleLines,
                expectedRetainedTrailCount);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("maxTrails", maxTrails);
        values.put("maxBytes", maxBytes);
        values.put("maxBytesDisplay", AgenticCommerceWayangByteSizes.formatLimit(maxBytes));
        values.put("byteLimited", byteLimited());
        if (!attributes.isEmpty()) {
            values.put("attributes", attributes);
        }
        return Map.copyOf(values);
    }

    private static List<String> retainByCount(List<String> lines, int maxTrails) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> normalized = lines.stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        int capacity = maxTrails < 1 ? Integer.MAX_VALUE : maxTrails;
        if (normalized.size() <= capacity) {
            return normalized;
        }
        return List.copyOf(normalized.subList(normalized.size() - capacity, normalized.size()));
    }

    private static long bytesOnDisk(String line) {
        return line.getBytes(StandardCharsets.UTF_8).length
                + System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
    }

    private static Map<String, Object> mergedRetentionValues(Map<?, ?> values) {
        Map<String, Object> resolved = new LinkedHashMap<>(AgenticCommerceWayangMaps.copy(values));
        mergeNested(resolved, values, "retentionPolicy");
        mergeNested(resolved, values, "retention");
        mergeNested(resolved, values, "auditRetention");
        mergeNested(resolved, values, "auditRetentionPolicy");
        return Map.copyOf(resolved);
    }

    private static void mergeNested(Map<String, Object> target, Map<?, ?> values, String key) {
        Object nested = values == null ? null : values.get(key);
        if (nested instanceof Map<?, ?> map) {
            target.putAll(AgenticCommerceWayangMaps.copy(map));
        }
    }

    private static IntParseReport firstIntReport(Map<?, ?> values, int defaultValue, String... keys) {
        if (values == null || values.isEmpty() || keys == null) {
            return IntParseReport.unconfigured(defaultValue);
        }
        for (String key : keys) {
            if (key == null || !values.containsKey(key)) {
                continue;
            }
            return IntParseReport.from(values.get(key), defaultValue);
        }
        return IntParseReport.unconfigured(defaultValue);
    }

    private static Map<String, Object> attributesFrom(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> attributes = new LinkedHashMap<>(values);
        List.of(
                "maxTrails",
                "maxEvents",
                "retention",
                "limit",
                "capacity",
                "maxBytes",
                "byteLimit",
                "maxJournalBytes",
                "maxSizeBytes",
                "journalMaxBytes",
                "retentionPolicy",
                "auditRetention",
                "auditRetentionPolicy")
                .forEach(attributes::remove);
        return attributes;
    }

    private record IntParseReport(
            boolean configured,
            boolean valid,
            int value,
            String rawValue,
            String issue) {

        private IntParseReport {
            rawValue = AgenticCommerceWayangMaps.text(rawValue);
            issue = AgenticCommerceWayangMaps.text(issue);
        }

        private static IntParseReport unconfigured(int defaultValue) {
            return new IntParseReport(false, true, defaultValue, "", "");
        }

        private static IntParseReport from(Object rawValue, int defaultValue) {
            String text = AgenticCommerceWayangMaps.text(rawValue);
            if (text.isBlank()) {
                return unconfigured(defaultValue);
            }
            if (rawValue instanceof Number number) {
                return new IntParseReport(true, true, number.intValue(), text, "");
            }
            String normalized = text
                    .replace("_", "")
                    .replace(",", "")
                    .replace(" ", "");
            try {
                return new IntParseReport(true, true, Integer.parseInt(normalized), text, "");
            } catch (NumberFormatException exception) {
                return new IntParseReport(true, false, defaultValue, text, ISSUE_INVALID_MAX_TRAILS);
            }
        }

        private boolean invalid() {
            return configured && !valid;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("configured", configured);
            values.put("valid", valid);
            values.put("maxTrails", value);
            if (!rawValue.isBlank()) {
                values.put("rawValue", rawValue);
            }
            if (!issue.isBlank()) {
                values.put("issue", issue);
            }
            return Map.copyOf(values);
        }
    }
}
