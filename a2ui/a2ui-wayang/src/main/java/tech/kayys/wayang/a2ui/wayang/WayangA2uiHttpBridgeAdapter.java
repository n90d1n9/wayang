package tech.kayys.wayang.a2ui.wayang;

import java.util.Objects;

/**
 * Dependency-free HTTP-shaped adapter over the A2UI bridge SPI.
 */
public final class WayangA2uiHttpBridgeAdapter {

    public static final String PATH_EXCHANGE = WayangA2uiHttpRoute.PATH_EXCHANGE;
    public static final String PATH_SURFACE_CATALOG = WayangA2uiHttpRoute.PATH_SURFACE_CATALOG;
    public static final String PATH_ROUTE_CATALOG = WayangA2uiHttpRoute.PATH_ROUTE_CATALOG;
    public static final String PATH_BINDING_REPORT = WayangA2uiHttpRoute.PATH_BINDING_REPORT;
    public static final String PATH_SMOKE = WayangA2uiHttpRoute.PATH_SMOKE;
    public static final String PATH_READINESS = WayangA2uiHttpRoute.PATH_READINESS;

    private static final WayangA2uiHttpRouteCatalog ROUTE_CATALOG = WayangA2uiHttpRouteCatalog.defaultCatalog();

    private final WayangA2uiHttpRouteCatalog routeCatalog;
    private final WayangA2uiHttpOperationDispatcher dispatcher;

    public WayangA2uiHttpBridgeAdapter(WayangA2uiBridge bridge) {
        this(bridge, WayangA2uiHttpRouteGuard.strict());
    }

    public WayangA2uiHttpBridgeAdapter(WayangA2uiBridge bridge, WayangA2uiHttpRouteGuard guard) {
        this(bridge, ROUTE_CATALOG, guard);
    }

    public WayangA2uiHttpBridgeAdapter(
            WayangA2uiBridge bridge,
            WayangA2uiHttpRouteCatalog routeCatalog,
            WayangA2uiHttpRouteGuard guard) {
        this(
                resolveCatalog(routeCatalog),
                WayangA2uiHttpOperationDispatcher.from(bridge, resolveCatalog(routeCatalog), guard));
    }

    public WayangA2uiHttpBridgeAdapter(
            WayangA2uiHttpRouteCatalog routeCatalog,
            WayangA2uiHttpOperationDispatcher dispatcher) {
        this.routeCatalog = resolveCatalog(routeCatalog);
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    public static WayangA2uiHttpBridgeAdapter from(WayangA2uiTransportAdapter adapter) {
        return new WayangA2uiHttpBridgeAdapter(WayangA2uiBridge.from(adapter));
    }

    public static WayangA2uiHttpRouteCatalog defaultRouteCatalog() {
        return ROUTE_CATALOG;
    }

    public WayangA2uiHttpRouteCatalog routeCatalog() {
        return routeCatalog;
    }

    public WayangA2uiHttpBindingReport bindingReport() {
        return dispatcher.bindingReport(routeCatalog);
    }

    public WayangA2uiHttpResponse handle(WayangA2uiHttpRequest request) {
        WayangA2uiHttpRequest resolved = Objects.requireNonNull(request, "request");
        if (resolved.method("OPTIONS")) {
            return handleOptions(resolved);
        }
        return routeCatalog.routeForPath(resolved.path())
                .map(route -> dispatcher.dispatch(resolved, route))
                .orElseGet(() -> notFound(resolved));
    }

    private WayangA2uiHttpResponse handleOptions(WayangA2uiHttpRequest request) {
        return routeCatalog.routeForPath(request.path())
                .map(route -> dispatcher.dispatchOptions(request, route))
                .orElseGet(() -> notFound(request));
    }

    private static WayangA2uiHttpResponse notFound(WayangA2uiHttpRequest request) {
        return WayangA2uiHttpResponse.error(
                404,
                "not_found",
                "Unknown A2UI HTTP route: " + request.method() + " " + request.path());
    }

    public WayangA2uiHttpResponse exchange(String requestEnvelopeJson) {
        return handle(WayangA2uiHttpRequest.exchange(requestEnvelopeJson));
    }

    public WayangA2uiHttpResponse surfaceCatalog() {
        return handle(WayangA2uiHttpRequest.surfaceCatalog());
    }

    public WayangA2uiHttpResponse routeCatalogResponse() {
        return handle(WayangA2uiHttpRequest.routeCatalog());
    }

    public WayangA2uiHttpResponse bindingReportResponse() {
        return handle(WayangA2uiHttpRequest.bindingReport());
    }

    public WayangA2uiHttpBindingReportProbeResult bindingReportProbe() {
        return WayangA2uiHttpBindingReportProbeResult.run(this);
    }

    public WayangA2uiHttpResponse smoke() {
        return handle(WayangA2uiHttpRequest.smoke());
    }

    public WayangA2uiHttpSmokeProbeResult smokeProbe() {
        return WayangA2uiHttpSmokeProbeResult.run(this);
    }

    public WayangA2uiHttpResponse readinessResponse() {
        return handle(WayangA2uiHttpRequest.readiness());
    }

    public WayangA2uiHttpReadinessProbeResult readinessProbe() {
        return WayangA2uiHttpReadinessProbeResult.run(this);
    }

    private static WayangA2uiHttpRouteCatalog resolveCatalog(WayangA2uiHttpRouteCatalog routeCatalog) {
        return routeCatalog == null ? ROUTE_CATALOG : routeCatalog;
    }
}
