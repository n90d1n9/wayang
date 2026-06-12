package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catalog-backed publication plan for framework/runtime A2A JSON-RPC route registration.
 */
public final class WayangA2aJsonRpcHttpPublication {

    private final WayangA2aJsonRpcHttpAdapter adapter;
    private final WayangA2aJsonRpcHttpRouteCatalog routeCatalog;
    private final List<WayangA2aJsonRpcHttpRouteBinding> bindings;

    private WayangA2aJsonRpcHttpPublication(
            WayangA2aJsonRpcHttpAdapter adapter,
            WayangA2aJsonRpcHttpRouteCatalog routeCatalog) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.routeCatalog = Objects.requireNonNull(routeCatalog, "routeCatalog");
        this.bindings = this.routeCatalog.routes().stream()
                .filter(WayangA2aJsonRpcHttpRoute::enabled)
                .map(route -> new WayangA2aJsonRpcHttpRouteBinding(route, this.adapter))
                .toList();
    }

    public static WayangA2aJsonRpcHttpPublication from(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return new WayangA2aJsonRpcHttpPublication(resolved, resolved.routeCatalog());
    }

    public WayangA2aJsonRpcHttpAdapter adapter() {
        return adapter;
    }

    public WayangA2aJsonRpcHttpRouteCatalog routeCatalog() {
        return routeCatalog;
    }

    public List<WayangA2aJsonRpcHttpRouteBinding> bindings() {
        return bindings;
    }

    public int routeCount() {
        return routeCatalog.routeCount();
    }

    public int enabledRouteCount() {
        return routeCatalog.enabledRouteCount();
    }

    public int publishedRouteCount() {
        return bindings.size();
    }

    public List<String> operations() {
        return bindings.stream()
                .map(WayangA2aJsonRpcHttpRouteBinding::operation)
                .toList();
    }

    public Optional<WayangA2aJsonRpcHttpRouteBinding> bindingForOperation(String operation) {
        String normalized = operation == null ? "" : operation.trim();
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return bindings.stream()
                .filter(binding -> binding.operation().equals(normalized))
                .findFirst();
    }

    public Optional<WayangA2aJsonRpcHttpRouteBinding> bindingForPath(String path) {
        WayangA2aHttpRequest request = WayangA2aHttpRequest.get(path);
        return bindings.stream()
                .filter(binding -> binding.path(request))
                .findFirst();
    }

    public Optional<WayangA2aJsonRpcHttpRouteBinding> binding(String method, String path) {
        return binding(new WayangA2aHttpRequest(method, path, "", Map.of(), Map.of()));
    }

    public Optional<WayangA2aJsonRpcHttpRouteBinding> binding(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        return bindings.stream()
                .filter(binding -> binding.matches(resolved))
                .findFirst();
    }

    public WayangA2aHttpResponse dispatch(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        return bindingForPath(resolved.path())
                .map(binding -> binding.handle(resolved))
                .orElseGet(() -> WayangA2aHttpResponse.error(
                        404,
                        "jsonrpc_route_not_published",
                        "No published A2A JSON-RPC route matches " + resolved.path() + "."));
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpPublicationProjection.publication(this);
    }
}
