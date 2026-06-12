package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskCancelRequestTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void createsHttpCancelMessageWithDefaultReason() {
        store.create(task("task-1"));

        A2aTask canceled = WayangA2aTaskCancelRequest.fromHttp("task-1", Map.of()).apply(store);

        assertThat(canceled.status().state()).isEqualTo(A2aTaskState.TASK_STATE_CANCELED);
        assertThat(canceled.status().message()).isNotNull();
        assertThat(canceled.status().message().metadata()).containsEntry("source", "a2a.cancel");
        assertThat(canceled.status().message().parts().get(0).toMap())
                .containsEntry("text", "Task canceled by A2A client.");
    }

    @Test
    void jsonRpcCancelMessageIsOptionalButCanCarryReason() {
        store.create(task("task-1"));
        store.create(task("task-2"));

        A2aTask withoutReason = WayangA2aTaskCancelRequest.fromJsonRpc("task-1", Map.of()).apply(store);
        A2aTask withReason = WayangA2aTaskCancelRequest.fromJsonRpc(
                        "task-2",
                        Map.of("reason", "client stopped"))
                .apply(store);

        assertThat(withoutReason.status().message()).isNull();
        assertThat(withReason.status().message()).isNotNull();
        assertThat(withReason.status().message().parts().get(0).toMap())
                .containsEntry("text", "client stopped");
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
