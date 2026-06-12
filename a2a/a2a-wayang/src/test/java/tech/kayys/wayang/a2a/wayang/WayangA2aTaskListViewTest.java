package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskListViewTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();

    @Test
    void projectsHttpAndJsonRpcTaskListEnvelopes() {
        store.create(task("task-1", "context-1", "tenant-a"));
        store.create(task("task-2", "context-2", "tenant-b"));

        WayangA2aTaskListView view = WayangA2aTaskListView.fromStore(
                store,
                new WayangA2aTaskQuery(
                        "tenant-a",
                        null,
                        Set.of(A2aTaskState.TASK_STATE_WORKING),
                        10));
        Map<String, Object> http = view.toHttpMap();
        Map<String, Object> jsonRpc = view.toJsonRpcMap();
        String httpJson = WayangA2aHttpJson.write(http);
        String jsonRpcJson = WayangA2aHttpJson.write(jsonRpc);

        assertThat(http)
                .containsEntry("taskCount", 1)
                .containsKey("tasks");
        assertThat(http.keySet()).containsExactly("taskCount", "tasks");
        assertThat(httpJson).startsWith("{\"taskCount\":");
        assertThat(httpJson.indexOf("\"tasks\"")).isGreaterThan(httpJson.indexOf("\"taskCount\""));
        assertThat(jsonRpc)
                .containsEntry("pageSize", 10)
                .containsEntry("totalSize", 1)
                .containsEntry("nextPageToken", "")
                .containsKey("tasks");
        assertThat(jsonRpc.keySet()).containsExactly("tasks", "nextPageToken", "pageSize", "totalSize");
        assertThat(jsonRpcJson).startsWith("{\"tasks\":");
        assertThat(jsonRpcJson.indexOf("\"nextPageToken\"")).isGreaterThan(jsonRpcJson.indexOf("\"tasks\""));
        assertThat(jsonRpcJson.indexOf("\"totalSize\"")).isGreaterThan(jsonRpcJson.indexOf("\"pageSize\""));
    }

    @Test
    void defaultsMissingQueryToAllTasks() {
        store.create(task("task-1", "context-1", "tenant-a"));

        WayangA2aTaskListView view = WayangA2aTaskListView.fromStore(store, null);

        assertThat(view.toHttpMap()).containsEntry("taskCount", 1);
        assertThat(view.toJsonRpcMap()).containsEntry("pageSize", WayangA2aTaskQuery.DEFAULT_LIMIT);
    }

    private static A2aTask task(String id, String contextId, String tenant) {
        return new A2aTask(
                id,
                contextId,
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }
}
