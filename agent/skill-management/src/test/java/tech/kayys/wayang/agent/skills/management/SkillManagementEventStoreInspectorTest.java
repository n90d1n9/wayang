package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventStoreInspectorTest {

    private final SkillManagementEventStoreInspector inspector = new SkillManagementEventStoreInspector();

    @Test
    void reportsQueryableEventHistory() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        events.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        events.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventStoreInspection inspection = inspector.inspect("events", events);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.status()).isEqualTo(SkillManagementEventStoreHealthStatus.READY);
        assertThat(inspection.matchedEvents()).isEqualTo(2);
        assertThat(inspection.returnedEvents()).isEqualTo(2);
        assertThat(inspection.summary().failedEvents()).isEqualTo(1);
        assertThat(inspection.summary().operationCounts()).containsEntry("CREATE_SKILL", 1)
                .containsEntry("DELETE_SKILL", 1);
        assertThat(inspection.capabilities().names())
                .containsExactly("write", "query-events", "prune-events");
    }

    @Test
    void reportsUnavailableEventReaderWithoutThrowing() {
        SkillManagementEventReader reader = query -> {
            throw new IllegalStateException("event history unavailable");
        };

        SkillManagementEventStoreInspection inspection = inspector.inspect("events", reader);

        assertThat(inspection.ready()).isFalse();
        assertThat(inspection.status()).isEqualTo(SkillManagementEventStoreHealthStatus.UNAVAILABLE);
        assertThat(inspection.failure()).contains("event history unavailable");
        assertThat(inspection.summary().totalEvents()).isZero();
    }

    @Test
    void includesCompositeChildEventStoreInspections() {
        InMemorySkillManagementEventSink fallback = new InMemorySkillManagementEventSink();
        CompositeSkillManagementEventSink composite =
                new CompositeSkillManagementEventSink(new FailingReadableEventSink(), fallback);
        composite.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));

        SkillManagementEventStoreInspection inspection = inspector.inspect("events", composite);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.matchedEvents()).isEqualTo(1);
        assertThat(inspection.children()).extracting(SkillManagementEventStoreInspection::name)
                .containsExactly("primary", "fallback");
        assertThat(inspection.children().get(0).ready()).isFalse();
        assertThat(inspection.children().get(0).failure()).contains("primary unavailable");
        assertThat(inspection.children().get(0).capabilities().names())
                .containsExactly("write", "query-events");
        assertThat(inspection.children().get(1).ready()).isTrue();
        assertThat(inspection.children().get(1).matchedEvents()).isEqualTo(1);
        assertThat(inspection.capabilities().names())
                .containsExactly("write", "query-events", "composite");
    }

    @Test
    void reportsMirroredEventStoreCapabilitiesAndChildren() {
        InMemorySkillManagementEventSink fallback = new InMemorySkillManagementEventSink();
        MirroredSkillManagementEventSink mirrored =
                new MirroredSkillManagementEventSink(new FailingReadableEventSink(), fallback);
        mirrored.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));

        SkillManagementEventStoreInspection inspection = inspector.inspect("events", mirrored);

        assertThat(inspection.ready()).isTrue();
        assertThat(inspection.matchedEvents()).isEqualTo(1);
        assertThat(inspection.children()).extracting(SkillManagementEventStoreInspection::name)
                .containsExactly("primary", "fallback");
        assertThat(inspection.children().get(0).ready()).isFalse();
        assertThat(inspection.children().get(1).ready()).isTrue();
        assertThat(inspection.capabilities().names())
                .containsExactly(
                        "write",
                        "query-events",
                        "primary-fallback",
                        "mirror-write");
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                success,
                Map.of());
    }

    private static final class FailingReadableEventSink
            implements SkillManagementEventSink, SkillManagementEventReader {

        @Override
        public void record(SkillManagementEvent event) {
        }

        @Override
        public SkillManagementEventPage query(SkillManagementEventQuery query) {
            throw new IllegalStateException("primary unavailable");
        }
    }
}
