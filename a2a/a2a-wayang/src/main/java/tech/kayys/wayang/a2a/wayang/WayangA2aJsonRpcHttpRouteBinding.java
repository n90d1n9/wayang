package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral registration handle for one enabled A2A JSON-RPC HTTP route.
 */
public record WayangA2aJsonRpcHttpRouteBinding(
        WayangA2aJsonRpcHttpRoute route,
        WayangA2aJsonRpcHttpAdapter adapter) {

    public WayangA2aJsonRpcHttpRouteBinding {
        route = Objects.requireNonNull(route, "route");
        adapter = Objects.requireNonNull(adapter, "adapter");
        if (!route.enabled()) {
            throw new IllegalArgumentException("A2A JSON-RPC route binding requires an enabled route");
        }
    }

    public String operation() {
        return route.operation();
    }

    public String path() {
        return route.path();
    }

    public String httpMethod() {
        return route.httpMethod();
    }

    public List<String> allowedMethods() {
        return route.allowedMethods();
    }

    public String allow() {
        return route.allow();
    }

    public String requestMediaType() {
        return route.requestMediaType();
    }

    public List<String> responseMediaTypes() {
        return route.responseMediaTypes();
    }

    public boolean requestBodyRequired() {
        return route.requestBodyRequired();
    }

    public boolean path(WayangA2aHttpRequest request) {
        return request != null && route.path().equals(request.path());
    }

    public boolean matches(WayangA2aHttpRequest request) {
        if (!path(request)) {
            return false;
        }
        return request.method("OPTIONS") || route.matches(request);
    }

    public WayangA2aHttpResponse handle(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        if (!path(resolved)) {
            return WayangA2aHttpResponse.error(
                    404,
                    "jsonrpc_route_binding_mismatch",
                    "A2A JSON-RPC route binding " + route.path() + " cannot handle " + resolved.path() + ".");
        }
        return adapter.dispatch(resolved);
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpPublicationProjection.binding(this);
    }
}
