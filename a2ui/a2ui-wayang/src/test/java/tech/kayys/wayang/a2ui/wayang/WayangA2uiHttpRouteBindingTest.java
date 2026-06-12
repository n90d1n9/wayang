package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpRouteBindingTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void exposesMountedRouteRegistrationMetadata() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();
        WayangA2uiHttpRouteBinding exchange = endpoint.binding("POST", "/api/a2ui/exchange")
                .orElseThrow();

        assertThat(endpoint.bindings()).hasSize(6);
        assertThat(exchange.operation()).isEqualTo(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(exchange.path()).isEqualTo("/api/a2ui/exchange");
        assertThat(exchange.httpMethod()).isEqualTo("POST");
        assertThat(exchange.allowedMethods()).containsExactly("POST", "OPTIONS");
        assertThat(exchange.allow()).isEqualTo("POST, OPTIONS");
        assertThat(exchange.requestMediaType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(exchange.responseMediaType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(exchange.requestBodyRequired()).isTrue();
        assertThat(exchange.toMap())
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("published", true)
                .containsEntry("allowHeader", "POST, OPTIONS")
                .satisfies(values -> assertThat((Iterable<String>) values.get("allowedMethods"))
                        .containsExactly("POST", "OPTIONS"));
    }

    @Test
    void matchesOptionsAndExactRouteMethodOnly() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();

        assertThat(endpoint.binding("OPTIONS", "/api/a2ui/exchange?cors=true"))
                .contains(endpoint.binding("POST", "/api/a2ui/exchange").orElseThrow());
        assertThat(endpoint.binding("GET", "/api/a2ui/exchange")).isEmpty();
        assertThat(endpoint.canHandle("GET", "/api/a2ui/route-catalog")).isTrue();
        assertThat(endpoint.canHandle("POST", "/api/a2ui/route-catalog")).isFalse();
    }

    @Test
    void dispatchesThroughMountedRouteBinding() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpEndpointBinding endpoint = WayangA2uiHttpEndpointBinding.from(
                new WayangA2uiTransportAdapter(sdk),
                "/api/a2ui");
        WayangA2uiHttpRouteBinding exchange = endpoint.binding("POST", "/api/a2ui/exchange")
                .orElseThrow();
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));

        WayangA2uiHttpResponse response = exchange.handle(
                "POST",
                "/api/a2ui/exchange?tenant=demo",
                request.toJson(),
                Map.of(
                        WayangA2uiHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2uiTransportContent.MIME_JSON,
                        WayangA2uiHttpResponse.HEADER_ACCEPT,
                        List.of(WayangA2uiTransportContent.MIME_JSON)));
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transport.handledCount()).isEqualTo(1L);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void rejectsRequestsForOtherPaths() {
        WayangA2uiHttpEndpointBinding endpoint = endpoint();
        WayangA2uiHttpRouteBinding exchange = endpoint.binding("POST", "/api/a2ui/exchange")
                .orElseThrow();

        WayangA2uiHttpResponse response = exchange.handle(endpoint.request(
                "GET",
                "/api/a2ui/route-catalog",
                "",
                Map.of(),
                Map.of()));
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(transport.transportError())
                .contains(WayangA2uiTransportError.of(
                        "a2ui_route_binding_mismatch",
                        "A2UI HTTP route binding /api/a2ui/exchange cannot handle /api/a2ui/route-catalog."));
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }

    private static A2uiUserAction action(String name) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                Map.of("runId", "run-1"));
    }
}
