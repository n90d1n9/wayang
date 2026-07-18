package tech.kayys.wayang.agent.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpMaps {

    private McpMaps() {
    }

    static Map<String, Object> copy(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copied.put(String.valueOf(key), snapshot(value));
            }
        });
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }

    static Map<String, Object> fromObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return copy(map);
    }

    private static Object snapshot(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copy(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null)
                    .map(McpMaps::snapshot)
                    .toList();
        }
        return value;
    }
}
