package tech.kayys.wayang.a2ui.wayang.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Shared JSON codec for Wayang A2UI transport envelopes.
 */
public final class TransportJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private TransportJson() {
    }

    public static Map<String, Object> map(String json, String blankMessage, String decodeMessage) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(blankMessage);
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(decodeMessage, e);
        }
    }

    public static String json(Map<String, Object> payload, String encodeMessage) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(encodeMessage, e);
        }
    }
}
