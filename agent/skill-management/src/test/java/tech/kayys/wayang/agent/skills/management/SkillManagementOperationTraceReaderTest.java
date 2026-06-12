package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.OPERATION_ID;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.PARENT_OPERATION_ID;

class SkillManagementOperationTraceReaderTest {

    @Test
    void readsRootAndChildEventsFromEventHistory() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                true,
                Map.of(
                        OPERATION_ID, "maintenance-1",
                        PARENT_OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.RECONCILE_LIFECYCLE,
                false,
                Map.of(
                        OPERATION_ID, "reconcile-1",
                        PARENT_OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                true,
                Map.of(
                        OPERATION_ID, "maintenance-2",
                        PARENT_OPERATION_ID, "deployment-2")));

        SkillManagementAdminOperationTrace trace = reader.trace(" deployment-1 ", 10);

        assertThat(trace.operationId()).isEqualTo("deployment-1");
        assertThat(trace.rootEventAvailable()).isTrue();
        assertThat(trace.totalEvents()).isEqualTo(3);
        assertThat(trace.successfulEvents()).isEqualTo(2);
        assertThat(trace.failedEvents()).isEqualTo(1);
        assertThat(trace.healthy()).isFalse();
        assertThat(trace.failed()).isTrue();
        assertThat(trace.failedChildEvents()).isEqualTo(1);
        assertThat(trace.status()).isEqualTo(SkillManagementOperationTraceStatus.FAILED.name());
        assertThat(trace.summary().operationCounts())
                .containsEntry("DEPLOYMENT", 1)
                .containsEntry("MAINTENANCE", 1)
                .containsEntry("RECONCILE_LIFECYCLE", 1);
        assertThat(trace.childEvents()).extracting(SkillManagementAdminEvent::operation)
                .containsExactly("MAINTENANCE", "RECONCILE_LIFECYCLE");
    }

    @Test
    void returnsEmptyTraceForBlankOperationId() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));

        SkillManagementAdminOperationTrace trace = reader.trace(" ", 10);

        assertThat(trace.operationId()).isEmpty();
        assertThat(trace.rootEventAvailable()).isFalse();
        assertThat(trace.totalEvents()).isZero();
        assertThat(trace.healthy()).isFalse();
        assertThat(trace.failed()).isFalse();
        assertThat(trace.failedChildEvents()).isZero();
        assertThat(trace.status()).isEqualTo(SkillManagementOperationTraceStatus.MISSING_OPERATION_ID.name());
        assertThat(trace.summary().totalEvents()).isZero();
        assertThat(trace.childEvents()).isEmpty();
    }

    @Test
    void marksTraceHealthyWhenRootAndChildrenSucceed() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                true,
                Map.of(
                        OPERATION_ID, "maintenance-1",
                        PARENT_OPERATION_ID, "deployment-1")));

        SkillManagementAdminOperationTrace trace = reader.trace("deployment-1", 10);

        assertThat(trace.healthy()).isTrue();
        assertThat(trace.failed()).isFalse();
        assertThat(trace.failedChildEvents()).isZero();
        assertThat(trace.status()).isEqualTo(SkillManagementOperationTraceStatus.HEALTHY.name());
        assertThat(trace.successfulEvents()).isEqualTo(2);
    }

    @Test
    void marksTraceRootMissingWhenOnlyChildrenAreFound() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                true,
                Map.of(
                        OPERATION_ID, "maintenance-1",
                        PARENT_OPERATION_ID, "deployment-1")));

        SkillManagementAdminOperationTrace trace = reader.trace("deployment-1", 10);

        assertThat(trace.rootEventAvailable()).isFalse();
        assertThat(trace.childEventCount()).isEqualTo(1);
        assertThat(trace.healthy()).isFalse();
        assertThat(trace.failed()).isFalse();
        assertThat(trace.status()).isEqualTo(SkillManagementOperationTraceStatus.ROOT_MISSING.name());
    }

    @Test
    void readsRecentDeploymentTracePage() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                false,
                Map.of(OPERATION_ID, "deployment-2")));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-3")));
        events.record(event(
                SkillManagementEventOperation.MAINTENANCE,
                true,
                Map.of(
                        OPERATION_ID, "maintenance-3",
                        PARENT_OPERATION_ID, "deployment-3")));

        SkillManagementAdminOperationTracePage page = reader.deploymentTraces(2, 10);

        assertThat(page.matchedRootEvents()).isEqualTo(3);
        assertThat(page.returnedRootEvents()).isEqualTo(2);
        assertThat(page.traceableRootEvents()).isEqualTo(2);
        assertThat(page.untraceableRootEvents()).isZero();
        assertThat(page.filteredTraces()).isZero();
        assertThat(page.returnedTraces()).isEqualTo(2);
        assertThat(page.truncated()).isTrue();
        assertThat(page.childEventLimit()).isEqualTo(10);
        assertThat(page.statusFilter()).isEmpty();
        assertThat(page.healthyTraces()).isEqualTo(1);
        assertThat(page.failedTraces()).isEqualTo(1);
        assertThat(page.rootMissingTraces()).isZero();
        assertThat(page.missingOperationIdTraces()).isZero();
        assertThat(page.traces()).extracting(SkillManagementAdminOperationTrace::operationId)
                .containsExactly("deployment-2", "deployment-3");
        assertThat(page.traces()).extracting(SkillManagementAdminOperationTrace::status)
                .containsExactly(
                        SkillManagementOperationTraceStatus.FAILED.name(),
                        SkillManagementOperationTraceStatus.HEALTHY.name());
        assertThat(page.traces().get(1).childEventCount()).isEqualTo(1);
    }

    @Test
    void filtersRecentDeploymentTracePageByStatus() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                false,
                Map.of(OPERATION_ID, "deployment-2")));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-3")));

        SkillManagementAdminOperationTracePage page = reader.deploymentTraces(
                SkillManagementOperationTraceQuery.deploymentsByStatus(
                        10,
                        10,
                        SkillManagementOperationTraceStatus.FAILED));

        assertThat(page.matchedRootEvents()).isEqualTo(3);
        assertThat(page.returnedRootEvents()).isEqualTo(3);
        assertThat(page.traceableRootEvents()).isEqualTo(3);
        assertThat(page.untraceableRootEvents()).isZero();
        assertThat(page.filteredTraces()).isEqualTo(2);
        assertThat(page.returnedTraces()).isEqualTo(1);
        assertThat(page.truncated()).isFalse();
        assertThat(page.statusFilter()).isEqualTo(SkillManagementOperationTraceStatus.FAILED.name());
        assertThat(page.healthyTraces()).isZero();
        assertThat(page.failedTraces()).isEqualTo(1);
        assertThat(page.traces()).extracting(SkillManagementAdminOperationTrace::operationId)
                .containsExactly("deployment-2");
    }

    @Test
    void reportsUntraceableDeploymentRootsWithoutOperationIds() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementOperationTraceReader reader =
                new SkillManagementOperationTraceReader(new SkillManagementEventHistory(events, events));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of()));
        events.record(event(
                SkillManagementEventOperation.DEPLOYMENT,
                true,
                Map.of(OPERATION_ID, "deployment-1")));

        SkillManagementAdminOperationTracePage page = reader.deploymentTraces(10, 10);

        assertThat(page.matchedRootEvents()).isEqualTo(2);
        assertThat(page.returnedRootEvents()).isEqualTo(2);
        assertThat(page.traceableRootEvents()).isEqualTo(1);
        assertThat(page.untraceableRootEvents()).isEqualTo(1);
        assertThat(page.filteredTraces()).isZero();
        assertThat(page.returnedTraces()).isEqualTo(1);
        assertThat(page.traces()).extracting(SkillManagementAdminOperationTrace::operationId)
                .containsExactly("deployment-1");
    }

    private SkillManagementEvent event(
            SkillManagementEventOperation operation,
            boolean success,
            Map<String, String> attributes) {
        return new SkillManagementEvent(
                Instant.parse("2026-01-01T00:00:00Z"),
                operation,
                "",
                success,
                attributes);
    }
}
