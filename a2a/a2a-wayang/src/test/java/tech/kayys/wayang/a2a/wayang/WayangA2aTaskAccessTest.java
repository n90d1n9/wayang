package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskAccessTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void filtersHttpTaskAccessByTenantHint() {
        store.create(task("task-a", "tenant-a"));
        store.create(task("task-b", "tenant-b"));
        WayangA2aHttpRequest request = WayangA2aHttpRequest.get("/tasks/task-b")
                .withAttribute("tenant", "tenant-a");

        assertThat(WayangA2aTaskAccess.getForHttp(store, request, "task-b")).isEmpty();
        assertThat(WayangA2aTaskAccess.getForHttp(store, request, "task-a"))
                .map(A2aTask::id)
                .hasValue("task-a");
    }

    @Test
    void filtersJsonRpcTaskAccessByTenantHint() {
        store.create(task("task-a", "tenant-a"));
        store.create(task("task-b", "tenant-b"));
        WayangA2aJsonRpcRequest request = WayangA2aJsonRpcRequest.of(
                "tenant-scope",
                WayangA2aJsonRpcMethods.GET_TASK,
                Map.of("id", "task-b", "metadata", Map.of("tenantId", "tenant-a")));

        assertThat(WayangA2aTaskAccess.getForJsonRpc(store, request, "task-b")).isEmpty();
        assertThat(WayangA2aTaskAccess.getForJsonRpc(store, request, "task-a"))
                .map(A2aTask::id)
                .hasValue("task-a");
    }

    @Test
    void allowsUnscopedTaskAccess() {
        store.create(task("task-a", "tenant-a"));

        assertThat(WayangA2aTaskAccess.get(store, "task-a", Optional.empty()))
                .map(A2aTask::id)
                .hasValue("task-a");
    }

    private static A2aTask task(String id, String tenant) {
        return new A2aTask(
                id,
                "context-" + id,
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }
}
