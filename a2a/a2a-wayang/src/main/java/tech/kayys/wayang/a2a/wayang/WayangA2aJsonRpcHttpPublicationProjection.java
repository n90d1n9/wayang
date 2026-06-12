package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC HTTP route publication plans.
 */
final class WayangA2aJsonRpcHttpPublicationProjection {

    private WayangA2aJsonRpcHttpPublicationProjection() {
    }

    static Map<String, Object> publication(WayangA2aJsonRpcHttpPublication publication) {
        WayangA2aJsonRpcHttpPublication resolved = Objects.requireNonNull(publication, "publication");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("routeCount", resolved.routeCount());
        values.put("enabledRouteCount", resolved.enabledRouteCount());
        values.put("publishedRouteCount", resolved.publishedRouteCount());
        values.put("operations", resolved.operations());
        values.put("routes", resolved.bindings().stream()
                .map(WayangA2aJsonRpcHttpPublicationProjection::binding)
                .toList());
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> binding(WayangA2aJsonRpcHttpRouteBinding binding) {
        WayangA2aJsonRpcHttpRouteBinding resolved = Objects.requireNonNull(binding, "binding");
        Map<String, Object> values = new LinkedHashMap<>(resolved.route().toMap());
        values.put("published", true);
        return WayangA2aMaps.copyMap(values);
    }
}
