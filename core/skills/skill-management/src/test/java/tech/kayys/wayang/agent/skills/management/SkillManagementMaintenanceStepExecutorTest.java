package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementMaintenanceStepExecutorTest {

    @Test
    void executesDefinitionArtifactLifecycleAndEventPruneSteps() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        source.registerSkill(skill("planner"));
        sourceArtifacts.putArtifact(SkillArtifact.text(prompt, "source"));
        eventSink.record(event("old-1", Instant.parse("2026-01-01T00:00:00Z")));
        eventSink.record(event("old-2", Instant.parse("2026-01-01T00:00:01Z")));
        SkillManagementMaintenanceStepExecutor executor = new SkillManagementMaintenanceStepExecutor(
                new SkillDefinitionStoreSynchronizer(),
                new SkillArtifactStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                SkillManagementEventPruner.forSink(eventSink));

        SkillManagementMaintenanceResult result = executor.execute(
                SkillManagementMaintenanceInputs.withArtifacts(
                        source,
                        target,
                        lifecycle,
                        sourceArtifacts,
                        targetArtifacts),
                SkillManagementMaintenancePlan.bootstrap()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.artifactSyncResult().copied()).isEqualTo(1);
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.eventPruneResult().prunedEvents()).isEqualTo(1);
        assertThat(target.getSkill("planner")).isPresent();
        assertThat(targetArtifacts.getArtifact(prompt)).isPresent();
        assertThat(lifecycle.snapshot()).containsOnlyKeys("planner");
        assertThat(eventSink.events()).hasSize(1);
    }

    @Test
    void skipsArtifactSyncWhenArtifactStoresAreUnavailable() {
        TestSkillDefinitionStore source = new TestSkillDefinitionStore();
        TestSkillDefinitionStore target = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycle = new InMemorySkillLifecycleStateStore();
        source.registerSkill(skill("planner"));
        SkillManagementMaintenanceStepExecutor executor = new SkillManagementMaintenanceStepExecutor(
                new SkillDefinitionStoreSynchronizer(),
                new SkillArtifactStoreSynchronizer(),
                new SkillLifecycleStateReconciler(),
                options -> SkillManagementEventPruneResult.failure(options, "unexpected pruning"));

        SkillManagementMaintenanceResult result = executor.execute(
                SkillManagementMaintenanceInputs.definitionsOnly(source, target, lifecycle),
                SkillManagementMaintenancePlan.bootstrap());

        assertThat(result.artifactSyncResult().dryRun()).isTrue();
        assertThat(result.artifactSyncResult().changes()).isEmpty();
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
}
