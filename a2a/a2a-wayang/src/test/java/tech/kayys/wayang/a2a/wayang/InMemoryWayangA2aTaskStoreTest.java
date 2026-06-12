package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryWayangA2aTaskStoreTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void storesTaskLifecycleHistoryArtifactsAndReplayEvents() {
        A2aTask task = task("task-1", "context-1", A2aTaskState.TASK_STATE_SUBMITTED);
        A2aMessage userMessage = message("message-1", A2aRole.ROLE_USER, "hello");
        A2aArtifact artifact = new A2aArtifact(
                "artifact-1",
                "answer",
                null,
                List.of(A2aPart.text("done")),
                Map.of(),
                List.of());

        store.create(task);
        store.appendMessage("task-1", userMessage);
        store.updateStatus("task-1", A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING));
        A2aTask withArtifact = store.appendArtifact("task-1", artifact);

        assertThat(withArtifact.history()).containsExactly(userMessage);
        assertThat(withArtifact.artifacts()).containsExactly(artifact);
        assertThat(store.get("task-1")).contains(withArtifact);
        assertThat(store.events("task-1", 0, 10))
                .extracting(WayangA2aTaskEvent::type)
                .containsExactly(
                        WayangA2aTaskEvent.TYPE_TASK_CREATED,
                        WayangA2aTaskEvent.TYPE_MESSAGE_APPENDED,
                        WayangA2aTaskEvent.TYPE_STATUS_UPDATED,
                        WayangA2aTaskEvent.TYPE_ARTIFACT_APPENDED);
        assertThat(store.events("task-1", 2, 10))
                .extracting(WayangA2aTaskEvent::sequence)
                .containsExactly(3L, 4L);
    }

    @Test
    void filtersTasksAndCancelsNonTerminalTasksOnly() {
        store.create(task("task-1", "context-a", A2aTaskState.TASK_STATE_WORKING));
        store.create(task("task-2", "context-b", A2aTaskState.TASK_STATE_COMPLETED));
        store.create(task("task-3", "context-a", A2aTaskState.TASK_STATE_WORKING, "tenant-b"));

        A2aTask canceled = store.cancel("task-1", message("cancel-1", A2aRole.ROLE_AGENT, "stop"));

        assertThat(canceled.status().state()).isEqualTo(A2aTaskState.TASK_STATE_CANCELED);
        assertThatThrownBy(() -> store.cancel("task-2", message("cancel-2", A2aRole.ROLE_AGENT, "stop")))
                .isInstanceOf(WayangA2aTaskLifecycleException.class)
                .hasMessageContaining("terminal")
                .hasMessageContaining("cancel");
        assertThat(store.list(new WayangA2aTaskQuery(
                "context-a",
                Set.of(A2aTaskState.TASK_STATE_CANCELED),
                10)))
                .singleElement()
                .extracting(A2aTask::id)
                .isEqualTo("task-1");
        assertThat(store.list(new WayangA2aTaskQuery(
                "tenant-b",
                "context-a",
                Set.of(A2aTaskState.TASK_STATE_WORKING),
                10)))
                .singleElement()
                .extracting(A2aTask::id)
                .isEqualTo("task-3");
        assertThat(store.events("task-1", 0, 10))
                .extracting(WayangA2aTaskEvent::type)
                .contains(WayangA2aTaskEvent.TYPE_TASK_CANCELED);
    }

    @Test
    void managesPushNotificationConfigsPerTask() {
        store.create(task("task-1", "context-1", A2aTaskState.TASK_STATE_WORKING));
        WayangA2aPushNotificationConfig config = new WayangA2aPushNotificationConfig(
                "task-1",
                "primary",
                "https://hooks.test/a2a",
                Map.of("type", "bearer"),
                Map.of("tenant", "default"));

        store.putPushNotificationConfig(config);

        assertThat(store.getPushNotificationConfig("task-1", "primary")).contains(config);
        assertThat(store.listPushNotificationConfigs("task-1")).containsExactly(config);
        assertThat(store.deletePushNotificationConfig("task-1", "primary")).isTrue();
        assertThat(store.getPushNotificationConfig("task-1", "primary")).isEmpty();
    }

    @Test
    void rejectsDuplicateAndMissingTasks() {
        store.create(task("task-1", "context-1", A2aTaskState.TASK_STATE_WORKING));

        assertThatThrownBy(() -> store.create(task("task-1", "context-1", A2aTaskState.TASK_STATE_WORKING)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        assertThatThrownBy(() -> store.updateStatus("missing", A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rejectsTerminalAndUnspecifiedLifecycleMutations() {
        store.create(task("task-terminal", "context-terminal", A2aTaskState.TASK_STATE_COMPLETED));

        assertThatThrownBy(() -> store.updateStatus(
                "task-terminal",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING)))
                .isInstanceOf(WayangA2aTaskLifecycleException.class)
                .hasMessageContaining("terminal")
                .hasMessageContaining("transition");
        assertThatThrownBy(() -> store.appendMessage(
                "task-terminal",
                message("message-terminal", A2aRole.ROLE_AGENT, "late")))
                .isInstanceOf(WayangA2aTaskLifecycleException.class)
                .hasMessageContaining("append messages");
        assertThatThrownBy(() -> store.updateStatus(
                "task-terminal",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_UNSPECIFIED)))
                .isInstanceOf(WayangA2aTaskLifecycleException.class)
                .hasMessageContaining("TASK_STATE_UNSPECIFIED");
    }

    private static A2aTask task(String id, String contextId, A2aTaskState state) {
        return new A2aTask(id, contextId, A2aTaskStatus.of(state), List.of(), List.of(), Map.of());
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

    private static A2aMessage message(String id, A2aRole role, String text) {
        return new A2aMessage(id, null, null, role, List.of(A2aPart.text(text)), Map.of(), List.of(), List.of());
    }
}
