package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data-only publication manifest for mounted A2UI HTTP endpoint bindings.
 */
public record WayangA2uiHttpEndpointPublication(List<Map<String, Object>> routes) {

    public WayangA2uiHttpEndpointPublication {
        routes = WayangA2uiTransportMaps.copyMaps(routes);
    }

    public static WayangA2uiHttpEndpointPublication from(WayangA2uiHttpEndpointBinding endpoint) {
        WayangA2uiHttpEndpointBinding resolved = Objects.requireNonNull(endpoint, "endpoint");
        return new WayangA2uiHttpEndpointPublication(resolved.bindings().stream()
                .map(WayangA2uiHttpRouteBinding::toMap)
                .toList());
    }

    public int routeCount() {
        return routes.size();
    }

    public List<String> operations() {
        return values("operation");
    }

    public List<String> paths() {
        return values("path");
    }

    public Optional<Map<String, Object>> routeForOperation(String operation) {
        String normalized = WayangA2uiDecodeValues.text(operation);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return routes.stream()
                .filter(route -> normalized.equals(WayangA2uiDecodeValues.text(route.get("operation"))))
                .findFirst();
    }

    public Optional<Map<String, Object>> routeForPath(String path) {
        String normalized = normalizePath(path);
        return routes.stream()
                .filter(route -> normalized.equals(normalizePath(route.get("path"))))
                .findFirst();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpPublicationProjection.publication(this);
    }

    private List<String> values(String field) {
        return routes.stream()
                .map(route -> WayangA2uiDecodeValues.text(route.get(field)))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String normalizePath(Object value) {
        String path = WayangA2uiDecodeValues.text(value);
        int end = path.length();
        int queryStart = path.indexOf('?');
        if (queryStart >= 0) {
            end = Math.min(end, queryStart);
        }
        int fragmentStart = path.indexOf('#');
        if (fragmentStart >= 0) {
            end = Math.min(end, fragmentStart);
        }
        return WayangA2uiHttpRequest.normalizePath(path.substring(0, end));
    }
}
