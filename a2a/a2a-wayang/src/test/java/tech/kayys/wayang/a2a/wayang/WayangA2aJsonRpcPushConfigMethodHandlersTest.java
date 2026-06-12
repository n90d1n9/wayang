package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcPushConfigMethodHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcPushConfigMethodHandlers handlers =
            WayangA2aJsonRpcPushConfigMethodHandlers.fromStore(store);

    @Test
    void exposesPushConfigMethodHandlersInProtocolOrder() {
        assertThat(handlers.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
    }

    @Test
    void dispatchesPushConfigMethodsDirectly() {
        store.create(task("task-push"));

        WayangA2aHttpResponse create = dispatch(
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                "push-create",
                Map.of("taskId", "task-push", "configId", "primary", "url", "https://hooks.test/a2a"));
        WayangA2aHttpResponse get = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                "push-get",
                Map.of("taskId", "task-push", "configId", "primary"));
        WayangA2aHttpResponse list = dispatch(
                WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                "push-list",
                Map.of("taskId", "task-push"));
        WayangA2aHttpResponse delete = dispatch(
                WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                "push-delete",
                Map.of("taskId", "task-push", "configId", "primary"));

        assertThat(result(create))
                .containsEntry("taskId", "task-push")
                .containsEntry("configId", "primary")
                .containsEntry("url", "https://hooks.test/a2a");
        assertThat(result(get)).containsEntry("configId", "primary");
        assertThat(listOfMaps(result(list), "configs"))
                .singleElement()
                .satisfies(config -> assertThat(config).containsEntry("configId", "primary"));
        assertThat(result(delete)).containsEntry("deleted", true);
        assertThat(store.listPushNotificationConfigs("task-push")).isEmpty();
    }

    @Test
    void returnsTaskNotFoundForMissingTaskOrConfig() {
        store.create(task("task-push"));

        WayangA2aHttpResponse missingTask = dispatch(
                WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                "missing-task",
                Map.of("taskId", "missing", "configId", "primary", "url", "https://hooks.test/a2a"));
        WayangA2aHttpResponse missingConfig = dispatch(
                WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                "missing-config",
                Map.of("taskId", "task-push", "configId", "missing"));

        assertThat(error(missingTask)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
        assertThat(error(missingConfig)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    private WayangA2aHttpResponse dispatch(String method, Object id, Map<String, Object> params) {
        return handlers.handlers()
                .get(method)
                .dispatch(
                        WayangA2aJsonRpcRequest.of(id, method, params),
                        WayangA2aSendMessagePreflight.JsonRpcResult.empty());
    }

    private static A2aTask task(String taskId) {
        return new A2aTask(
                taskId,
                "context-1",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
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
