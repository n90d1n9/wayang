package tech.kayys.wayang.a2ui.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class A2uiValues {

    private A2uiValues() {
    }

    static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static Map<String, Object> copyMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                copy.put(String.valueOf(key), copyValue(value));
            }
        });
        return Map.copyOf(copy);
    }

    static List<Object> copyList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(A2uiValues::copyValue)
                .toList();
    }

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return copyList(list);
        }
        return value;
    }

    static Map<String, Object> payload(String key, Map<String, Object> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, body == null ? Map.of() : body);
        return Map.copyOf(payload);
    }

    static void putOptional(Map<String, Object> target, String key, String value) {
        String normalized = optional(value);
        if (normalized != null) {
            target.put(key, normalized);
        }
    }

    static void putOptional(Map<String, Object> target, String key, Map<String, Object> value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    static String timestamp(Instant timestamp) {
        return (timestamp == null ? Instant.now() : timestamp).toString();
    }
}
