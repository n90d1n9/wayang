package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class WayangA2aMaps {

    private WayangA2aMaps() {
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

    static String optional(Object value) {
        return value == null ? null : optional(String.valueOf(value));
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

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(WayangA2aMaps::copyValue).toList();
        }
        return value;
    }

    static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangA2aMaps::optional)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        String text = optional(value);
        if (text == null) {
            return List.of();
        }
        return List.of(text.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    static List<String> firstStringList(Map<String, ?> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        for (String key : keys) {
            List<String> list = stringList(values.get(key));
            if (!list.isEmpty()) {
                return list;
            }
        }
        return List.of();
    }

    static Optional<String> firstString(Map<String, ?> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        for (String key : keys) {
            String text = optional(values.get(key));
            if (text != null) {
                return Optional.of(text);
            }
        }
        return Optional.empty();
    }

    static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> map) {
                objects.add(copyMap(map));
            }
        }
        return List.copyOf(objects);
    }
}
