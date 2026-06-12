package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent-side intent trace for cancellation and abandonment analytics.
 */
public record AgenticCommerceIntentTrace(
        String reasonCode,
        String traceSummary,
        Map<String, Object> metadata) {

    public AgenticCommerceIntentTrace {
        reasonCode = AgenticCommerceValues.textValue(reasonCode);
        traceSummary = AgenticCommerceValues.textValue(traceSummary);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceIntentTrace empty() {
        return new AgenticCommerceIntentTrace("", "", Map.of());
    }

    public static AgenticCommerceIntentTrace fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceIntentTrace(
                AgenticCommerceValues.text(values, "reason_code", "reasonCode"),
                AgenticCommerceValues.text(values, "trace_summary", "traceSummary", "summary"),
                AgenticCommerceValues.metadata(values, "reason_code", "reasonCode", "trace_summary", "traceSummary", "summary"));
    }

    public boolean isEmpty() {
        return reasonCode.isBlank() && traceSummary.isBlank() && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "reason_code", reasonCode);
        AgenticCommerceValues.putText(values, "trace_summary", traceSummary);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
