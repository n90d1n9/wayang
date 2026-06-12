package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcRequestDispatcherTest {

    @Test
    void exposesDispatchMethodsAndCoverageFromDispatchTable() {
        WayangA2aJsonRpcRequestDispatcher dispatcher = dispatcher(Map.of(
                WayangA2aJsonRpcMethods.GET_TASK,
                (request, preflight) -> result(request.id(), Map.of("ok", true))));

        assertThat(dispatcher.methods()).containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(dispatcher.coverage().complete()).isFalse();
    }

    @Test
    void dispatchesKnownMethodThroughPreflightAndExecutor() {
        WayangA2aJsonRpcRequestDispatcher dispatcher = dispatcher(Map.of(
                WayangA2aJsonRpcMethods.GET_TASK,
                (request, preflight) -> result(request.id(), Map.of("taskId", request.params().get("id")))));

        WayangA2aHttpResponse response = dispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "get-1",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of("id", "task-1")));

        assertThat(result(response)).containsEntry("taskId", "task-1");
    }

    @Test
    void returnsMethodNotFoundBeforePreflightForUnregisteredMethod() {
        WayangA2aJsonRpcRequestDispatcher dispatcher = dispatcher(Map.of(
                WayangA2aJsonRpcMethods.GET_TASK,
                (request, preflight) -> result(request.id(), Map.of("ok", true))));

        WayangA2aHttpResponse response = dispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "missing-1",
                "UnknownMethod",
                Map.of()));

        assertThat(error(response))
                .containsEntry("code", WayangA2aJsonRpcError.METHOD_NOT_FOUND)
                .containsEntry("message", "Method not found: UnknownMethod");
    }

    @Test
    void returnsPreflightErrorBeforeExecutingHandler() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        WayangA2aJsonRpcRequestDispatcher dispatcher = dispatcher(Map.of(
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                (request, preflight) -> {
                    handlerCalled.set(true);
                    return result(request.id(), Map.of("streamed", true));
                }));

        WayangA2aHttpResponse response = dispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "stream-1",
                WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                WayangA2aSendMessageServiceTest.request(
                        "message-stream",
                        "context-stream",
                        "task-stream",
                        "ping").toMap()));

        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
        assertThat(handlerCalled).isFalse();
    }

    @Test
    void fromAgentCardAcceptsExtraProvidersForConfigurableDispatch() {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(
                new InMemoryWayangA2aTaskStore(),
                request -> null);
        WayangA2aJsonRpcMethodHandlerProvider provider = () -> WayangA2aJsonRpcMethodHandlerGroup.of(
                "task-override",
                Map.of(WayangA2aJsonRpcMethods.GET_TASK,
                        (request, preflight) -> result(request.id(), Map.of("id", request.params().get("id")))));
        WayangA2aJsonRpcRequestDispatcher dispatcher = WayangA2aJsonRpcRequestDispatcher.fromAgentCard(
                card(),
                new InMemoryWayangA2aTaskStore(),
                service,
                List.of(provider));

        WayangA2aHttpResponse response = dispatcher.dispatch(WayangA2aJsonRpcRequest.of(
                "task-provider",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of("id", "task-provider")));

        assertThat(result(response)).containsEntry("id", "task-provider");
    }

    private static WayangA2aJsonRpcRequestDispatcher dispatcher(
            Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers) {
        A2aAgentCard card = card();
        return WayangA2aJsonRpcRequestDispatcher.create(
                WayangA2aJsonRpcMethodDispatchTable.of(handlers),
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(card),
                WayangA2aJsonRpcMethodHandlerExecutor.create());
    }

    private static WayangA2aHttpResponse result(Object id, Map<String, Object> result) {
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(id, result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("result");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("error");
    }

    private static A2aAgentCard card() {
        return A2aAgentCard.minimal(
                "wayang-request-dispatcher",
                "A request dispatcher test card",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
