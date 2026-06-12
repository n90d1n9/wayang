package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangReportMaps {

    private WayangReportMaps() {
    }

    static List<Map<String, Object>> copyObjects(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(values.stream()
                .map(WayangReportMaps::copyMap)
                .toList());
    }

    static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            if (!normalizedKey.isEmpty() && value != null) {
                copy.put(normalizedKey, copyValue(value));
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof String text) {
            return WayangSecretRedactor.connectionString(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                String normalizedKey = SdkText.trimToEmpty(String.valueOf(key));
                if (!normalizedKey.isEmpty() && nested != null) {
                    copy.put(normalizedKey, copyValue(nested));
                }
            });
            return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            return Collections.unmodifiableList(list.stream()
                    .map(WayangReportMaps::copyValue)
                    .toList());
        }
        return value;
    }
}
