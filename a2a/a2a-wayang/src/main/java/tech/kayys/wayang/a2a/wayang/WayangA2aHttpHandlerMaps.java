package tech.kayys.wayang.a2a.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered HTTP operation handler map assembly shared by handler factories.
 */
final class WayangA2aHttpHandlerMaps {

    private WayangA2aHttpHandlerMaps() {
    }

    static Builder builder() {
        return new Builder();
    }

    static Map<String, WayangA2aHttpOperationHandler> merge(
            Map<String, ? extends WayangA2aHttpOperationHandler> first,
            Map<String, ? extends WayangA2aHttpOperationHandler> second) {
        return builder()
                .putAllIfPresent(first)
                .putAllIfPresent(second)
                .build();
    }

    static Map<String, WayangA2aHttpOperationHandler> copyStrict(
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers) {
        Builder builder = builder();
        if (handlers != null) {
            handlers.forEach((operation, handler) -> {
                if (handler != null) {
                    builder.put(operation, handler);
                }
            });
        }
        return builder.build();
    }

    static final class Builder {

        private final Map<String, WayangA2aHttpOperationHandler> handlers = new LinkedHashMap<>();

        Builder put(String operation, WayangA2aHttpOperationHandler handler) {
            if (handler != null) {
                handlers.put(normalizeOperation(operation), handler);
            }
            return this;
        }

        Builder putAllIfPresent(Map<String, ? extends WayangA2aHttpOperationHandler> values) {
            if (values != null) {
                values.forEach((operation, handler) -> {
                    if (operation != null && handler != null) {
                        handlers.put(operation.trim(), handler);
                    }
                });
            }
            return this;
        }

        Map<String, WayangA2aHttpOperationHandler> build() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
        }
    }

    private static String normalizeOperation(String operation) {
        String normalized = operation == null ? "" : operation.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("A2A HTTP operation handler key must not be blank");
        }
        return normalized;
    }
}
