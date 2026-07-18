package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementMaintenanceRunnerTest {

    private final SkillManagementMaintenanceRunner runner = new SkillManagementMaintenanceRunner();

    @Test
    void bootstrapsDefinitionsAndCreatesLifecycleState() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        source.registerSkill(skill("planner"));

        SkillManagementMaintenanceResult result = runner.run(
                source,
                target,
                lifecycle,
                SkillManagementMaintenancePlan.bootstrap());

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.eventPruneResult().skipped()).isTrue();
        assertThat(result.changed()).isTrue();
        assertThat(result.consistent()).isTrue();
        assertThat(target.getSkill("planner")).isPresent();
        assertThat(lifecycle.snapshot()).containsOnlyKeys("planner");
    }

    @Test
    void dryRunReportsDefinitionChangesWithoutMutatingStores() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        source.registerSkill(skill("planner"));

        SkillManagementMaintenanceResult result = runner.run(
                source,
                target,
                lifecycle,
                SkillManagementMaintenancePlan.bootstrap().asDryRun());

        assertThat(result.dryRun()).isTrue();
        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.eventPruneResult().skipped()).isTrue();
        assertThat(target.getSkill("planner")).isEmpty();
        assertThat(lifecycle.snapshot()).isEmpty();
    }

    @Test
    void mirrorAndRepairPrunesDefinitionAndLifecycleOrphans() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        source.registerSkill(skill("planner"));
        target.registerSkill(skill("old"));
        lifecycle.save(SkillLifecycleState.created("old"));

        SkillManagementMaintenanceResult result = runner.run(
                source,
                target,
                lifecycle,
                SkillManagementMaintenancePlan.mirrorAndRepair());

        assertThat(result.definitionSyncResult().deleted()).isEqualTo(1);
        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.lifecycleStateReconcileResult().removedStateSkillIds()).containsExactly("old");
        assertThat(target.getSkill("old")).isEmpty();
        assertThat(target.getSkill("planner")).isPresent();
        assertThat(lifecycle.snapshot()).containsOnlyKeys("planner");
    }

    @Test
    void mirrorAndRepairSynchronizesArtifactsWhenStoresAreProvided() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference orphan = SkillArtifactReference.resource("old", "prompt", "v1");
        source.registerSkill(skill("planner"));
        sourceArtifacts.putArtifact(SkillArtifact.text(prompt, "source"));
        targetArtifacts.putArtifact(SkillArtifact.text(prompt, "target"));
        targetArtifacts.putArtifact(SkillArtifact.text(orphan, "remove"));

        SkillManagementMaintenanceResult result = runner.run(
                source,
                target,
                lifecycle,
                sourceArtifacts,
                targetArtifacts,
                SkillManagementMaintenancePlan.mirrorAndRepair());

        assertThat(result.artifactSyncResult().updated()).isEqualTo(1);
        assertThat(result.artifactSyncResult().deleted()).isEqualTo(1);
        assertThat(targetArtifacts.getArtifact(prompt)).hasValueSatisfying(artifact ->
                assertThat(new String(artifact.content(), java.nio.charset.StandardCharsets.UTF_8))
                        .isEqualTo("source"));
        assertThat(targetArtifacts.getArtifact(orphan)).isEmpty();
        assertThat(result.changed()).isTrue();
        assertThat(result.consistent()).isTrue();
    }

    @Test
    void emitsMaintenanceEventWithChangeSummary() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementMaintenanceRunner eventingRunner = new SkillManagementMaintenanceRunner(
                new SkillDefinitionStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                eventSink);
        source.registerSkill(skill("planner"));

        eventingRunner.run(source, target, lifecycle, SkillManagementMaintenancePlan.bootstrap());

        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent event = eventSink.events().get(0);
        assertThat(event.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(event.success()).isTrue();
        assertThat(event.attributes())
                .containsEntry("changed", "true")
                .containsEntry("definitionChanged", "true")
                .containsEntry("definitionChanges", "1")
                .containsEntry("artifactChanged", "false")
                .containsEntry("artifactChanges", "0")
                .containsEntry("lifecycleCreated", "1")
                .containsEntry("eventPruneSkipped", "true");
        assertThat(event.attributes().get("operationId")).isNotBlank();
        assertThat(event.attributes()).doesNotContainKey("parentOperationId");
    }

    @Test
    void maintenancePlanCanPruneEventHistory() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementMaintenanceRunner eventingRunner = new SkillManagementMaintenanceRunner(
                new SkillDefinitionStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                eventSink);
        eventSink.record(event("old-1", Instant.parse("2026-01-01T00:00:00Z")));
        eventSink.record(event("old-2", Instant.parse("2026-01-01T00:00:01Z")));
        eventSink.record(event("kept", Instant.parse("2026-01-01T00:00:02Z")));

        SkillManagementMaintenanceResult result = eventingRunner.run(
                source,
                target,
                lifecycle,
                SkillManagementMaintenancePlan.inspectOnly()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(result.eventPruneResult().skipped()).isFalse();
        assertThat(result.eventPruneResult().changed()).isTrue();
        assertThat(result.eventPruneResult().scannedEvents()).isEqualTo(3);
        assertThat(result.eventPruneResult().prunedEvents()).isEqualTo(2);
        assertThat(result.changed()).isTrue();
        assertThat(eventSink.events()).hasSize(2);
        assertThat(eventSink.events().get(0).skillId()).isEqualTo("kept");
        SkillManagementEvent maintenanceEvent = eventSink.events().get(1);
        assertThat(maintenanceEvent.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(maintenanceEvent.attributes())
                .containsEntry("eventPruneEnabled", "true")
                .containsEntry("eventPruneSkipped", "false")
                .containsEntry("eventPruneChanged", "true")
                .containsEntry("eventPruned", "2");
    }

    @Test
    void preflightReportsDefaultPlanReady() {
        SkillManagementPreflightReport report = runner.preflight(null);

        assertThat(report.ready()).isTrue();
        assertThat(report.errors()).isEmpty();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
    }

    @Test
    void preflightReportsUnsupportedEventPruneWithoutRecordingEvents() {
        RecordingWriteOnlyEventSink eventSink = new RecordingWriteOnlyEventSink();
        SkillManagementMaintenanceRunner eventingRunner = new SkillManagementMaintenanceRunner(
                new SkillDefinitionStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                eventSink);

        SkillManagementPreflightReport report = eventingRunner.preflight(
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(report.ready()).isFalse();
        assertThat(report.capabilityValidation().errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
        assertThat(eventSink.events()).isEmpty();
    }

    @Test
    void rejectsUnsupportedEventPruneBeforeMutatingStores() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        RecordingWriteOnlyEventSink eventSink = new RecordingWriteOnlyEventSink();
        SkillManagementMaintenanceRunner eventingRunner = new SkillManagementMaintenanceRunner(
                new SkillDefinitionStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                eventSink);
        source.registerSkill(skill("planner"));

        assertThatThrownBy(() -> eventingRunner.run(
                source,
                target,
                lifecycle,
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1))))
                .isInstanceOf(SkillManagementMaintenancePreflightException.class)
                .hasMessageContaining(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);

        assertThat(target.getSkill("planner")).isEmpty();
        assertThat(lifecycle.snapshot()).isEmpty();
        assertThat(eventSink.events()).hasSize(1);
        SkillManagementEvent failure = eventSink.events().get(0);
        assertThat(failure.operation()).isEqualTo(SkillManagementEventOperation.MAINTENANCE);
        assertThat(failure.success()).isFalse();
        assertThat(failure.attributes())
                .containsEntry("errorType", "SkillManagementMaintenancePreflightException")
                .containsEntry("preflightReady", "false")
                .containsEntry("preflightDeployable", "false")
                .containsEntry("preflightErrors", "1")
                .containsEntry("preflightConfigurationErrors", "0")
                .containsEntry("preflightTargetStoreErrors", "0")
                .containsEntry("preflightSourceStoreErrors", "0")
                .containsEntry("preflightCapabilityErrors", "1")
                .containsEntry(
                        "preflightCapabilityMessage",
                        SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED)
                .containsEntry(
                        "error",
                        "Skill-management maintenance preflight failed: "
                                + SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }

    private SkillDefinition skill(String id) {
        return TestSkillDefinitions.withMetadata(id, Map.of("version", "1"));
    }

    private SkillManagementEvent event(String skillId, Instant occurredAt) {
        return new SkillManagementEvent(
                occurredAt,
                SkillManagementEventOperation.UPDATE_SKILL,
                skillId,
                true,
                Map.of());
    }

    private static final class RecordingWriteOnlyEventSink implements SkillManagementEventSink {
        private final List<SkillManagementEvent> events = new java.util.ArrayList<>();

        @Override
        public void record(SkillManagementEvent event) {
            if (event != null) {
                events.add(event);
            }
        }

        List<SkillManagementEvent> events() {
            return List.copyOf(events);
        }
    }
}
