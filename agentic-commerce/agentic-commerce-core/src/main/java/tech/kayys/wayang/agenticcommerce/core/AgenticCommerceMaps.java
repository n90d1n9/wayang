package tech.kayys.wayang.agenticcommerce.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defensive copy helpers for Agentic Commerce DTO maps.
 */
final class AgenticCommerceMaps {

    private AgenticCommerceMaps() {
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

    static List<Map<String, Object>> copyMaps(List<? extends Map<?, ?>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(AgenticCommerceMaps::copy)
                .toList();
    }

    static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(AgenticCommerceMaps::copyValue)
                    .toList();
        }
        return value;
    }
}
