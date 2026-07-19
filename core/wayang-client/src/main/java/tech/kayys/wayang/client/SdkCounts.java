package tech.kayys.wayang.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final public class SdkCounts {

    private SdkCounts() {
    }

    public static Map<String, Integer> copy(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static Map<String, Integer> copyPositiveTextKeys(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            if (!normalizedKey.isEmpty() && value != null && value > 0) {
                copy.put(normalizedKey, value);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    public static <K> Map<K, Integer> copyPositiveKeys(Map<K, Integer> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<K, Integer> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null && value > 0) {
                copy.put(key, value);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }
}
