package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for SendMessage and SendStreamingMessage.
 */
public record A2aSendMessageRequest(
        String tenant,
        A2aMessage message,
        A2aSendMessageConfiguration configuration,
        Map<String, Object> metadata) {

    public A2aSendMessageRequest {
        tenant = A2aValues.optional(tenant);
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        metadata = A2aValues.copyMap(metadata);
    }

    public static A2aSendMessageRequest of(A2aMessage message) {
        return new A2aSendMessageRequest(null, message, null, Map.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        A2aValues.putOptional(payload, "tenant", tenant);
        payload.put("message", message.toMap());
        if (configuration != null) {
            payload.put("configuration", configuration.toMap());
        }
        A2aValues.putOptional(payload, "metadata", metadata);
        return A2aValues.copyMap(payload);
    }

    public String toJson() {
        return A2aJson.write(toMap(), "send message request");
    }

    public static A2aSendMessageRequest fromJson(String json) {
        return fromMap(A2aJson.read(json, "send message request"));
    }

    public static A2aSendMessageRequest fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        A2aSendMessageConfiguration config = source.get("configuration") instanceof Map<?, ?> configMap
                ? A2aSendMessageConfiguration.fromMap(configMap)
                : null;
        return new A2aSendMessageRequest(
                A2aValues.optionalString(source, "tenant"),
                A2aMessage.fromMap(A2aValues.objectRequired(source, "message")),
                config,
                A2aValues.objectOrEmpty(source, "metadata"));
    }
}
