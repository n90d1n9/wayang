package tech.kayys.wayang.a2ui.wayang.projection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defensive map helpers used by projection-only JSON envelope builders.
 */
final class ProjectionMaps {

    private ProjectionMaps() {
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

    static Map<String, Object> freeze(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(ProjectionMaps::copyValue)
                    .toList();
        }
        return value;
    }
}
