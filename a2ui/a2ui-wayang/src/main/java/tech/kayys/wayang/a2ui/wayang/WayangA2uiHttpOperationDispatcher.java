package tech.kayys.wayang.a2ui.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dispatches validated A2UI HTTP route operations to registered handlers.
 */
public final class WayangA2uiHttpOperationDispatcher {

    private final Map<String, WayangA2uiHttpOperationHandler> handlers;
    private final WayangA2uiHttpRouteGuard guard;

    public WayangA2uiHttpOperationDispatcher(Map<String, ? extends WayangA2uiHttpOperationHandler> handlers) {
        this(handlers, WayangA2uiHttpRouteGuard.strict());
    }

    public WayangA2uiHttpOperationDispatcher(
            Map<String, ? extends WayangA2uiHttpOperationHandler> handlers,
            WayangA2uiHttpRouteGuard guard) {
        this.handlers = copyHandlers(handlers);
        this.guard = Objects.requireNonNull(guard, "guard");
    }

    public static WayangA2uiHttpOperationDispatcher from(WayangA2uiBridge bridge) {
        return from(bridge, WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpOperationDispatcher from(
            WayangA2uiBridge bridge,
            WayangA2uiHttpRouteCatalog routeCatalog) {
        return from(bridge, routeCatalog, WayangA2uiHttpRouteGuard.strict());
    }

    public static WayangA2uiHttpOperationDispatcher from(
            WayangA2uiBridge bridge,
            WayangA2uiHttpRouteCatalog routeCatalog,
            WayangA2uiHttpRouteGuard guard) {
        WayangA2uiBridge resolvedBridge = Objects.requireNonNull(bridge, "bridge");
        WayangA2uiHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? WayangA2uiHttpRouteCatalog.defaultCatalog()
                : routeCatalog;
        WayangA2uiHttpRouteGuard resolvedGuard = guard == null ? WayangA2uiHttpRouteGuard.strict() : guard;
        Map<String, WayangA2uiHttpOperationHandler> handlers = new LinkedHashMap<>();
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                (request, route) -> resolvedBridge.exchangeEnvelopeJsonOrError(request.body()));
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_SURFACE_CATALOG,
                (request, route) -> resolvedBridge.exchange(WayangA2uiTransportRequest.surfaceCatalog()));
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG,
                (request, route) -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(resolvedCatalog)));
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_BINDING_REPORT,
                (request, route) -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(
                        WayangA2uiHttpBindingReport.of(
                                resolvedCatalog,
                                new WayangA2uiHttpOperationDispatcher(handlers, resolvedGuard)))));
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_SMOKE,
                (request, route) -> {
                    WayangA2uiHttpOperationDispatcher smokeDispatcher =
                            new WayangA2uiHttpOperationDispatcher(handlers, resolvedGuard);
                    WayangA2uiHttpBridgeAdapter smokeAdapter =
                            new WayangA2uiHttpBridgeAdapter(resolvedCatalog, smokeDispatcher);
                    WayangA2uiHttpSmokeResult result =
                            new WayangA2uiHttpSmokeRunner(WayangA2uiHttpHarness.of(smokeAdapter), resolvedCatalog)
                                    .run();
                    return WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(result));
                });
        handlers.put(
                WayangA2uiHttpRoute.OPERATION_READINESS,
                (request, route) -> {
                    WayangA2uiHttpOperationDispatcher readinessDispatcher =
                            new WayangA2uiHttpOperationDispatcher(handlers, resolvedGuard);
                    WayangA2uiHttpBridgeAdapter readinessAdapter =
                            new WayangA2uiHttpBridgeAdapter(resolvedCatalog, readinessDispatcher);
                    return WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(
                            readinessAdapter.readinessProbe()));
                });
        return new WayangA2uiHttpOperationDispatcher(handlers, resolvedGuard);
    }

    public List<String> operations() {
        return List.copyOf(handlers.keySet());
    }

    public boolean supports(WayangA2uiHttpRoute route) {
        return route != null && handlers.containsKey(route.operation());
    }

    public WayangA2uiHttpBindingReport bindingReport(WayangA2uiHttpRouteCatalog routeCatalog) {
        return WayangA2uiHttpBindingReport.of(routeCatalog, this);
    }

    public WayangA2uiHttpResponse dispatch(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        WayangA2uiHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2uiHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        return guard.validate(resolvedRequest, resolvedRoute)
                .orElseGet(() -> dispatchValidated(resolvedRequest, resolvedRoute));
    }

    public WayangA2uiHttpResponse dispatchOptions(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        WayangA2uiHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2uiHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        return guard.validateOptions(resolvedRequest, resolvedRoute)
                .orElseGet(() -> guard.routeResponse(
                        resolvedRoute,
                        WayangA2uiHttpResponse.fromBridge(WayangA2uiBridgeResponse.of(
                                WayangA2uiTransportResponse.from(
                                        new WayangA2uiHttpRouteCatalog(List.of(resolvedRoute)))))));
    }

    private WayangA2uiHttpResponse dispatchValidated(
            WayangA2uiHttpRequest request,
            WayangA2uiHttpRoute route) {
        WayangA2uiHttpOperationHandler handler = handlers.get(route.operation());
        if (handler == null) {
            return guard.routeResponse(route, WayangA2uiHttpResponse.error(
                    501,
                    "unsupported_route_operation",
                    "Unsupported A2UI HTTP route operation: " + route.operation()));
        }
        return guard.routeResponse(route, WayangA2uiHttpResponse.fromBridge(handler.handle(request, route)));
    }

    private static Map<String, WayangA2uiHttpOperationHandler> copyHandlers(
            Map<String, ? extends WayangA2uiHttpOperationHandler> handlers) {
        Map<String, WayangA2uiHttpOperationHandler> copy = new LinkedHashMap<>();
        if (handlers != null) {
            handlers.forEach((operation, handler) -> {
                if (handler != null) {
                    copy.put(normalizeOperation(operation), handler);
                }
            });
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String normalizeOperation(String operation) {
        String normalized = operation == null ? "" : operation.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("A2UI HTTP operation handler key must not be blank");
        }
        return normalized;
    }
}
