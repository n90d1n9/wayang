package tech.kayys.wayang.agenticcommerce.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Small map-reading helpers for protocol DTOs without binding to a JSON library.
 */
final class AgenticCommerceValues {

    private AgenticCommerceValues() {
    }

    static Object first(Map<?, ?> values, String... keys) {
        if (values == null || values.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            if (key != null && values.containsKey(key)) {
                return values.get(key);
            }
        }
        return null;
    }

    static String text(Map<?, ?> values, String... keys) {
        return textValue(first(values, keys));
    }

    static String textValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String uppercaseText(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    static long longValue(Map<?, ?> values, String... keys) {
        return nonNegativeLong(first(values, keys));
    }

    static long nonNegativeLong(Object value) {
        long parsed = parseLong(value);
        return parsed < 0 ? 0 : parsed;
    }

    static Map<String, Object> map(Map<?, ?> values, String... keys) {
        Object value = first(values, keys);
        if (value instanceof Map<?, ?> map) {
            return AgenticCommerceMaps.copy(map);
        }
        return Map.of();
    }

    static List<Map<String, Object>> maps(Map<?, ?> values, String... keys) {
        Object value = first(values, keys);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(AgenticCommerceMaps::copy)
                    .toList();
        }
        if (value instanceof Map<?, ?> map) {
            return List.of(AgenticCommerceMaps.copy(map));
        }
        return List.of();
    }

    static List<Object> list(Map<?, ?> values, String... keys) {
        Object value = first(values, keys);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceMaps::copyValue)
                    .toList();
        }
        if (value == null) {
            return List.of();
        }
        return List.of(AgenticCommerceMaps.copyValue(value));
    }

    static List<String> stringList(Map<?, ?> values, String... keys) {
        return strings(list(values, keys));
    }

    static List<String> strings(List<?> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(AgenticCommerceValues::textValue)
                .filter(value -> !value.isBlank())
                .toList();
    }

    static Map<String, Object> metadata(Map<?, ?> values, String... knownKeys) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Set<String> known = Set.copyOf(Arrays.asList(knownKeys));
        Map<String, Object> metadata = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String name = key == null ? "" : String.valueOf(key);
            if (!name.isBlank() && value != null && !known.contains(name) && !"metadata".equals(name)) {
                metadata.put(name, value);
            }
        });
        metadata.putAll(map(values, "metadata"));
        return AgenticCommerceMaps.copy(metadata);
    }

    static void putText(Map<String, Object> values, String key, String value) {
        String normalized = textValue(value);
        if (!normalized.isBlank()) {
            values.put(key, normalized);
        }
    }

    static void putMap(Map<String, Object> values, String key, Map<String, Object> nested) {
        if (nested != null && !nested.isEmpty()) {
            values.put(key, nested);
        }
    }

    static void putList(Map<String, Object> values, String key, List<?> nested) {
        if (nested != null && !nested.isEmpty()) {
            values.put(key, nested.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceMaps::copyValue)
                    .toList());
        }
    }

    static void putStringList(Map<String, Object> values, String key, List<String> nested) {
        List<String> strings = strings(nested);
        if (!strings.isEmpty()) {
            values.put(key, strings);
        }
    }

    private static long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String normalized = textValue(value);
        if (normalized.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
