package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcTaskQueryMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcTaskQueryMethodHandlers handlers =
            WayangA2aJsonRpcTaskQueryMethodHandlers.fromStore(store);

    @Test
    void exposesTaskQueryMethodHandlersInProtocolOrder() {
        assertThat(handlers.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.GET_TASK,
                WayangA2aJsonRpcMethods.LIST_TASKS);
    }

    @Test
    void dispatchesGetAndListDirectly() {
        store.create(task("task-1", "context-1", A2aTaskState.TASK_STATE_WORKING, "tenant-a"));
        store.create(task("task-2", "context-1", A2aTaskState.TASK_STATE_SUBMITTED, "tenant-b"));

        WayangA2aHttpResponse get = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK,
                "get-1",
                Map.of("id", "task-1", "tenant", "tenant-a"));
        WayangA2aHttpResponse list = dispatch(
                WayangA2aJsonRpcMethods.LIST_TASKS,
                "list-1",
                Map.of("contextId", "context-1", "tenant", "tenant-a"));

        assertThat(result(get)).containsEntry("id", "task-1");
        assertThat(result(list))
                .containsEntry("totalSize", 1)
                .containsEntry("pageSize", 100);
        assertThat(listOfMaps(result(list), "tasks"))
                .singleElement()
                .satisfies(task -> assertThat(task).containsEntry("id", "task-1"));
    }

    @Test
    void returnsTaskNotFoundForMissingOrTenantHiddenTask() {
        store.create(task("task-1", "context-1", A2aTaskState.TASK_STATE_WORKING, "tenant-a"));

        WayangA2aHttpResponse missing = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK,
                "missing-1",
                Map.of("id", "missing"));
        WayangA2aHttpResponse hidden = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK,
                "hidden-1",
                Map.of("id", "task-1", "tenant", "tenant-b"));

        assertThat(error(missing)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
        assertThat(error(hidden)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    private WayangA2aHttpResponse dispatch(String method, Object id, Map<String, Object> params) {
        return handlers.handlers()
                .get(method)
                .dispatch(
                        WayangA2aJsonRpcRequest.of(id, method, params),
                        WayangA2aSendMessagePreflight.JsonRpcResult.empty());
    }

    private static A2aTask task(String id, String contextId, A2aTaskState state, String tenant) {
        return new A2aTask(
                id,
                contextId,
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

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static List<Map<String, Object>> listOfMaps(Map<String, Object> source, String key) {
        assertThat(source.get(key)).isInstanceOf(List.class);
        return ((List<?>) source.get(key)).stream()
                .map(value -> {
                    assertThat(value).isInstanceOf(Map.class);
                    return WayangA2aMaps.copyMap((Map<?, ?>) value);
                })
                .toList();
    }
}
