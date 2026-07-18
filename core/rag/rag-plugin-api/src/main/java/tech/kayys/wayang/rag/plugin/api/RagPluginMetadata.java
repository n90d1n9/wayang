package tech.kayys.wayang.rag.plugin.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata copy helpers shared by plugin API records and plugin implementations.
 */
public final class RagPluginMetadata {

    private RagPluginMetadata() {
    }

    public static Map<String, Object> copy(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public static Map<String, Object> with(Map<String, Object> metadata, String key, Object value) {
        Map<String, Object> updated = mutableCopy(metadata);
        updated.put(key, value);
        return copy(updated);
    }

    public static Map<String, Object> with(Map<String, Object> metadata, Map<String, Object> additions) {
        Map<String, Object> updated = mutableCopy(metadata);
        if (additions != null) {
            updated.putAll(additions);
        }
        return copy(updated);
    }

    private static Map<String, Object> mutableCopy(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }
}
