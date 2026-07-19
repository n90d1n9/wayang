package tech.kayys.wayang.tool.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class McpMaps {

    private McpMaps() {
    }

    static Map<String, Object> copy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    static Map<String, Object> fromObject(Object value) {
        if (!(value instanceof Map<?, ?> raw) || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        raw.forEach((key, entryValue) -> {
            if (key instanceof String stringKey) {
                copied.put(stringKey, entryValue);
            }
        });
        if (copied.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(copied);
    }
}
