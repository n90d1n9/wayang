package tech.kayys.wayang.agent.api;

import java.util.List;
import java.util.Map;

/**
 * Parsed view of learning-audit retention metadata shared by API responses.
 */
record HermesLearningAuditRetentionProjection(
        Map<String, Object> metadata,
        String ledgerType,
        boolean bounded,
        int recordCount,
        int maxEntries,
        int remainingEntries,
        int overflowEntries,
        int utilizationPercent,
        boolean nearCapacity,
        boolean atCapacity,
        String status,
        String severity,
        int priority,
        boolean requiresAttention,
        List<String> attention,
        List<String> recommendedActions) {

    HermesLearningAuditRetentionProjection {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        ledgerType = HermesResponseMetadata.text(ledgerType, "unknown");
        status = HermesResponseMetadata.text(status, "");
        severity = HermesResponseMetadata.text(severity, "");
        attention = attention == null ? List.of() : List.copyOf(attention);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
    }

    static HermesLearningAuditRetentionProjection from(Map<String, Object> metadata) {
        Map<String, Object> values = metadata == null ? Map.of() : Map.copyOf(metadata);
        return new HermesLearningAuditRetentionProjection(
                values,
                HermesResponseMetadata.text(values.get("ledgerType"), "unknown"),
                HermesResponseMetadata.bool(values.get("bounded")),
                HermesResponseMetadata.integer(values.get("recordCount")),
                HermesResponseMetadata.integer(values.get("maxEntries")),
                HermesResponseMetadata.integer(values.get("remainingEntries")),
                HermesResponseMetadata.integer(values.get("overflowEntries")),
                HermesResponseMetadata.integer(values.get("utilizationPercent")),
                HermesResponseMetadata.bool(values.get("nearCapacity")),
                HermesResponseMetadata.bool(values.get("atCapacity")),
                HermesResponseMetadata.text(values.get("status"), ""),
                HermesResponseMetadata.text(values.get("severity"), ""),
                HermesResponseMetadata.integer(values.get("priority")),
                HermesResponseMetadata.bool(values.get("requiresAttention")),
                HermesResponseMetadata.strings(values.get("attention")),
                HermesResponseMetadata.strings(values.get("recommendedActions")));
    }
}
