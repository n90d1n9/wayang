package tech.kayys.wayang.a2ui.wayang.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small string-map normalization helpers for A2UI context maps.
 */
public final class StringMaps {

    private StringMaps() {
    }

    public static Map<String, String> copyStringValues(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> putIfPresent(copy, key, value));
        return freeze(copy);
    }

    public static Map<String, String> stringValues(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        map.forEach((key, entryValue) -> {
            if (entryValue != null) {
                putIfPresent(copy, key, String.valueOf(entryValue));
            }
        });
        return freeze(copy);
    }

    private static void putIfPresent(Map<String, String> copy, Object key, String value) {
        String normalizedKey = key == null ? "" : String.valueOf(key).trim();
        if (!normalizedKey.isBlank() && value != null) {
            copy.put(normalizedKey, value);
        }
    }

    private static Map<String, String> freeze(Map<String, String> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
