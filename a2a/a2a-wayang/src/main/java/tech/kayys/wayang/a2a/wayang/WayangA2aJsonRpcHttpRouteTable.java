package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

record WayangA2aJsonRpcHttpRouteTable(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {

    WayangA2aJsonRpcHttpRouteTable {
        routes = routes == null
                ? List.of()
                : routes.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    static WayangA2aJsonRpcHttpRouteTable fromConfig(WayangA2aJsonRpcHttpConfig config) {
        return new WayangA2aJsonRpcHttpRouteTable(WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(config));
    }

    Optional<WayangA2aJsonRpcHttpRouteDescriptor> routeFor(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        return routes.stream()
                .filter(route -> route.matchesPath(resolved))
                .findFirst();
    }

    WayangA2aHttpResponse dispatch(
            WayangA2aHttpRequest request,
            BiFunction<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpRequest, WayangA2aHttpResponse> handler) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        BiFunction<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpRequest, WayangA2aHttpResponse> resolvedHandler =
                Objects.requireNonNull(handler, "handler");
        Optional<WayangA2aJsonRpcHttpRouteDescriptor> route = routeFor(resolved);
        if (route.isEmpty()) {
            return notFound(resolved);
        }
        WayangA2aJsonRpcHttpRouteDescriptor resolvedRoute = route.orElseThrow();
        return resolved.method("OPTIONS")
                ? resolvedRoute.optionsResponse()
                : Objects.requireNonNull(resolvedHandler.apply(resolvedRoute, resolved), "response");
    }

    private static WayangA2aHttpResponse notFound(WayangA2aHttpRequest request) {
        return WayangA2aJsonRpcHttpResponses.error(
                404,
                WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
                "",
                "jsonrpc_path_not_found",
                "No A2A JSON-RPC path matches " + request.path() + ".");
    }
}
