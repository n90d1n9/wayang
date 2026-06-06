package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP route descriptors.
 */
final class WayangA2uiHttpRouteProjection {

    private WayangA2uiHttpRouteProjection() {
    }

    static Map<String, Object> route(WayangA2uiHttpRoute route) {
        WayangA2uiHttpRoute resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", resolved.operation());
        values.put("method", resolved.method());
        values.put("allowedMethods", resolved.allowedMethods());
        values.put("allowHeader", resolved.allowHeader());
        values.put("path", resolved.path());
        values.put("requestContentType", resolved.requestContentType());
        values.put("responseContentType", resolved.responseContentType());
        values.put("requestBodyRequired", resolved.requestBodyRequired());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> binding(WayangA2uiHttpRouteBinding binding) {
        WayangA2uiHttpRouteBinding resolved = Objects.requireNonNull(binding, "binding");
        Map<String, Object> values = new LinkedHashMap<>(resolved.route().toMap());
        values.put("published", true);
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> catalog(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("routeCount", resolved.routeCount());
        values.put("routes", resolved.routes().stream()
                .map(WayangA2uiHttpRouteProjection::route)
                .toList());
        return WayangA2uiTransportMaps.freeze(values);
    }
}
