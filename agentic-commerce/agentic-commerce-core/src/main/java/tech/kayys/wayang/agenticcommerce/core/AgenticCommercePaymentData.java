package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payment data submitted when completing an Agentic Commerce checkout session.
 */
public record AgenticCommercePaymentData(
        String handlerId,
        Map<String, Object> instrument,
        String provider,
        String token,
        Map<String, Object> metadata) {

    public AgenticCommercePaymentData {
        handlerId = AgenticCommerceValues.textValue(handlerId);
        instrument = AgenticCommerceMaps.copy(instrument);
        provider = AgenticCommerceValues.textValue(provider);
        token = AgenticCommerceValues.textValue(token);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommercePaymentData empty() {
        return new AgenticCommercePaymentData("", Map.of(), "", "", Map.of());
    }

    public static AgenticCommercePaymentData fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommercePaymentData(
                AgenticCommerceValues.text(values, "handler_id", "handlerId"),
                AgenticCommerceValues.map(values, "instrument"),
                AgenticCommerceValues.text(values, "provider"),
                AgenticCommerceValues.text(values, "token"),
                AgenticCommerceValues.metadata(
                        values,
                        "handler_id",
                        "handlerId",
                        "instrument",
                        "provider",
                        "token"));
    }

    public boolean isEmpty() {
        return handlerId.isBlank()
                && instrument.isEmpty()
                && provider.isBlank()
                && token.isBlank()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "handler_id", handlerId);
        AgenticCommerceValues.putMap(values, "instrument", instrument);
        AgenticCommerceValues.putText(values, "provider", provider);
        AgenticCommerceValues.putText(values, "token", token);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
