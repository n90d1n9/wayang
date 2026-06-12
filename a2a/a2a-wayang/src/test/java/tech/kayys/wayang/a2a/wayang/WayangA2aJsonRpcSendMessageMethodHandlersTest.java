package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcSendMessageMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcSendMessageMethodHandlers handlers =
            WayangA2aJsonRpcSendMessageMethodHandlers.forService(new WayangA2aSendMessageService(
                    store,
                    request -> AgentResponse.builder()
                            .runId("run-send")
                            .requestId(request.requestId())
                            .answer("pong")
                            .strategy("react")
                            .build()));

    @Test
    void exposesSendMessageMethodHandlersInProtocolOrder() {
        assertThat(handlers.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);
    }

    @Test
    void dispatchesSendMessageWithParsedPreflightRequest() {
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-send",
                "context-send",
                "task-send",
                "ping");

        WayangA2aHttpResponse response = dispatch(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                "send-1",
                preflight(sendRequest));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(resultTask(response)).containsEntry("id", "task-send");
        assertThat(store.get("task-send")).isPresent();
    }

    @Test
    void dispatchesStreamingSendMessageWithParsedPreflightRequest() {
        A2aSendMessageRequest sendRequest = WayangA2aSendMessageServiceTest.request(
                "message-stream",
                "context-stream",
                "task-stream",
                "ping");

        WayangA2aHttpResponse response = dispatch(
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                "stream-1",
                preflight(sendRequest));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(response.body())
                .contains("data: ")
                .contains("\"task\"")
                .contains("\"task-stream\"");
        assertThat(store.get("task-stream")).isPresent();
    }

    private WayangA2aHttpResponse dispatch(
            String method,
            Object id,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        return handlers.handlers()
                .get(method)
                .dispatch(WayangA2aJsonRpcRequest.of(id, method, Map.of()), preflight);
    }

    private static WayangA2aSendMessagePreflight.JsonRpcResult preflight(A2aSendMessageRequest request) {
        return new WayangA2aSendMessagePreflight.JsonRpcResult(Optional.of(request), Optional.empty());
    }

    private static Map<String, Object> resultTask(WayangA2aHttpResponse response) {
        return map(result(response).get("task"));
    }

    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("result"));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
