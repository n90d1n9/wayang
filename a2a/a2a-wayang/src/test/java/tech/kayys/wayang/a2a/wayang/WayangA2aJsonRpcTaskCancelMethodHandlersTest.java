package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcTaskCancelMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcTaskCancelMethodHandlers handlers =
            WayangA2aJsonRpcTaskCancelMethodHandlers.fromStore(store);

    @Test
    void exposesTaskCancelMethodHandler() {
        assertThat(handlers.handlers().keySet()).containsExactly(WayangA2aJsonRpcMethods.CANCEL_TASK);
    }

    @Test
    void dispatchesCancelDirectly() {
        store.create(task("task-1", A2aTaskState.TASK_STATE_WORKING));

        WayangA2aHttpResponse response = dispatch(
                "cancel-1",
                Map.of("id", "task-1", "reason", "client stopped"));

        assertThat(result(response)).containsEntry("id", "task-1");
        assertThat(status(result(response))).containsEntry("state", A2aTaskState.TASK_STATE_CANCELED.value());
        assertThat(status(result(response)))
                .extractingByKey("message")
                .satisfies(message -> assertThat(map(message)).containsEntry("role", "ROLE_AGENT"));
    }

    @Test
    void returnsTaskNotFoundForMissingOrTenantHiddenTask() {
        store.create(task("task-1", A2aTaskState.TASK_STATE_WORKING, "tenant-a"));

        WayangA2aHttpResponse missing = dispatch("missing-1", Map.of("id", "missing"));
        WayangA2aHttpResponse hidden = dispatch("hidden-1", Map.of("id", "task-1", "tenant", "tenant-b"));

        assertThat(error(missing)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
        assertThat(error(hidden)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    @Test
    void letsLifecycleExceptionsBubbleToDispatcher() {
        store.create(task("task-terminal", A2aTaskState.TASK_STATE_COMPLETED));

        assertThatThrownBy(() -> dispatch("terminal-1", Map.of("id", "task-terminal")))
                .isInstanceOf(WayangA2aTaskLifecycleException.class)
                .hasMessageContaining("cannot cancel");
    }

    private WayangA2aHttpResponse dispatch(Object id, Map<String, Object> params) {
        return handlers.handlers()
                .get(WayangA2aJsonRpcMethods.CANCEL_TASK)
                .dispatch(
                        WayangA2aJsonRpcRequest.of(id, WayangA2aJsonRpcMethods.CANCEL_TASK, params),
                        WayangA2aSendMessagePreflight.JsonRpcResult.empty());
    }

    private static A2aTask task(String id, A2aTaskState state) {
        return task(id, state, "tenant-a");
    }

    private static A2aTask task(String id, A2aTaskState state, String tenant) {
        return new A2aTask(
                id,
                "context-1",
                A2aTaskStatus.of(state),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }

    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("result"));
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        return map(WayangA2aHttpJson.read(response.body()).get("error"));
    }

    private static Map<String, Object> status(Map<String, Object> task) {
        return map(task.get("status"));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
