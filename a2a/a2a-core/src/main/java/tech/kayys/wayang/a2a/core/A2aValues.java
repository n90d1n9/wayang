package tech.kayys.wayang.a2a.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class A2aValues {

    private A2aValues() {
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
        return Collections.unmodifiableMap(copy);
    }

    static List<Object> copyList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Object> copy = new ArrayList<>(values.size());
        values.forEach(value -> copy.add(copyValue(value)));
        return Collections.unmodifiableList(copy);
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

    static void putOptional(Map<String, Object> target, String key, String value) {
        String normalized = optional(value);
        if (normalized != null) {
            target.put(key, normalized);
        }
    }

    static void putOptional(Map<String, Object> target, String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                target.put(key, copyMap(map));
            }
            return;
        }
        if (value instanceof List<?> list) {
            if (!list.isEmpty()) {
                target.put(key, copyList(list));
            }
            return;
        }
        if (value != null) {
            target.put(key, value);
        }
    }

    static Map<String, Object> objectRequired(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        throw new IllegalArgumentException("A2A field must be an object: " + key);
    }

    static Map<String, Object> objectOrEmpty(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        throw new IllegalArgumentException("A2A field must be an object: " + key);
    }

    static String string(Map<String, Object> payload, String key) {
        String value = optionalString(payload, key);
        if (value == null) {
            throw new IllegalArgumentException("A2A field is required: " + key);
        }
        return value;
    }

    static String optionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : optional(String.valueOf(value));
    }

    static Boolean optionalBoolean(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static boolean booleanOrFalse(Map<String, Object> payload, String key) {
        Boolean value = optionalBoolean(payload, key);
        return value != null && value;
    }

    static Integer optionalInteger(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A2A field must be an integer: " + key, e);
        }
    }

    static List<String> stringList(Object raw, String field) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("A2A field must be an array: " + field);
        }
        List<String> values = new ArrayList<>();
        for (Object value : list) {
            String normalized = optional(value == null ? null : String.valueOf(value));
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return Collections.unmodifiableList(values);
    }

    static List<Map<String, Object>> objectList(Object raw, String field) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("A2A field must be an array: " + field);
        }
        List<Map<String, Object>> values = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("A2A " + field + " entry must be an object");
            }
            values.add(copyMap(map));
        }
        return Collections.unmodifiableList(values);
    }

    static <T> List<T> copyRecords(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
