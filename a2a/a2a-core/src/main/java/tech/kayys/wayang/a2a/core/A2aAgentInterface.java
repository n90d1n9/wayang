package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binding endpoint advertised by an A2A Agent Card.
 */
public record A2aAgentInterface(
        String url,
        String protocolBinding,
        String tenant,
        String protocolVersion) {

    public A2aAgentInterface {
        url = A2aValues.required(url, "url");
        protocolBinding = A2aValues.required(protocolBinding, "protocolBinding");
        tenant = A2aValues.optional(tenant);
        protocolVersion = protocolVersion == null || protocolVersion.isBlank()
                ? A2aProtocol.VERSION
                : protocolVersion.trim();
    }

    public static A2aAgentInterface httpJson(String url) {
        return new A2aAgentInterface(url, A2aProtocol.BINDING_HTTP_JSON, null, A2aProtocol.VERSION);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", url);
        payload.put("protocolBinding", protocolBinding);
        A2aValues.putOptional(payload, "tenant", tenant);
        payload.put("protocolVersion", protocolVersion);
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentInterface fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentInterface(
                A2aValues.string(source, "url"),
                A2aValues.string(source, "protocolBinding"),
                A2aValues.optionalString(source, "tenant"),
                A2aValues.string(source, "protocolVersion"));
    }
}
