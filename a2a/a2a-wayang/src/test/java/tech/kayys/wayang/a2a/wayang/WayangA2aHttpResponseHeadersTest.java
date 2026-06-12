package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpResponseHeadersTest {

    @Test
    void projectsRouteOperationAndAllowHeaders() {
        WayangA2aHttpResponse response = new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "{}",
                Map.of(
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION, "JsonRpcSmoke",
                        WayangA2aHttpResponse.HEADER_ALLOW, "GET, OPTIONS"));

        assertThat(WayangA2aHttpResponseHeaders.routeOperationHeader(response)).isEqualTo("JsonRpcSmoke");
        assertThat(WayangA2aHttpResponseHeaders.allowHeader(response)).isEqualTo("GET, OPTIONS");
        assertThat(WayangA2aHttpResponseHeaders.header(response, "missing")).isEmpty();
    }

    @Test
    void prefersProtocolVersionHeaderAndFallsBackToA2aVersion() {
        WayangA2aHttpResponse both = responseWithHeaders(Map.of(
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, "1.0",
                WayangA2aHttpResponse.HEADER_A2A_VERSION, "0.9"));
        WayangA2aHttpResponse a2aOnly = responseWithHeaders(Map.of(
                WayangA2aHttpResponse.HEADER_A2A_VERSION, A2aProtocol.VERSION));
        WayangA2aHttpResponse missing = responseWithHeaders(Map.of());

        assertThat(WayangA2aHttpResponseHeaders.protocolVersionHeader(both)).isEqualTo("1.0");
        assertThat(WayangA2aHttpResponseHeaders.protocolVersionHeader(a2aOnly)).isEqualTo(A2aProtocol.VERSION);
        assertThat(WayangA2aHttpResponseHeaders.protocolVersionHeader(missing)).isEmpty();
    }

    @Test
    void protocolHeadersPreserveA2aWireOrder() {
        Map<String, Object> headers = WayangA2aHttpResponse.protocolHeaders("JsonRpcSmoke", "1.0");

        assertThat(headers.keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION);
    }

    @Test
    void withRouteAppendsA2aRouteHeadersAfterExistingHeaders() {
        WayangA2aHttpResponse response = new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "{}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON))
                .withRoute(new A2aHttpRoute(
                        A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        null,
                        "POST",
                        "/message:stream",
                        true,
                        "Streams an A2A message."));

        assertThat(response.headers().keySet()).containsExactly(
                WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_VERSION,
                WayangA2aHttpResponse.HEADER_A2A_STREAMING);
    }

    private static WayangA2aHttpResponse responseWithHeaders(Map<String, Object> headers) {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "{}",
                headers);
    }
}
