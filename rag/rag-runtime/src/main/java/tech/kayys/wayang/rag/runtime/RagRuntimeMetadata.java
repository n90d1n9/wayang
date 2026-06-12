package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class RagRuntimeMetadata {

    private RagRuntimeMetadata() {
    }

    static Map<String, Object> copy(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    static Map<String, Object> mutableCopy(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    static Map<String, String> copyStrings(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copied = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                copied.put(key, value);
            }
        });
        if (copied.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(copied);
    }

    static void copyStringsInto(Map<String, String> source, Map<String, Object> target) {
        if (target == null) {
            return;
        }
        copyStrings(source).forEach(target::put);
    }

    static Map<String, String> stringifyValues(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copied = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                copied.put(key, String.valueOf(value));
            }
        });
        if (copied.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(copied);
    }

    static Map<String, Object> fromResult(RagResult result) {
        return result == null ? Map.of() : copy(result.metadata());
    }
}
