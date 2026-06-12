package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpOperationDispatcherTest {

    @Test
    void dispatchesRegisteredOperationsAndDecoratesResponses() {
        WayangA2uiHttpRoute route = demoRoute();
        WayangA2uiHttpOperationDispatcher dispatcher = new WayangA2uiHttpOperationDispatcher(Map.of(
                " demo.operation ",
                (request, matchedRoute) -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.from(
                        new WayangA2uiHttpRouteCatalog(List.of(matchedRoute))))));

        WayangA2uiHttpResponse response = dispatcher.dispatch(WayangA2uiHttpRequest.get("/demo"), route);

        assertThat(dispatcher.operations()).containsExactly("demo.operation");
        assertThat(dispatcher.supports(route)).isTrue();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION, "demo.operation")
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).metadata())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG)
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 1);
    }

    @Test
    void returnsRouteProblemForUnsupportedOperations() {
        WayangA2uiHttpRoute route = demoRoute();
        WayangA2uiHttpOperationDispatcher dispatcher = new WayangA2uiHttpOperationDispatcher(Map.of());

        WayangA2uiHttpResponse response = dispatcher.dispatch(WayangA2uiHttpRequest.get("/demo"), route);

        assertThat(dispatcher.supports(route)).isFalse();
        assertThat(response.statusCode()).isEqualTo(501);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION, "demo.operation")
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).transportError())
                .contains(WayangA2uiTransportError.of(
                        "unsupported_route_operation",
                        "Unsupported A2UI HTTP route operation: demo.operation"));
    }

    @Test
    void servesOptionsWithoutInvokingOperationHandlers() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        WayangA2uiHttpRoute route = demoRoute();
        WayangA2uiHttpOperationDispatcher dispatcher = new WayangA2uiHttpOperationDispatcher(Map.of(
                "demo.operation",
                (request, matchedRoute) -> {
                    invoked.set(true);
                    return WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unexpected", "Unexpected"));
                }));

        WayangA2uiHttpResponse response = dispatcher.dispatchOptions(new WayangA2uiHttpRequest(
                "OPTIONS",
                "/demo",
                "",
                Map.of(),
                Map.of()), route);

        assertThat(invoked).isFalse();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(WayangA2uiTransportResponse.fromJson(response.body()).metadata())
                .containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 1);
    }

    @Test
    void reportsRouteAndHandlerBindingCoverage() {
        WayangA2uiHttpRoute route = demoRoute();
        WayangA2uiHttpRouteCatalog catalog = new WayangA2uiHttpRouteCatalog(List.of(
                WayangA2uiHttpRoute.exchange(),
                route));
        WayangA2uiHttpOperationDispatcher dispatcher = new WayangA2uiHttpOperationDispatcher(Map.of(
                "demo.operation",
                (request, matchedRoute) -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error(
                        "demo",
                        "Demo")),
                "orphan.operation",
                (request, matchedRoute) -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error(
                        "orphan",
                        "Orphan"))));

        WayangA2uiHttpBindingReport report = dispatcher.bindingReport(catalog);

        assertThat(report.complete()).isFalse();
        assertThat(report.routeOperations()).containsExactly(
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                "demo.operation");
        assertThat(report.handlerOperations()).containsExactly("demo.operation", "orphan.operation");
        assertThat(report.missingHandlerOperations()).containsExactly(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(report.orphanHandlerOperations()).containsExactly("orphan.operation");
        assertThat(report.toMap())
                .containsEntry("complete", false)
                .containsEntry("routeOperationCount", 2)
                .containsEntry("handlerOperationCount", 2);
    }

    @Test
    void reportsDefaultWayangBindingsAsComplete() {
        WayangA2uiHttpOperationDispatcher dispatcher = WayangA2uiHttpOperationDispatcher.from(request -> {
            throw new UnsupportedOperationException("not used");
        });

        WayangA2uiHttpBindingReport report = dispatcher.bindingReport(WayangA2uiHttpRouteCatalog.defaultCatalog());

        assertThat(report.complete()).isTrue();
        assertThat(report.missingHandlerOperations()).isEmpty();
        assertThat(report.orphanHandlerOperations()).isEmpty();
        assertThat(report.routeOperationCount()).isEqualTo(6);
        assertThat(report.handlerOperationCount()).isEqualTo(6);
    }

    private static WayangA2uiHttpRoute demoRoute() {
        return new WayangA2uiHttpRoute(
                "demo.operation",
                "GET",
                "/demo",
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }
}
