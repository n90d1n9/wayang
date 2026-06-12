package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpPublicationProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data-only publication manifest for mounted A2UI HTTP endpoint bindings.
 */
public record WayangA2uiHttpEndpointPublication(List<Map<String, Object>> routes) {

    public WayangA2uiHttpEndpointPublication {
        routes = TransportMaps.copyMaps(routes);
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
        String normalized = DecodeValues.text(operation);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return routes.stream()
                .filter(route -> normalized.equals(DecodeValues.text(route.get("operation"))))
                .findFirst();
    }

    public Optional<Map<String, Object>> routeForPath(String path) {
        String normalized = normalizePath(path);
        return routes.stream()
                .filter(route -> normalized.equals(normalizePath(route.get("path"))))
                .findFirst();
    }

    public Map<String, Object> toMap() {
        return HttpPublicationProjection.publication(this);
    }

    private List<String> values(String field) {
        return routes.stream()
                .map(route -> DecodeValues.text(route.get(field)))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String normalizePath(Object value) {
        String path = DecodeValues.text(value);
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
