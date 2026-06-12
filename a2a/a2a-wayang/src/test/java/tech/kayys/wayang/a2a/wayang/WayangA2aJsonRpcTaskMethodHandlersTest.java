package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcTaskMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcTaskMethodHandlers handlers =
            WayangA2aJsonRpcTaskMethodHandlers.fromStore(store);

    @Test
    void exposesTaskAndPushConfigMethodHandlersInProtocolOrder() {
        assertThat(handlers.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.GET_TASK,
                WayangA2aJsonRpcMethods.LIST_TASKS,
                WayangA2aJsonRpcMethods.CANCEL_TASK,
                WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK,
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
    }

    @Test
    void dispatchesTaskLifecycleMethodsDirectly() {
        store.create(task("task-1", A2aTaskState.TASK_STATE_WORKING));

        WayangA2aHttpResponse get = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK,
                "get-1",
                Map.of("id", "task-1"));
        WayangA2aHttpResponse list = dispatch(
                WayangA2aJsonRpcMethods.LIST_TASKS,
                "list-1",
                Map.of("contextId", "context-1"));
        WayangA2aHttpResponse cancel = dispatch(
                WayangA2aJsonRpcMethods.CANCEL_TASK,
                "cancel-1",
                Map.of("id", "task-1", "reason", "client canceled"));

        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(result(get)).containsEntry("id", "task-1");
        assertThat(result(list)).containsEntry("totalSize", 1);
        assertThat(result(cancel)).containsEntry("id", "task-1");
        assertThat(status(result(cancel))).containsEntry("state", A2aTaskState.TASK_STATE_CANCELED.value());
    }

    @Test
    void dispatchesSubscribeAndPushConfigMethodsDirectly() {
        store.create(task("task-events", A2aTaskState.TASK_STATE_WORKING));
        store.updateStatus("task-events", A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING));

        WayangA2aHttpResponse subscribe = dispatch(
                WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK,
                "sub-1",
                Map.of("id", "task-events", "pageSize", 10));
        WayangA2aHttpResponse createPush = dispatch(
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                "push-1",
                Map.of("taskId", "task-events", "configId", "primary", "url", "https://hooks.test/a2a"));
        WayangA2aHttpResponse listPush = dispatch(
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                "push-list-1",
                Map.of("taskId", "task-events"));

        assertThat(subscribe.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(subscribe.body()).contains("data: ").contains("\"statusUpdate\"");
        assertThat(result(createPush))
                .containsEntry("taskId", "task-events")
                .containsEntry("configId", "primary");
        assertThat(listOfMaps(result(listPush), "configs"))
                .singleElement()
                .satisfies(config -> assertThat(config).containsEntry("url", "https://hooks.test/a2a"));
    }

    @Test
    void returnsJsonRpcTaskNotFoundEnvelopeDirectly() {
        WayangA2aHttpResponse response = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK,
                "missing-1",
                Map.of("id", "missing"));

        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    private WayangA2aHttpResponse dispatch(String method, Object id, Map<String, Object> params) {
        return handlers.handlers()
                .get(method)
                .dispatch(
                        WayangA2aJsonRpcRequest.of(id, method, params),
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
