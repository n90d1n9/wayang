package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional request body for canceling an Agentic Commerce checkout session.
 */
public record AgenticCommerceCancelCheckoutSessionRequest(
        AgenticCommerceIntentTrace intentTrace,
        Map<String, Object> metadata) implements AgenticCommerceCheckoutPayload {

    public AgenticCommerceCancelCheckoutSessionRequest {
        intentTrace = intentTrace == null ? AgenticCommerceIntentTrace.empty() : intentTrace;
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCancelCheckoutSessionRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCancelCheckoutSessionRequest(AgenticCommerceIntentTrace.empty(), Map.of());
        }
        return new AgenticCommerceCancelCheckoutSessionRequest(
                AgenticCommerceIntentTrace.fromMap(AgenticCommerceValues.map(values, "intent_trace", "intentTrace")),
                AgenticCommerceValues.metadata(values, "intent_trace", "intentTrace"));
    }

    @Override
    public boolean isEmpty() {
        return intentTrace.isEmpty() && metadata.isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (!intentTrace.isEmpty()) {
            values.put("intent_trace", intentTrace.toMap());
        }
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
