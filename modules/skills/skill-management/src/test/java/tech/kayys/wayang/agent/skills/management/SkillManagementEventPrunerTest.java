package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventPrunerTest {

    @Test
    void inMemoryPrunerSupportsDryRunAndApply() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        events.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner"));
        events.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner"));
        events.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner"));

        SkillManagementEventPruneResult preview = events.pruneEvents(SkillManagementEventPruneOptions.dryRun(1));

        assertThat(preview.dryRun()).isTrue();
        assertThat(preview.prunedEvents()).isEqualTo(2);
        assertThat(preview.changed()).isFalse();
        assertThat(events.events()).hasSize(3);

        SkillManagementEventPruneResult result = events.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.changed()).isTrue();
        assertThat(result.prunedEvents()).isEqualTo(2);
        assertThat(events.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void serviceExposesConfiguredEventHistoryPruner() {
        InMemorySkillManagementEventSink events = new InMemorySkillManagementEventSink();
        SkillManagementService service = new SkillManagementService(
                new TestSkillDefinitionStore(),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                events);
        events.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner"));
        events.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner"));

        SkillManagementEventPruneResult result = service.pruneEventHistory(
                        SkillManagementEventPruneOptions.keepLatest(1))
                .await().indefinitely();

        assertThat(result.changed()).isTrue();
        assertThat(service.eventHistory(SkillManagementEventQuery.latest()).await().indefinitely().events())
                .extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.UPDATE_SKILL);
    }

    @Test
    void compositePrunerRejectsUnsupportedSinksBeforeMutatingChildren() {
        InMemorySkillManagementEventSink primary = new InMemorySkillManagementEventSink();
        primary.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner"));
        primary.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner"));
        SkillManagementEventSink writeOnly = event -> {
        };
        CompositeSkillManagementEventSink composite = new CompositeSkillManagementEventSink(primary, writeOnly);

        assertThat(composite.supportsPruning()).isFalse();
        SkillManagementEventPruneResult result =
                composite.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.success()).isFalse();
        assertThat(result.changed()).isFalse();
        assertThat(result.children()).hasSize(2);
        assertThat(result.children().get(0).skipped()).isTrue();
        assertThat(result.children().get(1).failure()).contains("not supported");
        assertThat(primary.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.UPDATE_SKILL);
    }

    @Test
    void mirroredPrunerRejectsUnsupportedFallbackBeforeMutatingPrimary() {
        InMemorySkillManagementEventSink primary = new InMemorySkillManagementEventSink();
        primary.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner"));
        primary.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner"));
        SkillManagementEventSink writeOnlyFallback = event -> {
        };
        MirroredSkillManagementEventSink mirrored =
                new MirroredSkillManagementEventSink(primary, writeOnlyFallback);

        assertThat(mirrored.supportsPruning()).isFalse();
        SkillManagementEventPruneResult result =
                mirrored.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.success()).isFalse();
        assertThat(result.changed()).isFalse();
        assertThat(result.children()).hasSize(2);
        assertThat(result.children().get(0).skipped()).isTrue();
        assertThat(result.children().get(1).failure()).contains("not supported");
        assertThat(primary.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.UPDATE_SKILL);
    }

    @Test
    void adaptsEventSinksToPruners() {
        InMemorySkillManagementEventSink prunable = new InMemorySkillManagementEventSink();
        SkillManagementEventSink writeOnly = event -> {
        };

        assertThat(SkillManagementEventPruner.forSink(prunable)).isSameAs(prunable);
        assertThat(SkillManagementEventPruner.forSink(prunable).supportsPruning()).isTrue();
        assertThat(SkillManagementEventPruner.forSink(writeOnly).supportsPruning()).isFalse();
        SkillManagementEventPruneResult unsupported = SkillManagementEventPruner.forSink(writeOnly)
                .pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(unsupported.success()).isFalse();
        assertThat(unsupported.failure()).contains("not supported");
    }

    @Test
    void pruneResultNormalizesCountsReferencesAndChildren() {
        SkillManagementEventPruneResult child = SkillManagementEventPruneResult.success(
                SkillManagementEventPruneOptions.keepLatest(1),
                2,
                List.of("child-old"));

        SkillManagementEventPruneResult result = new SkillManagementEventPruneResult(
                false,
                false,
                -3,
                -2,
                -1,
                java.util.Arrays.asList("old-a", "", null, "old-b"),
                null,
                java.util.Arrays.asList(child, null));

        assertThat(result.keepLatestEvents()).isZero();
        assertThat(result.scannedEvents()).isEqualTo(2);
        assertThat(result.prunedEvents()).isEqualTo(2);
        assertThat(result.prunedEventReferences()).containsExactly("old-a", "old-b");
        assertThat(result.failure()).isEmpty();
        assertThat(result.children()).containsExactly(child);
        assertThat(result.success()).isTrue();
        assertThat(result.changed()).isTrue();
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                true,
                Map.of());
    }

}
