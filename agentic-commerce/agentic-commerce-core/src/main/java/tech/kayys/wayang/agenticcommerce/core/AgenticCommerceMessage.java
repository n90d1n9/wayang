package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seller or protocol message attached to a checkout response.
 */
public record AgenticCommerceMessage(
        String type,
        String code,
        String message,
        Map<String, Object> metadata) {

    public AgenticCommerceMessage {
        type = AgenticCommerceValues.textValue(type);
        code = AgenticCommerceValues.textValue(code);
        message = AgenticCommerceValues.textValue(message);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceMessage fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceMessage("", "", "", Map.of());
        }
        return new AgenticCommerceMessage(
                AgenticCommerceValues.text(values, "type", "level"),
                AgenticCommerceValues.text(values, "code"),
                AgenticCommerceValues.text(values, "message", "text"),
                AgenticCommerceValues.metadata(values, "type", "level", "code", "message", "text"));
    }

    public boolean isEmpty() {
        return type.isBlank() && code.isBlank() && message.isBlank() && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "type", type);
        AgenticCommerceValues.putText(values, "code", code);
        AgenticCommerceValues.putText(values, "message", message);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
