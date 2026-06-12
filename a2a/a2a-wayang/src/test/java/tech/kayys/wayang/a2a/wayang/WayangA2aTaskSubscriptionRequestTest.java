package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskSubscriptionRequestTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void httpSubscriptionsAllowTerminalReplayAndApplyCursor() {
        store.create(task("task-1", A2aTaskState.TASK_STATE_WORKING));
        store.updateStatus("task-1", A2aTaskStatus.of(A2aTaskState.TASK_STATE_COMPLETED));

        WayangA2aTaskSubscriptionRequest request =
                WayangA2aTaskSubscriptionRequest.fromHttp(
                        "task-1",
                        Map.<String, Object>of("afterSequence", 1, "limit", 1));

        assertThat(request.terminalUnsupported(store.get("task-1").orElseThrow())).isFalse();
        assertThat(request.events(store))
                .extracting(WayangA2aTaskEvent::sequence)
                .containsExactly(2L);
    }

    @Test
    void jsonRpcSubscriptionsRejectTerminalTasks() {
        A2aTask task = task("task-1", A2aTaskState.TASK_STATE_COMPLETED);

        WayangA2aTaskSubscriptionRequest request =
                WayangA2aTaskSubscriptionRequest.fromJsonRpc(
                        "task-1",
                        Map.<String, Object>of("pageSize", 5));

        assertThat(request.terminalUnsupported(task)).isTrue();
        assertThat(request.cursor().limit()).isEqualTo(5);
    }

    private static A2aTask task(String taskId, A2aTaskState state) {
        return new A2aTask(
                taskId,
                "context-1",
                A2aTaskStatus.of(state),
                List.of(),
                List.of(),
                Map.of());
    }
}
