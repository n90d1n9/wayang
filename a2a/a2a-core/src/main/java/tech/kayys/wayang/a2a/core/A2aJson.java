package tech.kayys.wayang.a2a.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class A2aJson {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private A2aJson() {
    }

    static String write(Map<String, Object> payload, String type) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to encode A2A " + type, e);
        }
    }

    static Map<String, Object> read(String json, String type) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("A2A " + type + " JSON must not be blank");
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to decode A2A " + type, e);
        }
    }
}
