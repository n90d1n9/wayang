package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointBindingTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void exposesMountedRoutesForFrameworkDispatch() {
        WayangA2uiHttpEndpointBinding binding = new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");

        assertThat(binding.routeCatalog().routeCount()).isEqualTo(6);
        assertThat(binding.canHandle("GET", "/api/a2ui/route-catalog?tenant=demo")).isTrue();
        assertThat(binding.canHandle("GET", WayangA2uiHttpRoute.PATH_ROUTE_CATALOG)).isFalse();
        assertThat(binding.route("OPTIONS", "/api/a2ui/exchange?cors=true"))
                .contains(WayangA2uiHttpRoute.exchange().withPath("/api/a2ui/exchange"));

        WayangA2uiHttpResponse response = binding.handle(
                "get",
                "/api/a2ui/route-catalog?tenant=demo",
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, List.of(WayangA2uiTransportContent.MIME_JSON)));
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat(transport.body()).contains("\"path\":\"/api/a2ui/exchange\"");
    }

    @Test
    void convertsRawFrameworkRequestsAndMultiValueHeadersForExchange() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpEndpointBinding binding = WayangA2uiHttpEndpointBinding.from(
                new WayangA2uiTransportAdapter(sdk),
                "/api/a2ui");
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT)));
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, List.of(" application/json; charset=utf-8 "));
        headers.put(WayangA2uiHttpResponse.HEADER_ACCEPT, new String[] {
                "text/plain;q=0",
                WayangA2uiTransportContent.MIME_JSON
        });

        WayangA2uiHttpResponse response = binding.handle(
                "post",
                "/api/a2ui/exchange?tenant=demo#ignored",
                request.toJson(),
                headers,
                Map.of("traceId", "trace-1"));
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.successful()).isTrue();
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transport.handledCount()).isEqualTo(1L);
        assertThat(transport.rejectedCount()).isZero();
        assertThat(transport.metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND,
                        WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void handlesOptionsThroughMountedEndpoint() {
        WayangA2uiHttpEndpointBinding binding = new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");

        WayangA2uiHttpResponse response = binding.handle(
                "OPTIONS",
                "/api/a2ui/exchange?cors=true",
                "",
                Map.of());
        WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(transport.metadata()).containsEntry(WayangA2uiTransportFields.ROUTE_COUNT, 1);
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
