package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-friendly projection of a raw inbound A2UI HTTP request.
 */
public record WayangA2uiHttpEndpointRequest(
        WayangA2uiHttpRequest request,
        Map<String, Object> route,
        boolean knownPath,
        boolean matched) {

    public WayangA2uiHttpEndpointRequest {
        request = Objects.requireNonNull(request, "request");
        route = TransportMaps.copy(route);
        knownPath = knownPath || !route.isEmpty();
        matched = matched && knownPath;
    }

    public static WayangA2uiHttpEndpointRequest from(
            WayangA2uiHttpEndpointBinding endpoint,
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return from(endpoint, method, rawPath, body, headers, Map.of());
    }

    public static WayangA2uiHttpEndpointRequest from(
            WayangA2uiHttpEndpointBinding endpoint,
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        WayangA2uiHttpEndpointBinding resolved = Objects.requireNonNull(endpoint, "endpoint");
        WayangA2uiHttpRequest request = resolved.request(method, rawPath, body, headers, attributes);
        Optional<WayangA2uiHttpRoute> route = resolved.routeCatalog().routeForPath(request.path());
        boolean matched = route
                .map(matchedRoute -> new WayangA2uiHttpRouteBinding(matchedRoute, resolved).matches(request))
                .orElse(false);
        return new WayangA2uiHttpEndpointRequest(
                request,
                route.map(WayangA2uiHttpRoute::toMap).orElse(Map.of()),
                route.isPresent(),
                matched);
    }

    public String method() {
        return request.method();
    }

    public String path() {
        return request.path();
    }

    public String body() {
        return request.body();
    }

    public Map<String, Object> headers() {
        return request.headers();
    }

    public Map<String, Object> attributes() {
        return request.attributes();
    }

    public Optional<String> operation() {
        return routeValue("operation");
    }

    public Optional<String> allow() {
        return routeValue("allowHeader");
    }

    public boolean requestBodyRequired() {
        Object value = route.get("requestBodyRequired");
        return value instanceof Boolean bool && bool;
    }

    public Map<String, Object> toMap() {
        return HttpEndpointProjection.request(this);
    }

    private Optional<String> routeValue(String key) {
        Object value = route.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value).trim());
    }
}
