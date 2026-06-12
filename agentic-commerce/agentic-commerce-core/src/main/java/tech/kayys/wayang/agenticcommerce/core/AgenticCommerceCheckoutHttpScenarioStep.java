package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One executable checkout HTTP harness step.
 */
public record AgenticCommerceCheckoutHttpScenarioStep(
        String id,
        AgenticCommerceHttpRequest request,
        int expectedStatusCode,
        boolean expectedSuccessful,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpScenarioStep {
        id = AgenticCommerceValues.textValue(id);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce scenario step id must not be blank");
        }
        request = Objects.requireNonNull(request, "request");
        expectedStatusCode = expectedStatusCode <= 0 ? 200 : expectedStatusCode;
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpScenarioStep successful(
            String id,
            AgenticCommerceHttpRequest request,
            int expectedStatusCode) {
        return new AgenticCommerceCheckoutHttpScenarioStep(id, request, expectedStatusCode, true, Map.of());
    }

    public String operation() {
        return AgenticCommerceValues.text(request.attributes(), "operation");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("operation", operation());
        values.put("expectedStatusCode", expectedStatusCode);
        values.put("expectedSuccessful", expectedSuccessful);
        values.put("request", requestMap());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private Map<String, Object> requestMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", request.method());
        values.put("path", request.path());
        values.put("bodyPresent", !request.body().isBlank());
        values.put("attributes", request.attributes());
        return AgenticCommerceMaps.copy(values);
    }
}
