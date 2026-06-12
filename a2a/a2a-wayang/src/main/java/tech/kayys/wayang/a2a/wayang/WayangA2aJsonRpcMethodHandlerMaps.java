package tech.kayys.wayang.a2a.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered JSON-RPC method handler map assembly shared by method groups.
 */
final class WayangA2aJsonRpcMethodHandlerMaps {

    private WayangA2aJsonRpcMethodHandlerMaps() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private final Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers = new LinkedHashMap<>();

        Builder put(String method, WayangA2aJsonRpcMethodDispatchTable.Handler handler) {
            if (handler != null) {
                handlers.put(WayangA2aMaps.required(method, "method"), handler);
            }
            return this;
        }

        Builder putAll(Map<String, ? extends WayangA2aJsonRpcMethodDispatchTable.Handler> values) {
            if (values != null) {
                values.forEach(this::put);
            }
            return this;
        }

        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> build() {
            return Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
        }
    }
}
