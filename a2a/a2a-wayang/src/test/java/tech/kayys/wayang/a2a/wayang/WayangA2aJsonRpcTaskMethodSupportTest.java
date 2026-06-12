package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcTaskMethodSupportTest {

    private final InMemoryWayangA2aTaskStore store = new InMemoryWayangA2aTaskStore();
    private final WayangA2aJsonRpcTaskMethodSupport support =
            WayangA2aJsonRpcTaskMethodSupport.fromStore(store);

    @Test
    void resolvesTaskForJsonRpcTenantScope() {
        store.create(task("task-a", "tenant-a"));
        store.create(task("task-b", "tenant-b"));

        assertThat(support.taskForRequest(request("req-a", Map.of("tenant", "tenant-a")), "task-a"))
                .isPresent();
        assertThat(support.taskForRequest(request("req-hidden", Map.of("tenant", "tenant-a")), "task-b"))
                .isEmpty();
    }

    @Test
    void buildsTaskNotFoundJsonRpcEnvelope() {
        WayangA2aHttpResponse response = support.taskNotFound(request("missing-1", Map.of()), "missing");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(error(response)).containsEntry("code", WayangA2aJsonRpcError.TASK_NOT_FOUND);
    }

    @Test
    void requiresStringParameters() {
        assertThat(support.requiredParam(Map.of("id", "task-1"), "id")).isEqualTo("task-1");
        assertThatThrownBy(() -> support.requiredParam(Map.of(), "id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is required");
    }

    private static WayangA2aJsonRpcRequest request(Object id, Map<String, Object> params) {
        return WayangA2aJsonRpcRequest.of(id, WayangA2aJsonRpcMethods.GET_TASK, params);
    }

    private static A2aTask task(String id, String tenant) {
        return new A2aTask(
                id,
                "context-1",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }

    private static Map<String, Object> error(WayangA2aHttpResponse response) {
        Object error = WayangA2aHttpJson.read(response.body()).get("error");
        assertThat(error).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) error);
    }
}
