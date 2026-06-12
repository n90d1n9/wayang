package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_COUNT_INVALID;

/**
 * Machine-readable remediation hint for transfer audit config validation issues.
 */
public record AgenticCommerceWayangPersistenceTransferAuditConfigRemediation(
        String code,
        String path,
        String message,
        String operation,
        Map<String, Object> attributes,
        Map<String, Object> hints) {

    public static final String OPERATION_REVIEW_VALIDATION_ISSUE = "review_validation_issue";
    public static final String OPERATION_REPLACE_WITH_INTEGER = "replace_with_integer";
    public static final String OPERATION_INCREASE_RETAINED_HISTORY = "increase_retained_history";
    public static final String OPERATION_REPLACE_WITH_BYTE_SIZE = "replace_with_byte_size";
    public static final String OPERATION_INCREASE_BYTE_LIMIT = "increase_byte_limit";

    public AgenticCommerceWayangPersistenceTransferAuditConfigRemediation {
        code = AgenticCommerceWayangMaps.required(code, "remediation code");
        path = AgenticCommerceWayangMaps.text(path);
        message = AgenticCommerceWayangMaps.text(message);
        operation = AgenticCommerceWayangMaps.required(operation, "remediation operation");
        attributes = AgenticCommerceWayangMaps.copy(attributes);
        hints = AgenticCommerceWayangMaps.copy(hints);
    }

    public static List<AgenticCommerceWayangPersistenceTransferAuditConfigRemediation> fromIssues(
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::error)
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigRemediation::fromIssue)
                .toList();
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigRemediation fromIssue(
            AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        String operation = operationFor(issue);
        Map<String, Object> hints = new LinkedHashMap<>();
        switch (operation) {
            case OPERATION_REPLACE_WITH_INTEGER -> retentionCountInvalidRemediation(hints, issue);
            case OPERATION_INCREASE_RETAINED_HISTORY -> retentionCountBelowContractRemediation(hints, issue);
            case OPERATION_REPLACE_WITH_BYTE_SIZE -> retentionByteLimitInvalidRemediation(hints, issue);
            case OPERATION_INCREASE_BYTE_LIMIT -> retentionByteLimitBelowContractRemediation(hints, issue);
            default -> {
            }
        }
        return new AgenticCommerceWayangPersistenceTransferAuditConfigRemediation(
                issue.code(),
                issue.path(),
                issue.message(),
                operation,
                issue.attributes(),
                hints);
    }

    public Map<String, Object> toMap() {
        List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches = patches();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("path", path);
        values.put("message", message);
        values.put("attributes", attributes);
        values.put("operation", operation);
        values.put("patchCount", patches.size());
        values.put("patches", patches.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditConfigPatch::toMap)
                .toList());
        hints.forEach(values::putIfAbsent);
        return Map.copyOf(values);
    }

    public List<AgenticCommerceWayangPersistenceTransferAuditConfigPatch> patches() {
        Object suggestedValue = hints.get("suggestedValue");
        if (suggestedValue == null) {
            return List.of();
        }
        return switch (operation) {
            case OPERATION_REPLACE_WITH_INTEGER,
                    OPERATION_INCREASE_RETAINED_HISTORY,
                    OPERATION_INCREASE_BYTE_LIMIT -> List.of(
                            AgenticCommerceWayangPersistenceTransferAuditConfigPatch.replace(
                                    path,
                                    suggestedValue,
                                    Map.of(
                                            "sourceIssueCode",
                                            code,
                                            "remediationOperation",
                                            operation)));
            default -> List.of();
        };
    }

    public int patchCount() {
        return patches().size();
    }

    private static String operationFor(AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        return switch (issue.code()) {
            case AUDIT_RETENTION_COUNT_INVALID -> OPERATION_REPLACE_WITH_INTEGER;
            case AUDIT_RETENTION_BELOW_CONTRACT -> OPERATION_INCREASE_RETAINED_HISTORY;
            case AUDIT_RETENTION_BYTE_LIMIT_INVALID -> OPERATION_REPLACE_WITH_BYTE_SIZE;
            case AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT -> OPERATION_INCREASE_BYTE_LIMIT;
            default -> OPERATION_REVIEW_VALIDATION_ISSUE;
        };
    }

    private static void retentionCountInvalidRemediation(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        values.put("expectedType", "integer");
        values.put("minimumValue", AgenticCommerceWayangPersistenceTransferAuditContractHarness
                .DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
        values.put("suggestedValue", Math.max(
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT,
                number(issue.attributes().get("maxTrails"),
                        AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS)));
        values.put("acceptedKeys", List.of("maxTrails", "maxEvents", "retention", "limit", "capacity"));
        copyAttribute(values, issue, "rawValue");
    }

    private static void retentionCountBelowContractRemediation(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        int minimum = number(
                issue.attributes().get("minimumRetainedTrailCount"),
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
        values.put("minimumValue", minimum);
        values.put("suggestedValue", minimum);
        copyAttribute(values, issue, "maxTrails", "currentValue");
    }

    private static void retentionByteLimitInvalidRemediation(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        values.put("expectedType", "byte-size");
        values.put("examples", List.of("64 KiB", "1MiB", "unlimited"));
        copyAttribute(values, issue, "rawValue");
    }

    private static void retentionByteLimitBelowContractRemediation(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue) {
        copyAttribute(values, issue, "maxBytes", "currentValue");
        copyAttribute(values, issue, "maxBytesDisplay", "currentValueDisplay");
        copyAttribute(values, issue, "recommendedMinBytes", "suggestedValue");
        copyAttribute(values, issue, "recommendedMinBytesDisplay", "suggestedValueDisplay");
    }

    private static void copyAttribute(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue,
            String sourceKey) {
        copyAttribute(values, issue, sourceKey, sourceKey);
    }

    private static void copyAttribute(
            Map<String, Object> values,
            AgenticCommerceWayangPersistenceConfigValidationIssue issue,
            String sourceKey,
            String targetKey) {
        if (issue.attributes().containsKey(sourceKey)) {
            values.put(targetKey, issue.attributes().get(sourceKey));
        }
    }

    private static int number(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = AgenticCommerceWayangMaps.text(value);
        if (!text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
