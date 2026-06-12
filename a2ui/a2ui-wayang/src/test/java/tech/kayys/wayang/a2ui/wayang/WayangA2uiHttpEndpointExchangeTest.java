package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointExchangeTest {

    @Test
    void capturesMatchedEndpointExchangeForDiagnostics() {
        WayangA2uiHttpEndpointExchange exchange = endpoint().exchange(
                "get",
                "/api/a2ui/route-catalog?tenant=demo",
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, List.of(WayangA2uiTransportContent.MIME_JSON)),
                Map.of("traceId", "trace-1"));

        assertThat(exchange.knownPath()).isTrue();
        assertThat(exchange.matched()).isTrue();
        assertThat(exchange.statusCode()).isEqualTo(200);
        assertThat(exchange.successful()).isTrue();
        assertThat(exchange.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS);
        assertThat(exchange.transportError()).isFalse();
        assertThat(exchange.request().operation()).contains(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat(exchange.request().attributes()).containsEntry("traceId", "trace-1");
        assertThat(exchange.response().firstHeader(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION))
                .contains(WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        assertThat(exchange.responseEnvelope())
                .containsEntry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name());
        assertThat((Map<String, Object>) exchange.responseEnvelope().get(WayangA2uiTransportFields.METADATA))
                .containsEntry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_ROUTE_CATALOG);
        assertThat((Map<String, Object>) exchange.toMap().get("request"))
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry("matched", true);
    }

    @Test
    void capturesKnownPathMethodMismatch() {
        WayangA2uiHttpEndpointExchange exchange = endpoint().exchange(
                "GET",
                "/api/a2ui/exchange",
                "",
                Map.of());

        assertThat(exchange.knownPath()).isTrue();
        assertThat(exchange.matched()).isFalse();
        assertThat(exchange.statusCode()).isEqualTo(405);
        assertThat(exchange.successful()).isFalse();
        assertThat(exchange.outcome()).isEqualTo(WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(exchange.transportError()).isTrue();
        assertThat(exchange.request().operation()).contains(WayangA2uiHttpRoute.OPERATION_EXCHANGE);
        assertThat(exchange.request().allow()).contains("POST, OPTIONS");
        assertThat(exchange.response().firstHeader(WayangA2uiHttpResponse.HEADER_ALLOW))
                .contains("POST, OPTIONS");
        assertThat(exchange.transportResponse().transportError().orElseThrow().code())
                .isEqualTo("method_not_allowed");
        assertThat(exchange.toMap())
                .containsEntry("knownPath", true)
                .containsEntry("matched", false)
                .containsEntry("statusCode", 405)
                .containsEntry("transportError", true);
    }

    @Test
    void capturesUnknownPathWithoutRouteProjection() {
        WayangA2uiHttpEndpointExchange exchange = endpoint().exchange(
                "GET",
                "/api/a2ui/missing",
                "",
                Map.of());

        assertThat(exchange.knownPath()).isFalse();
        assertThat(exchange.matched()).isFalse();
        assertThat(exchange.statusCode()).isEqualTo(404);
        assertThat(exchange.successful()).isFalse();
        assertThat(exchange.outcome()).isEqualTo(WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(exchange.transportError()).isTrue();
        assertThat(exchange.request().operation()).isEmpty();
        assertThat(exchange.request().route()).isEmpty();
        assertThat(exchange.transportResponse().transportError().orElseThrow().code())
                .isEqualTo("not_found");
        assertThat((Map<String, Object>) exchange.toMap().get("request"))
                .containsEntry("knownPath", false)
                .containsEntry("matched", false)
                .containsEntry("route", Map.of());
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
