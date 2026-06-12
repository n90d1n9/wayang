package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manifest of configured A2A JSON-RPC HTTP routes for framework bindings.
 */
public record WayangA2aJsonRpcHttpRouteCatalog(List<WayangA2aJsonRpcHttpRoute> routes) {

    public static final String OPERATION_JSON_RPC_ROUTE_CATALOG = "JsonRpcRouteCatalog";

    public WayangA2aJsonRpcHttpRouteCatalog {
        routes = routes == null
                ? List.of()
                : routes.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    public static WayangA2aJsonRpcHttpRouteCatalog from(WayangA2aJsonRpcHttpAdapter adapter) {
        return fromConfig(Objects.requireNonNull(adapter, "adapter").config());
    }

    public static WayangA2aJsonRpcHttpRouteCatalog fromConfig(WayangA2aJsonRpcHttpConfig config) {
        WayangA2aJsonRpcHttpConfig resolved = Objects.requireNonNull(config, "config");
        return new WayangA2aJsonRpcHttpRouteCatalog(WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(resolved)
                .stream()
                .map(WayangA2aJsonRpcHttpRouteDescriptor::toRoute)
                .toList());
    }

    public int routeCount() {
        return routes.size();
    }

    public int enabledRouteCount() {
        return (int) routes.stream()
                .filter(WayangA2aJsonRpcHttpRoute::enabled)
                .count();
    }

    public Optional<WayangA2aJsonRpcHttpRoute> routeForOperation(String operation) {
        return routes.stream()
                .filter(route -> route.operation(operation))
                .findFirst();
    }

    public Optional<WayangA2aJsonRpcHttpRoute> routeForPath(String path) {
        WayangA2aHttpRequest request = WayangA2aHttpRequest.get(path);
        return routes.stream()
                .filter(route -> request.path().equals(route.path()))
                .findFirst();
    }

    public Optional<WayangA2aJsonRpcHttpRoute> route(String method, String path) {
        WayangA2aHttpRequest request = WayangA2aJsonRpcHttpRequests.routeProbe(method, path);
        return routes.stream()
                .filter(route -> route.matches(request))
                .findFirst();
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcHttpRouteCatalogProjection.response(this);
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpRouteCatalogProjection.catalog(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

}
