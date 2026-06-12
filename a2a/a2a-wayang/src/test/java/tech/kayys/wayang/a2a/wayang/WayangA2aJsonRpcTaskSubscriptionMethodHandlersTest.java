package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcTaskSubscriptionMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcTaskSubscriptionMethodHandlers handlers =
            WayangA2aJsonRpcTaskSubscriptionMethodHandlers.fromStore(store);

    @Test
    void exposesTaskSubscriptionMethodHandler() {
        assertThat(handlers.handlers().keySet()).containsExactly(WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
    }

    @Test
    void dispatchesTaskSubscriptionEventStreamDirectly() {
        store.create(task("task-events", A2aTaskState.TASK_STATE_WORKING));
        store.updateStatus("task-events", A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING));

        WayangA2aHttpResponse response = dispatch(
                "sub-1",
                Map.of("id", "task-events", "pageSize", 10));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(response.body())
                .contains("data: ")
                .contains("\"jsonrpc\":\"2.0\"")
                .contains("\"statusUpdate\"");
    }

    @Test
    void returnsTaskNotFoundForMissingTask() {
        WayangA2aHttpResponse response = dispatch("missing-1", Map.of("id", "missing"));

        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    @Test
    void rejectsTerminalTaskSubscriptions() {
        store.create(task("task-terminal", A2aTaskState.TASK_STATE_COMPLETED));

        WayangA2aHttpResponse response = dispatch("terminal-1", Map.of("id", "task-terminal"));

        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.UNSUPPORTED_OPERATION);
    }

    private WayangA2aHttpResponse dispatch(Object id, Map<String, Object> params) {
        return handlers.handlers()
                .get(WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK)
                .dispatch(
                        WayangA2aJsonRpcRequest.of(id, WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK, params),
                        WayangA2aSendMessagePreflight.JsonRpcResult.empty());
    }

    private static A2aTask task(String id, A2aTaskState state) {
        return new A2aTask(
                id,
                "context-1",
                A2aTaskStatus.of(state),
                List.of(),
                List.of(),
                Map.of());
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
