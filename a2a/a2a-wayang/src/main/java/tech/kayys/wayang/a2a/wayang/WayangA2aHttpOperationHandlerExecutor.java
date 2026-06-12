package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes registered HTTP operation handlers and normalizes handler failures.
 */
final class WayangA2aHttpOperationHandlerExecutor {

    private final Map<String, WayangA2aHttpOperationHandler> handlers;
    private final WayangA2aHttpRouteGuard guard;

    private WayangA2aHttpOperationHandlerExecutor(
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers,
            WayangA2aHttpRouteGuard guard) {
        this.handlers = WayangA2aHttpHandlerMaps.copyStrict(handlers);
        this.guard = guard == null ? WayangA2aHttpRouteGuard.strict() : guard;
    }

    static WayangA2aHttpOperationHandlerExecutor fromHandlers(
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers,
            WayangA2aHttpRouteGuard guard) {
        return new WayangA2aHttpOperationHandlerExecutor(handlers, guard);
    }

    List<String> operations() {
        return List.copyOf(handlers.keySet());
    }

    boolean supports(String operation) {
        return handlers.containsKey(operation);
    }

    WayangA2aHttpResponse execute(
            WayangA2aHttpRequest request,
            WayangA2aHttpRouteMatch match) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2aHttpRouteMatch resolvedMatch = Objects.requireNonNull(match, "match");
        A2aHttpRoute route = resolvedMatch.route();
        WayangA2aHttpOperationHandler handler = handlers.get(route.operation());
        if (handler == null) {
            return routeResponse(route, WayangA2aHttpResponse.error(
                    501,
                    "unsupported_route_operation",
                    "Unsupported A2A HTTP route operation: " + route.operation()));
        }
        try {
            return routeResponse(
                    route,
                    Objects.requireNonNull(handler.handle(resolvedRequest, resolvedMatch), "handler response"));
        } catch (WayangA2aTaskLifecycleException e) {
            return routeResponse(route, WayangA2aHttpResponse.error(
                    400,
                    "unsupported_operation",
                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return routeResponse(route, WayangA2aHttpResponse.error(
                    400,
                    "invalid_request",
                    e.getMessage() == null ? "Invalid A2A HTTP request." : e.getMessage()));
        }
    }

    private WayangA2aHttpResponse routeResponse(A2aHttpRoute route, WayangA2aHttpResponse response) {
        return guard.routeResponse(route, response);
    }
}
