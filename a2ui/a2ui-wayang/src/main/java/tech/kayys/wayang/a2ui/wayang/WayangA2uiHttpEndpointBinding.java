package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpHeaderValues;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-free binding helper for concrete HTTP framework adapters.
 */
public final class WayangA2uiHttpEndpointBinding {

    private final WayangA2uiHttpBridgeAdapter adapter;

    public WayangA2uiHttpEndpointBinding(WayangA2uiBridge bridge) {
        this(bridge, WayangA2uiHttpRoute.PATH_ROOT);
    }

    public WayangA2uiHttpEndpointBinding(WayangA2uiBridge bridge, String mountRoot) {
        this(bridge, mountRoot, WayangA2uiHttpRouteGuard.strict());
    }

    public WayangA2uiHttpEndpointBinding(
            WayangA2uiBridge bridge,
            String mountRoot,
            WayangA2uiHttpRouteGuard guard) {
        this(new WayangA2uiHttpBridgeAdapter(
                Objects.requireNonNull(bridge, "bridge"),
                WayangA2uiHttpRouteCatalog.defaultCatalog().mountedAt(mountRoot),
                guard == null ? WayangA2uiHttpRouteGuard.strict() : guard));
    }

    public WayangA2uiHttpEndpointBinding(WayangA2uiHttpBridgeAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public static WayangA2uiHttpEndpointBinding from(WayangA2uiTransportAdapter adapter) {
        return from(adapter, WayangA2uiHttpRoute.PATH_ROOT);
    }

    public static WayangA2uiHttpEndpointBinding from(
            WayangA2uiTransportAdapter adapter,
            String mountRoot) {
        return new WayangA2uiHttpEndpointBinding(WayangA2uiBridge.from(
                Objects.requireNonNull(adapter, "adapter")), mountRoot);
    }

    public WayangA2uiHttpBridgeAdapter adapter() {
        return adapter;
    }

    public WayangA2uiHttpRouteCatalog routeCatalog() {
        return adapter.routeCatalog();
    }

    public List<WayangA2uiHttpRoute> routes() {
        return routeCatalog().routes();
    }

    public List<WayangA2uiHttpRouteBinding> bindings() {
        return routes().stream()
                .map(route -> new WayangA2uiHttpRouteBinding(route, this))
                .toList();
    }

    public WayangA2uiHttpEndpointPublication publication() {
        return WayangA2uiHttpEndpointPublication.from(this);
    }

    public Optional<WayangA2uiHttpRoute> route(String method, String rawPath) {
        WayangA2uiHttpRequest request = request(method, rawPath, "", Map.of(), Map.of());
        return request.method("OPTIONS")
                ? routeCatalog().routeForPath(request.path())
                : routeCatalog().route(request.method(), request.path());
    }

    public Optional<WayangA2uiHttpRouteBinding> binding(String method, String rawPath) {
        WayangA2uiHttpRequest request = request(method, rawPath, "", Map.of(), Map.of());
        return routeCatalog().routeForPath(request.path())
                .map(route -> new WayangA2uiHttpRouteBinding(route, this))
                .filter(binding -> binding.matches(request));
    }

    public boolean canHandle(String method, String rawPath) {
        return binding(method, rawPath).isPresent();
    }

    public WayangA2uiHttpResponse handle(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return handle(method, rawPath, body, headers, Map.of());
    }

    public WayangA2uiHttpEndpointResponse respond(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return respond(method, rawPath, body, headers, Map.of());
    }

    public WayangA2uiHttpEndpointExchange exchange(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return exchange(method, rawPath, body, headers, Map.of());
    }

    public WayangA2uiHttpEndpointRequest project(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return project(method, rawPath, body, headers, Map.of());
    }

    public WayangA2uiHttpResponse handle(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return adapter.handle(request(method, rawPath, body, headers, attributes));
    }

    public WayangA2uiHttpEndpointResponse respond(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return WayangA2uiHttpEndpointResponse.from(handle(method, rawPath, body, headers, attributes));
    }

    public WayangA2uiHttpEndpointExchange exchange(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return WayangA2uiHttpEndpointExchange.from(this, method, rawPath, body, headers, attributes);
    }

    public WayangA2uiHttpEndpointRequest project(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return WayangA2uiHttpEndpointRequest.from(this, method, rawPath, body, headers, attributes);
    }

    public WayangA2uiHttpRequest request(
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        return new WayangA2uiHttpRequest(
                method,
                pathWithoutQuery(rawPath),
                body,
                normalizeHeaders(headers),
                TransportMaps.copy(attributes));
    }

    private static String pathWithoutQuery(String rawPath) {
        String path = rawPath == null ? "" : rawPath.trim();
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

    private static Map<String, Object> normalizeHeaders(Map<?, ?> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (name != null) {
                String headerValue = HttpHeaderValues.joined(value);
                if (!headerValue.isBlank()) {
                    normalized.put(String.valueOf(name), headerValue);
                }
            }
        });
        return TransportMaps.freeze(normalized);
    }
}
