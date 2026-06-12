package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protocol extension declaration from an A2A Agent Card.
 */
public record A2aAgentExtension(
        String uri,
        String description,
        boolean required,
        Map<String, Object> params) {

    public A2aAgentExtension {
        uri = A2aValues.optional(uri);
        description = A2aValues.optional(description);
        params = A2aValues.copyMap(params);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        A2aValues.putOptional(payload, "uri", uri);
        A2aValues.putOptional(payload, "description", description);
        if (required) {
            payload.put("required", true);
        }
        A2aValues.putOptional(payload, "params", params);
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentExtension fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentExtension(
                A2aValues.optionalString(source, "uri"),
                A2aValues.optionalString(source, "description"),
                A2aValues.booleanOrFalse(source, "required"),
                A2aValues.objectOrEmpty(source, "params"));
    }
}
