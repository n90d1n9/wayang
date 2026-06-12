package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiBridgeResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointBinding;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpEndpointProjectionTest {

    @Test
    void projectsOrderedEndpointRequestEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointRequest request = endpoint().project(
                "POST",
                "/api/a2ui/exchange?tenant=demo",
                "{}",
                Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.of("traceId", "trace-1"));

        Map<String, Object> values = HttpEndpointProjection.request(request);

        assertThat(request.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "method",
                "path",
                "body",
                "headers",
                "attributes",
                "knownPath",
                "matched",
                "operation",
                "allow",
                "requestBodyRequired",
                "route");
        assertThat(values)
                .containsEntry("method", "POST")
                .containsEntry("path", "/api/a2ui/exchange")
                .containsEntry("knownPath", true)
                .containsEntry("matched", true)
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry("requestBodyRequired", true);
    }

    @Test
    void omitsOptionalRouteFieldsForUnknownEndpointRequest() {
        WayangA2uiHttpEndpointRequest request = endpoint().project(
                "GET",
                "/api/a2ui/missing",
                "",
                Map.of());

        Map<String, Object> values = HttpEndpointProjection.request(request);

        assertThat(values.keySet()).containsExactly(
                "method",
                "path",
                "body",
                "headers",
                "attributes",
                "knownPath",
                "matched",
                "requestBodyRequired",
                "route");
        assertThat(values)
                .containsEntry("knownPath", false)
                .containsEntry("matched", false)
                .containsEntry("requestBodyRequired", false)
                .containsEntry("route", Map.of());
    }

    @Test
    void projectsOrderedEndpointResponseEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointResponse response = WayangA2uiHttpEndpointResponse.from(
                new WayangA2uiHttpResponse(
                        404,
                        WayangA2uiTransportContent.MIME_JSON,
                        "{}",
                        Map.of(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, "application/problem+json")));

        Map<String, Object> values = HttpEndpointProjection.response(response);

        assertThat(response.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "contentType",
                "body",
                "headerCount",
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 404)
                .containsEntry("contentType", WayangA2uiTransportContent.MIME_JSON)
                .containsEntry("body", "{}")
                .containsEntry("headerCount", 1);
        assertThat((Map<String, List<String>>) values.get("headers"))
                .containsEntry(WayangA2uiHttpResponse.HEADER_CONTENT_TYPE, List.of("application/problem+json"));
    }

    @Test
    void projectsOrderedEndpointExchangeEnvelopeAndRecordDelegates() {
        WayangA2uiHttpEndpointExchange exchange = endpoint().exchange(
                "GET",
                "/api/a2ui/route-catalog",
                "",
                Map.of(WayangA2uiHttpResponse.HEADER_ACCEPT, WayangA2uiTransportContent.MIME_JSON),
                Map.of("traceId", "trace-1"));

        Map<String, Object> values = HttpEndpointProjection.exchange(exchange);

        assertThat(exchange.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "knownPath",
                "matched",
                "statusCode",
                "successful",
                "outcome",
                "transportError",
                "request",
                "response",
                "responseEnvelope");
        assertThat(values)
                .containsEntry("knownPath", true)
                .containsEntry("matched", true)
                .containsEntry("statusCode", 200)
                .containsEntry("successful", true)
                .containsEntry("outcome", WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry("transportError", false);
        assertThat((Map<String, Object>) values.get("request"))
                .containsEntry("operation", WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG)
                .containsEntry("matched", true);
    }

    private static WayangA2uiHttpEndpointBinding endpoint() {
        return new WayangA2uiHttpEndpointBinding(
                request -> WayangA2uiBridgeResponse.of(WayangA2uiTransportResponse.error("unused", "Unused")),
                "/api/a2ui");
    }
}
