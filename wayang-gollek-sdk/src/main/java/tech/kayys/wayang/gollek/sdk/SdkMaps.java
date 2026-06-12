package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class SdkMaps {

    private SdkMaps() {
    }

    static Map<String, Object> copy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            Object normalizedValue = normalizeValue(value);
            if (!normalizedKey.isEmpty() && normalizedValue != null) {
                copy.put(normalizedKey, normalizedValue);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    static Map<String, Object> orderedCopy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    static <T> Map<String, T> orderedTypedCopy(Map<String, T> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof String string) {
            String normalized = SdkText.trimToEmpty(string);
            return normalized.isEmpty() ? null : normalized;
        }
        return value;
    }
}
