package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class AgenticCommerceWayangMaps {

    private AgenticCommerceWayangMaps() {
    }

    static Map<String, Object> copy(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(String.valueOf(key), copyValue(value));
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceWayangMaps::copyValue)
                    .toList();
        }
        return value;
    }

    static String required(String value, String name) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static Optional<String> optional(Object value) {
        String text = text(value);
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    static String firstText(Map<?, ?> values, String... keys) {
        return optional(first(values, keys)).orElse("");
    }

    static Object first(Map<?, ?> values, String... keys) {
        if (values == null || values.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    static Optional<Boolean> firstBoolean(Map<?, ?> values, String... keys) {
        if (values == null || values.isEmpty() || keys == null) {
            return Optional.empty();
        }
        for (String key : keys) {
            if (key == null || !values.containsKey(key)) {
                continue;
            }
            Object value = values.get(key);
            if (value instanceof Boolean booleanValue) {
                return Optional.of(booleanValue);
            }
            String text = AgenticCommerceWayangMaps.text(value);
            if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text) || "1".equals(text)) {
                return Optional.of(true);
            }
            if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text) || "0".equals(text)) {
                return Optional.of(false);
            }
        }
        return Optional.empty();
    }

    static int firstInt(Map<?, ?> values, int defaultValue, String... keys) {
        if (values == null || values.isEmpty() || keys == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (key == null || !values.containsKey(key)) {
                continue;
            }
            Object value = values.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            String text = AgenticCommerceWayangMaps.text(value);
            if (!text.isBlank()) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    static Map<String, Object> firstMap(Map<?, ?> values, String... keys) {
        Object value = first(values, keys);
        return value instanceof Map<?, ?> map ? copy(map) : Map.of();
    }

    static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(AgenticCommerceWayangMaps::text)
                    .filter(text -> !text.isBlank())
                    .distinct()
                    .toList();
        }
        String text = text(value);
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    static void putText(Map<String, Object> values, String key, String value) {
        String normalized = text(value);
        if (!normalized.isBlank()) {
            values.put(key, normalized);
        }
    }
}
