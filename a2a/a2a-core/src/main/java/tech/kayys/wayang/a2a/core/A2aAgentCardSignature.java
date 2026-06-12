package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWS signature entry for a signed Agent Card.
 */
public record A2aAgentCardSignature(
        String protectedHeader,
        String signature,
        Map<String, Object> header) {

    public A2aAgentCardSignature {
        protectedHeader = A2aValues.required(protectedHeader, "protected");
        signature = A2aValues.required(signature, "signature");
        header = A2aValues.copyMap(header);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("protected", protectedHeader);
        payload.put("signature", signature);
        A2aValues.putOptional(payload, "header", header);
        return A2aValues.copyMap(payload);
    }

    public static A2aAgentCardSignature fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aAgentCardSignature(
                A2aValues.string(source, "protected"),
                A2aValues.string(source, "signature"),
                A2aValues.objectOrEmpty(source, "header"));
    }
}
