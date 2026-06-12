package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC HTTP route catalogs.
 */
final class WayangA2aJsonRpcHttpRouteCatalogProjection {

    private WayangA2aJsonRpcHttpRouteCatalogProjection() {
    }

    static Map<String, Object> catalog(WayangA2aJsonRpcHttpRouteCatalog catalog) {
        WayangA2aJsonRpcHttpRouteCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("binding", A2aProtocol.BINDING_JSONRPC);
        values.put("protocolVersion", A2aProtocol.VERSION);
        values.put("routeCount", resolved.routeCount());
        values.put("enabledRouteCount", resolved.enabledRouteCount());
        values.put("routes", resolved.routes().stream()
                .map(WayangA2aJsonRpcHttpRoute::toMap)
                .toList());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcHttpRouteCatalog catalog) {
        WayangA2aJsonRpcHttpRouteCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        return WayangA2aJsonRpcHttpResponses.json(
                WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
                resolved.toJson());
    }
}
