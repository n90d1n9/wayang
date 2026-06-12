package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcHttpResponsesTest {

    @Test
    void jsonRpcResponseCarriesJsonRpcEnvelopeWithoutRouteHeaders() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.jsonRpc(
                WayangA2aJsonRpcResponse.error(
                        "missing-method",
                        WayangA2aJsonRpcError.methodNotFound("MissingMethod")));
        String bodyJson = response.body();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers()).containsExactlyEntriesOf(Map.of(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON));
        assertThat(bodyJson).startsWith("{\"jsonrpc\":");
        assertThat(bodyJson.indexOf("\"id\"")).isGreaterThan(bodyJson.indexOf("\"jsonrpc\""));
        assertThat(bodyJson.indexOf("\"error\"")).isGreaterThan(bodyJson.indexOf("\"id\""));
        assertThat(bodyJson.indexOf("\"message\"")).isGreaterThan(bodyJson.indexOf("\"code\""));
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("jsonrpc", WayangA2aJsonRpcRequest.VERSION)
                .containsEntry("id", "missing-method");
        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.METHOD_NOT_FOUND);
    }

    @Test
    void jsonRpcResultResponseCarriesResultEnvelope() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                "result-1",
                Map.of("passed", true));
        String bodyJson = response.body();

        Map<String, Object> body = WayangA2aHttpJson.read(response.body());

        assertThat(bodyJson).startsWith("{\"jsonrpc\":");
        assertThat(bodyJson.indexOf("\"id\"")).isGreaterThan(bodyJson.indexOf("\"jsonrpc\""));
        assertThat(bodyJson.indexOf("\"result\"")).isGreaterThan(bodyJson.indexOf("\"id\""));
        assertThat(body)
                .containsEntry("jsonrpc", WayangA2aJsonRpcRequest.VERSION)
                .containsEntry("id", "result-1");
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) body.get("result")))
                .containsEntry("passed", true);
    }

    @Test
    void eventStreamResponseCarriesEventStreamContentType() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.eventStream("data: {}\n\n");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(response.body()).isEqualTo("data: {}\n\n");
        assertThat(response.headers()).containsExactlyEntriesOf(Map.of(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                A2aProtocol.EVENT_STREAM_MEDIA_TYPE));
    }

    @Test
    void jsonResponseCarriesJsonContentTypeAndProtocolHeaders() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.json(
                "JsonRpcDiagnostics",
                "{\"passed\":true}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.body()).isEqualTo("{\"passed\":true}");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpcDiagnostics")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION);
    }

    @Test
    void jsonHeadersCanCarryAllowAndProtocolOverride() {
        Map<String, Object> headers = WayangA2aJsonRpcHttpResponses.headersWithProtocolVersion(
                "JsonRpcReadiness",
                "1.0",
                "GET, OPTIONS");

        assertThat(headers)
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpcReadiness")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, "1.0")
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_VERSION, "1.0")
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(headers.keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_ALLOW);
    }

    @Test
    void routeHeadersOmitContentTypeForDecoratingExistingResponses() {
        Map<String, Object> headers = WayangA2aJsonRpcHttpResponses.routeHeaders(
                "JsonRpcSmoke",
                "GET, OPTIONS");

        assertThat(headers)
                .doesNotContainKey(WayangA2aHttpResponse.HEADER_CONTENT_TYPE)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpcSmoke")
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(headers.keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_ALLOW);
    }

    @Test
    void errorResponseCarriesJsonErrorEnvelopeAndRouteHeaders() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.error(
                405,
                "JsonRpcReadiness",
                "GET, OPTIONS",
                "method_not_allowed",
                "A2A JSON-RPC readiness path /ready requires GET.");
        String bodyJson = response.body();

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpcReadiness")
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS");
        assertThat(bodyJson).startsWith("{\"error\":");
        assertThat(bodyJson.indexOf("\"message\"")).isGreaterThan(bodyJson.indexOf("\"code\""));
        assertThat(error(response))
                .containsEntry("code", "method_not_allowed")
                .containsEntry("message", "A2A JSON-RPC readiness path /ready requires GET.");
    }

    @Test
    void errorResponseRejectsBlankErrorFields() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpResponses.error(
                400,
                "JsonRpc",
                "POST, OPTIONS",
                "",
                "missing code"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code must not be blank");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpResponses.error(
                400,
                "JsonRpc",
                "POST, OPTIONS",
                "missing_message",
                ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message must not be blank");
    }

    @Test
    void jsonRpcErrorResponseCarriesJsonRpcEnvelopeAndRouteHeaders() {
        WayangA2aHttpResponse response = WayangA2aJsonRpcHttpResponses.jsonRpcError(
                400,
                "JsonRpc",
                "POST, OPTIONS",
                "bad-version",
                WayangA2aJsonRpcError.versionNotSupported("0.5"));
        String bodyJson = response.body();

        Map<String, Object> body = WayangA2aHttpJson.read(response.body());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpc")
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(body)
                .containsEntry("jsonrpc", WayangA2aJsonRpcRequest.VERSION)
                .containsEntry("id", "bad-version");
        assertThat(bodyJson).startsWith("{\"jsonrpc\":");
        assertThat(bodyJson.indexOf("\"id\"")).isGreaterThan(bodyJson.indexOf("\"jsonrpc\""));
        assertThat(bodyJson.indexOf("\"error\"")).isGreaterThan(bodyJson.indexOf("\"id\""));
        assertThat(bodyJson.indexOf("\"message\"")).isGreaterThan(bodyJson.indexOf("\"code\""));
        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.VERSION_NOT_SUPPORTED)
                .containsEntry("message", "A2A protocol version is not supported: 0.5");
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
