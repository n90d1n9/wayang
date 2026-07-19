package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementMaintenanceWorkflowTest {

    @Test
    void runsDefinitionMaintenanceAgainstTargetStores() {
        TestSkillDefinitionStore sourceDefinitions = new TestSkillDefinitionStore();
        TestSkillDefinitionStore targetDefinitions = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycleStates = new InMemorySkillLifecycleStateStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        sourceDefinitions.registerSkill(TestSkillDefinitions.categorized("planner", "REASONING"));
        SkillManagementMaintenanceWorkflow workflow = workflow(
                targetDefinitions,
                lifecycleStates,
                new InMemorySkillArtifactStore(),
                eventSink);

        SkillManagementMaintenanceResult result = workflow.run(
                sourceDefinitions,
                SkillManagementMaintenancePlan.bootstrap());

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.artifactSyncResult().changes()).isEmpty();
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.consistent()).isTrue();
        assertThat(targetDefinitions.getSkill("planner")).isPresent();
        assertThat(lifecycleStates.snapshot()).containsOnlyKeys("planner");
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.MAINTENANCE);
    }

    @Test
    void runsArtifactAwareMaintenanceAgainstTargetStores() {
        TestSkillDefinitionStore sourceDefinitions = new TestSkillDefinitionStore();
        TestSkillDefinitionStore targetDefinitions = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore lifecycleStates = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore sourceArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore targetArtifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference stalePrompt = SkillArtifactReference.resource("old", "prompt", "v1");
        sourceDefinitions.registerSkill(TestSkillDefinitions.categorized("planner", "REASONING"));
        targetDefinitions.registerSkill(TestSkillDefinitions.categorized("old", "GENERAL"));
        lifecycleStates.save(SkillLifecycleState.created("old"));
        sourceArtifacts.putArtifact(SkillArtifact.text(prompt, "source prompt"));
        targetArtifacts.putArtifact(SkillArtifact.text(stalePrompt, "stale prompt"));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "old-event", true));
        eventSink.record(event(SkillManagementEventOperation.UPDATE_SKILL, "kept-event", true));
        SkillManagementMaintenanceWorkflow workflow = workflow(
                targetDefinitions,
                lifecycleStates,
                targetArtifacts,
                eventSink);

        SkillManagementMaintenanceResult result = workflow.run(
                sourceDefinitions,
                sourceArtifacts,
                SkillManagementMaintenancePlan.mirrorAndRepair()
                        .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1)));

        assertThat(result.definitionSyncResult().copied()).isEqualTo(1);
        assertThat(result.definitionSyncResult().deleted()).isEqualTo(1);
        assertThat(result.artifactSyncResult().copied()).isEqualTo(1);
        assertThat(result.artifactSyncResult().deleted()).isEqualTo(1);
        assertThat(result.lifecycleStateReconcileResult().createdStateSkillIds()).containsExactly("planner");
        assertThat(result.lifecycleStateReconcileResult().removedStateSkillIds()).containsExactly("old");
        assertThat(result.eventPruneResult().prunedEvents()).isEqualTo(1);
        assertThat(result.consistent()).isTrue();
        assertThat(targetDefinitions.getSkill("planner")).isPresent();
        assertThat(targetDefinitions.getSkill("old")).isEmpty();
        assertThat(targetArtifacts.getArtifact(prompt)).isPresent();
        assertThat(targetArtifacts.getArtifact(stalePrompt)).isEmpty();
        assertThat(lifecycleStates.snapshot()).containsOnlyKeys("planner");
        assertThat(eventSink.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.MAINTENANCE);
        assertThat(eventSink.events().get(0).skillId()).isEqualTo("kept-event");
        assertThat(eventSink.events().get(1).attributes())
                .containsEntry("artifactChanges", "2")
                .containsEntry("eventPruneEnabled", "true")
                .containsEntry("eventPruned", "1");
    }

    @Test
    void rejectsMissingSources() {
        TestSkillDefinitionStore sourceDefinitions = new TestSkillDefinitionStore();
        SkillManagementMaintenanceWorkflow workflow = workflow(
                new TestSkillDefinitionStore(),
                new InMemorySkillLifecycleStateStore(),
                new InMemorySkillArtifactStore(),
                new InMemorySkillManagementEventSink());

        assertThatThrownBy(() -> workflow.run(null, SkillManagementMaintenancePlan.bootstrap()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("sourceDefinitions");
        assertThatThrownBy(() -> workflow.run(
                sourceDefinitions,
                null,
                SkillManagementMaintenancePlan.bootstrap()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("sourceArtifacts");
    }

    private SkillManagementMaintenanceWorkflow workflow(
            SkillDefinitionStore targetDefinitions,
            SkillLifecycleStateStore lifecycleStates,
            SkillArtifactStore targetArtifacts,
            InMemorySkillManagementEventSink eventSink) {
        return new SkillManagementMaintenanceWorkflow(
                targetDefinitions,
                lifecycleStates,
                targetArtifacts,
                eventSink,
                SkillManagementEventPruner.forSink(eventSink),
                new SkillManagementMaintenanceRunnerFactory());
    }

    private SkillManagementEvent event(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.now(),
                operation,
                skillId,
                success,
                Map.of());
    }

}
