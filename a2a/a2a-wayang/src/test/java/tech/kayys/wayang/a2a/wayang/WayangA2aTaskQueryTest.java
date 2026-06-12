package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskQueryTest {

    @Test
    void matchesTasksThroughSharedTenantVisibilityRule() {
        WayangA2aTaskQuery tenantQuery = new WayangA2aTaskQuery(
                "tenant-a",
                "context-a",
                Set.of(A2aTaskState.TASK_STATE_WORKING),
                10);

        assertThat(tenantQuery.matches(task("task-a", "tenant-a"))).isTrue();
        assertThat(tenantQuery.matches(task("task-b", "tenant-b"))).isFalse();
        assertThat(WayangA2aTaskQuery.all().matches(task("task-b", "tenant-b"))).isTrue();
    }

    @Test
    void readsTenantFromAttributeMetadata() {
        WayangA2aTaskQuery query = WayangA2aTaskQuery.fromAttributes(Map.of(
                "metadata", Map.of("tenantId", "tenant-a"),
                "contextId", "context-a"));

        assertThat(query.tenant()).isEqualTo("tenant-a");
        assertThat(query.contextId()).isEqualTo("context-a");
    }

    private static A2aTask task(String id, String tenant) {
        return new A2aTask(
                id,
                "context-a",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING),
                List.of(),
                List.of(),
                Map.of("tenant", tenant));
    }
}
