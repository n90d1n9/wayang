package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Shared request construction for JSON-RPC HTTP diagnostics and probes.
 */
final class WayangA2aJsonRpcHttpRequests {

    private WayangA2aJsonRpcHttpRequests() {
    }

    static WayangA2aHttpRequest getJson(String path) {
        return new WayangA2aHttpRequest(
                "GET",
                path,
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of());
    }

    static WayangA2aHttpRequest routeProbe(String method, String path) {
        return new WayangA2aHttpRequest(method, path, "", Map.of(), Map.of());
    }
}
