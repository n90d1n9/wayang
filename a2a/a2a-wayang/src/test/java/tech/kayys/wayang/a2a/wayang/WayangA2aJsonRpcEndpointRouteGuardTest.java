package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcEndpointRouteGuardTest {

    @Test
    void acceptsPostRequestsWithJsonBodyAndCompatibleAcceptHeader() {
        WayangA2aHttpRequest request = post(
                WayangA2aJsonRpcRequest.of("send", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()).toJson(),
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);

        Optional<WayangA2aHttpResponse> response = validate(request);

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsNonPostRequestsWithRouteHeaders() {
        WayangA2aHttpRequest request = new WayangA2aHttpRequest(
                "GET",
                "/",
                WayangA2aJsonRpcRequest.of("send", WayangA2aJsonRpcMethods.SEND_MESSAGE, Map.of()).toJson(),
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                Map.of());

        WayangA2aHttpResponse response = validate(request).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(error(response))
                .containsEntry("code", "method_not_allowed")
                .containsEntry("message", "A2A JSON-RPC endpoint / requires POST.");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcMethods.SEND_MESSAGE);
    }

    @Test
    void rejectsMissingRequestBodies() {
        WayangA2aHttpResponse response = validate(post(
                "",
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON))
                .orElseThrow();

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(error(response))
                .containsEntry("code", "missing_request_body")
                .containsEntry("message", "A2A JSON-RPC endpoint / requires a request body.");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC);
    }

    @Test
    void rejectsNonJsonContentTypes() {
        WayangA2aHttpRequest request = post(
                WayangA2aJsonRpcRequest.of(
                        "stream",
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        Map.of()).toJson(),
                "text/plain",
                A2aProtocol.EVENT_STREAM_MEDIA_TYPE);

        WayangA2aHttpResponse response = validate(request).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(error(response))
                .containsEntry("code", "unsupported_media_type")
                .containsEntry("message", "A2A JSON-RPC endpoint / requires Content-Type application/json.");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);
    }

    @Test
    void rejectsRequestsThatDoNotAcceptExpectedResponseMediaType() {
        WayangA2aHttpRequest request = post(
                WayangA2aJsonRpcRequest.of(
                        "stream",
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        Map.of()).toJson(),
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);

        WayangA2aHttpResponse response = validate(request).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(error(response))
                .containsEntry("code", "not_acceptable")
                .containsEntry("message", "A2A JSON-RPC endpoint / produces "
                        + A2aProtocol.EVENT_STREAM_MEDIA_TYPE
                        + ", but Accept was application/json.");
    }

    private static Optional<WayangA2aHttpResponse> validate(WayangA2aHttpRequest request) {
        return WayangA2aJsonRpcEndpointRouteGuard.validate(
                request,
                route(),
                WayangA2aJsonRpcHttpRequestContext.from(request));
    }

    private static WayangA2aJsonRpcHttpRouteDescriptor route() {
        return WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(WayangA2aJsonRpcHttpConfig.defaults()).getFirst();
    }

    private static WayangA2aHttpRequest post(String body, String contentType, String accept) {
        return new WayangA2aHttpRequest(
                "POST",
                "/",
                body,
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE, contentType,
                        WayangA2aHttpResponse.HEADER_ACCEPT, accept),
                Map.of());
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
