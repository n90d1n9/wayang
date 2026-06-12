package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aPushNotificationConfigCommandsTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aPushNotificationConfigCommands commands =
            WayangA2aPushNotificationConfigCommands.fromStore(store);

    @Test
    void createsGetsListsAndDeletesConfigsThroughStoreBoundary() {
        store.create(task("task-1"));

        WayangA2aPushNotificationConfig config = commands.create(
                "task-1",
                Map.of(
                        "configId", "primary",
                        "url", "https://hooks.test/a2a",
                        "metadata", Map.of("tenant", "tenant-a")));

        assertThat(commands.get("task-1", "primary")).contains(config);
        assertThat(commands.list("task-1").toHttpMap())
                .containsEntry("configCount", 1)
                .containsKey("configs");
        assertThat(commands.list("task-1").toHttpMap().keySet())
                .containsExactly("configCount", "configs");
        assertThat(commands.list("task-1").toJsonRpcMap())
                .containsEntry("nextPageToken", "")
                .containsKey("configs");
        assertThat(commands.list("task-1").toJsonRpcMap().keySet())
                .containsExactly("configs", "nextPageToken");
        assertThat(commands.delete("task-1", "primary"))
                .satisfies(result -> {
                    assertThat(result.deleted()).isTrue();
                    assertThat(result.toHttpMap()).containsEntry("configId", "primary");
                    assertThat(result.toHttpMap().keySet())
                            .containsExactly("taskId", "configId", "deleted");
                    assertThat(result.toJsonRpcMap()).containsEntry("deleted", true);
                    assertThat(result.toJsonRpcMap().keySet()).containsExactly("deleted");
                });
        assertThat(commands.get("task-1", "primary")).isEmpty();
    }

    @Test
    void reportsMissingDeleteWithoutMutatingResponseShape() {
        store.create(task("task-1"));

        WayangA2aPushNotificationConfigDeleteResult result = commands.delete("task-1", "missing");

        assertThat(result.deleted()).isFalse();
        assertThat(result.toHttpMap())
                .containsEntry("taskId", "task-1")
                .containsEntry("configId", "missing")
                .containsEntry("deleted", false);
        assertThat(result.toJsonRpcMap()).containsEntry("deleted", false);
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
}
