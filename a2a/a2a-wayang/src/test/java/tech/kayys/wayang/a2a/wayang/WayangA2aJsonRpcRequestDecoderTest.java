package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcRequestDecoderTest {

    private final WayangA2aJsonRpcRequestDecoder decoder = WayangA2aJsonRpcRequestDecoder.create();

    @Test
    void decodesValidJsonRpcRequest() {
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "decode-1",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of("id", "task-1"));

        WayangA2aJsonRpcRequestDecoder.Result result = decoder.decode(request.toJson());

        assertThat(result.error()).isEmpty();
        assertThat(result.request()).isPresent();
        assertThat(result.request().orElseThrow().id()).isEqualTo("decode-1");
        assertThat(result.request().orElseThrow().method()).isEqualTo(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(result.request().orElseThrow().params()).containsEntry("id", "task-1");
    }

    @Test
    void returnsParseErrorForMalformedJson() {
        WayangA2aJsonRpcRequestDecoder.Result result = decoder.decode("{");

        assertThat(result.request()).isEmpty();
        assertThat(result.error()).isPresent();
        assertThat(error(result.error().orElseThrow()))
                .containsEntry("code", WayangA2aJsonRpcError.PARSE_ERROR);
        assertThat(envelope(result.error().orElseThrow())).doesNotContainKey("id");
    }

    @Test
    void returnsInvalidRequestErrorWithOriginalId() {
        WayangA2aJsonRpcRequestDecoder.Result result = decoder.decode(WayangA2aHttpJson.write(Map.of(
                "jsonrpc", "1.0",
                "id", "invalid-1",
                "method", WayangA2aJsonRpcMethods.GET_TASK)));

        assertThat(result.request()).isEmpty();
        assertThat(result.error()).isPresent();
        assertThat(envelope(result.error().orElseThrow())).containsEntry("id", "invalid-1");
        assertThat(error(result.error().orElseThrow()))
                .containsEntry("code", WayangA2aJsonRpcError.INVALID_REQUEST)
                .containsEntry("message", "JSON-RPC request must declare jsonrpc 2.0");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> envelope(WayangA2aHttpResponse response) {
        return WayangA2aHttpJson.read(response.body());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return (Map<String, Object>) envelope(response).get("error");
    }
}
