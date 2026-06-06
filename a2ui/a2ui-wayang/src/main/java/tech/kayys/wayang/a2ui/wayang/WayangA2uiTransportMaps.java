package tech.kayys.wayang.a2ui.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defensive copy helpers for transport envelope maps and nested values.
 */
final class WayangA2uiTransportMaps {

    private WayangA2uiTransportMaps() {
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
        return freeze(copy);
    }

    static List<Map<String, Object>> copyMaps(List<? extends Map<?, ?>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(WayangA2uiTransportMaps::copy)
                .toList();
    }

    static Map<String, Object> copyMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        return Map.of();
    }

    static Map<String, Object> freeze(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    static List<Map<String, Object>> copyMapList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(WayangA2uiTransportMaps::copy)
                .toList();
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(WayangA2uiTransportMaps::copyValue)
                    .toList();
        }
        return value;
    }
}
