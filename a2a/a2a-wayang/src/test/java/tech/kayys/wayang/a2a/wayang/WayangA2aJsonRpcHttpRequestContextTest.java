package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRequestContextTest {

    @Test
    void extractsJsonRpcIdMethodAndExpectedResponseMediaType() {
        WayangA2aJsonRpcRequest rpcRequest = WayangA2aJsonRpcRequest.of(
                "stream-1",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                Map.of("message", Map.of("messageId", "message-1")));
        String requestJson = rpcRequest.toJson();
        WayangA2aJsonRpcHttpRequestContext context = WayangA2aJsonRpcHttpRequestContext.from(
                WayangA2aHttpRequest.postJson("/", requestJson));

        assertThat(requestJson).startsWith("{\"jsonrpc\":");
        assertThat(requestJson.indexOf("\"id\"")).isGreaterThan(requestJson.indexOf("\"jsonrpc\""));
        assertThat(requestJson.indexOf("\"method\"")).isGreaterThan(requestJson.indexOf("\"id\""));
        assertThat(requestJson.indexOf("\"params\"")).isGreaterThan(requestJson.indexOf("\"method\""));
        assertThat(context.id()).contains("stream-1");
        assertThat(context.methodName()).contains(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);
        assertThat(context.methodOr("JsonRpc")).isEqualTo(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);
        assertThat(context.responseMediaType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
    }

    @Test
    void preservesIdWhenJsonRpcEnvelopeIsInvalid() {
        WayangA2aJsonRpcHttpRequestContext context = WayangA2aJsonRpcHttpRequestContext.from(
                WayangA2aHttpRequest.postJson(
                        "/",
                        WayangA2aHttpJson.write(Map.of(
                                "jsonrpc", "1.0",
                                "id", "bad-version",
                                "method", WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))));

        assertThat(context.id()).contains("bad-version");
        assertThat(context.methodName()).isEmpty();
        assertThat(context.methodOr("JsonRpc")).isEqualTo("JsonRpc");
        assertThat(context.responseMediaType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
    }

    @Test
    void fallsBackForBlankOrUnparseableBodies() {
        WayangA2aJsonRpcHttpRequestContext blank = WayangA2aJsonRpcHttpRequestContext.from(
                WayangA2aHttpRequest.postJson("/", ""));
        WayangA2aJsonRpcHttpRequestContext malformed = WayangA2aJsonRpcHttpRequestContext.from(
                WayangA2aHttpRequest.postJson("/", "{"));

        assertThat(blank.id()).isEmpty();
        assertThat(blank.methodName()).isEmpty();
        assertThat(blank.responseMediaType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(malformed.id()).isEmpty();
        assertThat(malformed.methodName()).isEmpty();
        assertThat(malformed.methodOr(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC))
                .isEqualTo(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC);
    }
}
