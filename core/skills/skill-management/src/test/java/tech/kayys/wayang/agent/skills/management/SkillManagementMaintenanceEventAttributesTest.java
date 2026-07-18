package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementMaintenanceEventAttributesTest {

    @Test
    void maintenanceProjectsRunSummaryAndContext() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlan.inspectOnly()
                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1));
        SkillManagementMaintenanceResult result = maintenanceResult(
                SkillManagementEventPruneResult.success(
                        SkillManagementEventPruneOptions.keepLatest(1),
                        3,
                        List.of("event-1")));

        Map<String, String> attributes = SkillManagementMaintenanceEventAttributes.maintenance(
                result,
                plan,
                new SkillManagementOperationContext("maintenance-1", "deployment-1"));

        assertThat(attributes)
                .containsEntry("dryRun", "false")
                .containsEntry("changed", "true")
                .containsEntry("consistent", "false")
                .containsEntry("definitionChanged", "true")
                .containsEntry("definitionChanges", "1")
                .containsEntry("artifactChanged", "true")
                .containsEntry("artifactChanges", "1")
                .containsEntry("artifactConflicts", "0")
                .containsEntry("lifecycleCreated", "1")
                .containsEntry("lifecycleRemoved", "1")
                .containsEntry("eventPruneEnabled", "true")
                .containsEntry("eventPruneSkipped", "false")
                .containsEntry("eventPruneChanged", "true")
                .containsEntry("eventPruned", "1")
                .containsEntry("maintenanceStep.definition-sync.status", "CONFLICT")
                .containsEntry("maintenanceStep.definition-sync.changed", "true")
                .containsEntry("maintenanceStep.definition-sync.changes", "1")
                .containsEntry("maintenanceStep.definition-sync.conflicts", "1")
                .containsEntry("maintenanceStep.artifact-sync.status", "CHANGED")
                .containsEntry("maintenanceStep.lifecycle-reconcile.status", "CHANGED")
                .containsEntry("maintenanceStep.event-prune.status", "CHANGED")
                .containsEntry("maintenanceStep.event-prune.changes", "1")
                .containsEntry("operationId", "maintenance-1")
                .containsEntry("parentOperationId", "deployment-1");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deploymentUsesConfiguredMaintenancePlan() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlan.inspectOnly()
                .withEventPruning(SkillManagementEventPruneOptions.keepLatest(1));
        SkillManagementDeploymentResult deployment = new SkillManagementDeploymentResult(
                TestSkillManagementAdminFixtures.service(),
                SkillManagementDeploymentConfig.of(
                        SkillManagementServiceConfig.defaults(),
                        SkillManagementMaintenanceSourceConfig.none(),
                        plan),
                maintenanceResult(SkillManagementEventPruneResult.skipped(plan.eventPrunePolicy().options())));

        Map<String, String> attributes = SkillManagementMaintenanceEventAttributes.deployment(deployment);

        assertThat(attributes)
                .containsEntry("eventPruneEnabled", "true")
                .containsEntry("eventPruneSkipped", "true")
                .containsEntry("eventPruned", "0");
    }

    private SkillManagementMaintenanceResult maintenanceResult(SkillManagementEventPruneResult pruneResult) {
        return new SkillManagementMaintenanceResult(
                new SkillDefinitionStoreSyncResult(
                        false,
                        List.of(
                                new SkillDefinitionStoreSyncChange(
                                        "planner",
                                        SkillDefinitionStoreSyncAction.COPIED,
                                        "copied"),
                                new SkillDefinitionStoreSyncChange(
                                        "legacy",
                                        SkillDefinitionStoreSyncAction.CONFLICT,
                                        "conflict"))),
                new SkillArtifactStoreSyncResult(
                        false,
                        List.of(new SkillArtifactStoreSyncChange(
                                SkillArtifactReference.resource("planner", "prompt", "v1"),
                                SkillArtifactStoreSyncAction.UPDATED,
                                "updated"))),
                new SkillLifecycleStateReconcileResult(
                        List.of("planner"),
                        List.of("legacy"),
                        List.of("planner"),
                        List.of("legacy"),
                        List.of("planner"),
                        List.of("legacy")),
                pruneResult);
    }
}
