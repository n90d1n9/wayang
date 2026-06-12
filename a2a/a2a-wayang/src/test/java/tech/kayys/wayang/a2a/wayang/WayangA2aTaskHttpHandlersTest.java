package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskHttpHandlersTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void dispatchesTaskGetListCancelAndSubscribeThroughHttpHandlers() {
        store.create(new A2aTask(
                "task-1",
                "context-1",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of()));
        store.appendArtifact("task-1", new A2aArtifact(
                "artifact-1",
                "answer",
                null,
                List.of(A2aPart.text("hello")),
                Map.of(),
                List.of()));
        WayangA2aHttpOperationDispatcher dispatcher = dispatcher();

        WayangA2aHttpResponse get = dispatcher.dispatch(WayangA2aHttpRequest.get("/tasks/task-1"));
        WayangA2aHttpResponse list = dispatcher.dispatch(new WayangA2aHttpRequest(
                "GET",
                "/tasks",
                "",
                Map.of(),
                Map.of("contextId", "context-1", "state", A2aTaskState.TASK_STATE_WORKING.value())));
        WayangA2aHttpResponse cancel = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/tasks/task-1:cancel",
                "{\"reason\":\"client canceled\"}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()));
        WayangA2aHttpResponse subscribe = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/tasks/task-1:subscribe",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                Map.of("afterSequence", 1, "limit", 5)));

        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(get.body())).containsEntry("id", "task-1");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(list.body())).containsEntry("taskCount", 1);
        assertThat(cancel.statusCode()).isEqualTo(200);
        assertThat(A2aTask.fromJson(cancel.body()).status().state()).isEqualTo(A2aTaskState.TASK_STATE_CANCELED);
        assertThat(subscribe.statusCode()).isEqualTo(200);
        assertThat(subscribe.contentType()).isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(subscribe.body()).contains("task.artifact.appended").contains("task.canceled");
    }

    @Test
    void dispatchesPushNotificationConfigHandlers() {
        store.create(new A2aTask(
                "task-1",
                "context-1",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of()));
        WayangA2aHttpOperationDispatcher dispatcher = dispatcher();

        WayangA2aHttpResponse create = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/tasks/task-1/pushNotificationConfigs",
                "{\"configId\":\"primary\",\"url\":\"https://hooks.test/a2a\"}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()));
        WayangA2aHttpResponse get = dispatcher.dispatch(WayangA2aHttpRequest.get(
                "/tasks/task-1/pushNotificationConfigs/primary"));
        WayangA2aHttpResponse list = dispatcher.dispatch(WayangA2aHttpRequest.get(
                "/tasks/task-1/pushNotificationConfigs"));
        WayangA2aHttpResponse delete = dispatcher.dispatch(new WayangA2aHttpRequest(
                "DELETE",
                "/tasks/task-1/pushNotificationConfigs/primary",
                "",
                Map.of(),
                Map.of()));

        assertThat(create.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(create.body())).containsEntry("configId", "primary");
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(get.body())).containsEntry("url", "https://hooks.test/a2a");
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(list.body())).containsEntry("configCount", 1);
        assertThat(delete.statusCode()).isEqualTo(200);
        assertThat(WayangA2aHttpJson.read(delete.body())).containsEntry("deleted", true);
    }

    @Test
    void scopesTaskReadsAndListsByTenant() {
        store.create(task("task-a", "context-1", A2aTaskState.TASK_STATE_WORKING, "tenant-a"));
        store.create(task("task-b", "context-1", A2aTaskState.TASK_STATE_WORKING, "tenant-b"));
        WayangA2aHttpOperationDispatcher dispatcher = dispatcher();

        WayangA2aHttpResponse hiddenGet = dispatcher.dispatch(new WayangA2aHttpRequest(
                "GET",
                "/tasks/task-b",
                "",
                Map.of(),
                Map.of("tenant", "tenant-a")));
        WayangA2aHttpResponse list = dispatcher.dispatch(new WayangA2aHttpRequest(
                "GET",
                "/tasks",
                "",
                Map.of(),
                Map.of("tenant", "tenant-a", "contextId", "context-1")));

        assertThat(hiddenGet.statusCode()).isEqualTo(404);
        assertThat(WayangA2aHttpJson.read(hiddenGet.body()))
                .extractingByKey("error")
                .satisfies(error -> assertThat(WayangA2aMaps.copyMap((Map<?, ?>) error))
                        .containsEntry("code", "task_not_found"));
        assertThat(WayangA2aHttpJson.read(list.body())).containsEntry("taskCount", 1);
        assertThat((List<?>) WayangA2aHttpJson.read(list.body()).get("tasks"))
                .singleElement()
                .satisfies(task -> assertThat(WayangA2aMaps.copyMap((Map<?, ?>) task))
                        .containsEntry("id", "task-a"));
    }

    @Test
    void mapsTerminalCancelToUnsupportedOperation() {
        store.create(new A2aTask(
                "task-terminal",
                "context-terminal",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_COMPLETED),
                List.of(),
                List.of(),
                Map.of()));
        WayangA2aHttpOperationDispatcher dispatcher = dispatcher();

        WayangA2aHttpResponse response = dispatcher.dispatch(new WayangA2aHttpRequest(
                "POST",
                "/tasks/task-terminal:cancel",
                "{\"reason\":\"too late\"}",
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of()));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(WayangA2aMaps.copyMap((Map<?, ?>) WayangA2aHttpJson.read(response.body()).get("error")))
                .containsEntry("code", "unsupported_operation");
    }

    private WayangA2aHttpOperationDispatcher dispatcher() {
        return new WayangA2aHttpOperationDispatcher(
                new A2aAgentCard(
                        "Wayang",
                        "A2A endpoint",
                        List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                        null,
                        "1.0.0",
                        null,
                        new A2aAgentCapabilities(true, true, List.of(), false),
                        Map.of(),
                        List.of(),
                        List.of("text/plain"),
                        List.of("text/plain"),
                        List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))),
                        List.of(),
                        null),
                WayangA2aTaskHttpHandlers.forStore(store));
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
}
