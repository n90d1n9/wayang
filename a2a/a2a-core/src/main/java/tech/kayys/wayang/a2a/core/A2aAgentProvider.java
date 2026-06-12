package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service provider identity advertised in an A2A Agent Card.
 */
public record A2aAgentProvider(String url, String organization) {

    public A2aAgentProvider {
        url = A2aValues.required(url, "url");
        organization = A2aValues.required(organization, "organization");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", url);
        payload.put("organization", organization);
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentProvider fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentProvider(
                A2aValues.string(source, "url"),
                A2aValues.string(source, "organization"));
    }
}
