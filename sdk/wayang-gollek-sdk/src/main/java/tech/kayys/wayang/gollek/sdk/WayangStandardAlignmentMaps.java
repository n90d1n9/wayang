package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class WayangStandardAlignmentMaps {

    private WayangStandardAlignmentMaps() {
    }

    static Map<String, Object> copy(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = text(key);
            Object normalizedValue = copyValue(value);
            if (!normalizedKey.isEmpty() && normalizedValue != null) {
                copy.put(normalizedKey, normalizedValue);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? copy(map) : Map.of();
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String firstText(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            String text = text(value(values, key));
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    static boolean bool(Map<?, ?> values, String key) {
        Object value = value(values, key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(text(value));
    }

    static int number(Map<?, ?> values, String key) {
        Object value = value(values, key);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        String text = text(value);
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static List<String> stringList(Map<?, ?> values, String key) {
        Object value = value(values, key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(WayangStandardAlignmentMaps::text)
                    .filter(text -> !text.isEmpty())
                    .toList();
        }
        String text = text(value);
        return text.isEmpty() ? List.of() : List.of(text);
    }

    private static Object value(Map<?, ?> values, String key) {
        if (values == null || key == null) {
            return null;
        }
        if (values.containsKey(key)) {
            return values.get(key);
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (key.equals(String.valueOf(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copied = list.stream()
                    .filter(Objects::nonNull)
                    .map(WayangStandardAlignmentMaps::copyValue)
                    .filter(Objects::nonNull)
                    .toList();
            return copied.isEmpty() ? null : copied;
        }
        if (value instanceof String string) {
            String text = text(string);
            return text.isEmpty() ? null : text;
        }
        return value;
    }
}
