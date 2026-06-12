package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared coercion helpers for JSON-backed A2A report and probe DTOs.
 */
final class WayangA2aJsonReportMaps {

    private WayangA2aJsonReportMaps() {
    }

    static Map<String, Object> bodyMap(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return WayangA2aHttpJson.read(body);
    }

    static Map<String, Object> lenientBodyMap(String body) {
        try {
            return bodyMap(body);
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    static Map<String, Object> child(Map<String, Object> values, String key) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return map(values.get(key));
    }

    static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return WayangA2aMaps.copyMap(map);
        }
        return Map.of();
    }

    static List<Map<String, Object>> copyObjects(List<Map<String, Object>> objects) {
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }
        return objects.stream()
                .filter(Objects::nonNull)
                .map(WayangA2aMaps::copyMap)
                .toList();
    }

    static List<Map<String, Object>> copyMaps(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().map(WayangA2aMaps::copyMap).toList();
    }

    static String text(Object value, String fallback) {
        String text = WayangA2aMaps.optional(value);
        return text == null ? fallback : text;
    }

    static int number(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        String text = WayangA2aMaps.optional(value);
        if (text == null) {
            return fallback;
        }
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static boolean bool(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = WayangA2aMaps.optional(value);
        return text == null ? fallback : Boolean.parseBoolean(text);
    }
}
