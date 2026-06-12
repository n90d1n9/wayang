package tech.kayys.wayang.a2ui.wayang.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small map normalization helpers for A2UI record constructors.
 */
public final class RecordMaps {

    private RecordMaps() {
    }

    public static Map<String, Object> nullableValues(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                copy.put(key, value);
            }
        });
        return freeze(copy);
    }

    public static Map<String, Object> stringKeysNonNullValues(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(String.valueOf(key), value);
            }
        });
        return freeze(copy);
    }

    private static Map<String, Object> freeze(Map<String, Object> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
