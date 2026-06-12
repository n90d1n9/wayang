package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2A DataPart wrapper for one A2UI message.
 */
public record A2uiDataPart(Map<String, Object> data) {

    public A2uiDataPart {
        data = A2uiValues.copyMap(data);
    }

    public static A2uiDataPart of(A2uiServerMessage message) {
        return new A2uiDataPart(message.toPayload());
    }

    public static A2uiDataPart of(A2uiClientMessage message) {
        return new A2uiDataPart(message.toPayload());
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mimeType", A2uiProtocol.MIME_TYPE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "data");
        payload.put("data", data);
        payload.put("metadata", Map.copyOf(metadata));
        return Map.copyOf(payload);
    }
}
