package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpRouteProjection;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral registration handle for one mounted A2UI HTTP route.
 */
public record WayangA2uiHttpRouteBinding(
        WayangA2uiHttpRoute route,
        WayangA2uiHttpEndpointBinding endpoint) {

    public WayangA2uiHttpRouteBinding {
        route = Objects.requireNonNull(route, "route");
        endpoint = Objects.requireNonNull(endpoint, "endpoint");
    }

    public String operation() {
        return route.operation();
    }

    public String path() {
        return route.path();
    }

    public String httpMethod() {
        return route.method();
    }

    public List<String> allowedMethods() {
        return route.allowedMethods();
    }

    public String allow() {
        return route.allowHeader();
    }

    public String requestMediaType() {
        return route.requestContentType();
    }

    public String responseMediaType() {
        return route.responseContentType();
    }

    public boolean requestBodyRequired() {
        return route.requestBodyRequired();
    }

    public boolean path(WayangA2uiHttpRequest request) {
        return request != null && route.path(request);
    }

    public boolean matches(WayangA2uiHttpRequest request) {
        if (!path(request)) {
            return false;
        }
        return request.method("OPTIONS") || route.matches(request);
    }

    public WayangA2uiHttpResponse handle(WayangA2uiHttpRequest request) {
        WayangA2uiHttpRequest resolved = Objects.requireNonNull(request, "request");
        if (!path(resolved)) {
            return WayangA2uiHttpResponse.error(
                    404,
                    "a2ui_route_binding_mismatch",
                    "A2UI HTTP route binding " + route.path() + " cannot handle " + resolved.path() + ".");
        }
        return endpoint.adapter().handle(resolved);
    }

    public WayangA2uiHttpResponse handle(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return handle(method, rawPath, body, headers, Map.of());
    }

    public WayangA2uiHttpResponse handle(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return handle(endpoint.request(method, rawPath, body, headers, attributes));
    }

    public Map<String, Object> toMap() {
        return HttpRouteProjection.binding(this);
    }
}
