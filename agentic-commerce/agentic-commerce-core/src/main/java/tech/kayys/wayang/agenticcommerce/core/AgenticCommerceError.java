package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework-neutral Agentic Commerce error payload.
 */
public record AgenticCommerceError(
        String type,
        String code,
        String message,
        Map<String, Object> details,
        Map<String, Object> metadata) {

    public AgenticCommerceError {
        type = AgenticCommerceValues.textValue(type);
        code = AgenticCommerceValues.textValue(code);
        message = AgenticCommerceValues.textValue(message);
        details = AgenticCommerceMaps.copy(details);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceError of(String code, String message) {
        return new AgenticCommerceError("invalid_request", code, message, Map.of(), Map.of());
    }

    public static AgenticCommerceError fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceError("", "", "", Map.of(), Map.of());
        }
        Map<String, Object> error = AgenticCommerceValues.map(values, "error");
        Map<?, ?> source = error.isEmpty() ? values : error;
        return new AgenticCommerceError(
                AgenticCommerceValues.text(source, "type"),
                AgenticCommerceValues.text(source, "code"),
                AgenticCommerceValues.text(source, "message"),
                AgenticCommerceValues.map(source, "details", "detail"),
                AgenticCommerceValues.metadata(source, "type", "code", "message", "details", "detail"));
    }

    public boolean isEmpty() {
        return type.isBlank()
                && code.isBlank()
                && message.isBlank()
                && details.isEmpty()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "type", type);
        AgenticCommerceValues.putText(values, "code", code);
        AgenticCommerceValues.putText(values, "message", message);
        AgenticCommerceValues.putMap(values, "details", details);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
