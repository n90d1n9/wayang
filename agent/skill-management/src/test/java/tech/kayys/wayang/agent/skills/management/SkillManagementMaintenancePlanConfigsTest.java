package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementMaintenancePlanConfigsTest {

    @Test
    void parsesMirrorAndRepairModeFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.maintenance.mode", "repair");

        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromProperties(properties);

        assertThat(plan.definitionSyncOptions().overwriteExisting()).isTrue();
        assertThat(plan.definitionSyncOptions().deleteMissingFromTarget()).isTrue();
        assertThat(plan.artifactSyncOptions().overwriteExisting()).isTrue();
        assertThat(plan.artifactSyncOptions().deleteMissingFromTarget()).isTrue();
        assertThat(plan.lifecycleStateReconcileOptions().createMissingStates()).isTrue();
        assertThat(plan.lifecycleStateReconcileOptions().removeOrphanedStates()).isTrue();
    }

    @Test
    void rootDryRunForcesPreviewPlan() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_MAINTENANCE_MODE", "repair",
                "WAYANG_SKILLS_MAINTENANCE_DRY_RUN", "true"));

        assertThat(plan.definitionSyncOptions().dryRun()).isTrue();
        assertThat(plan.artifactSyncOptions().dryRun()).isTrue();
        assertThat(plan.lifecycleStateReconcileOptions()).isEqualTo(SkillLifecycleStateReconcileOptions.inspectOnly());
        assertThat(plan.eventPrunePolicy().enabled()).isFalse();
    }

    @Test
    void nestedOverridesTuneDefinitionArtifactSyncAndLifecycleReconcile() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "maintenance", Map.of(
                                        "mode", "repair",
                                        "definition", Map.of(
                                                "sync", Map.of(
                                                        "delete-missing", "false",
                                                        "dry-run", "false")),
                                        "artifacts", Map.of(
                                                "sync", Map.of(
                                                        "mode", "mirror",
                                                        "delete-missing", "false",
                                                        "dry-run", "true")),
                                        "lifecycle", Map.of(
                                                "reconcile", Map.of(
                                                        "mode", "sync",
                                                        "remove-orphans", "false")))))));

        assertThat(plan.definitionSyncOptions().overwriteExisting()).isTrue();
        assertThat(plan.definitionSyncOptions().deleteMissingFromTarget()).isFalse();
        assertThat(plan.definitionSyncOptions().dryRun()).isFalse();
        assertThat(plan.artifactSyncOptions().overwriteExisting()).isTrue();
        assertThat(plan.artifactSyncOptions().deleteMissingFromTarget()).isFalse();
        assertThat(plan.artifactSyncOptions().dryRun()).isTrue();
        assertThat(plan.lifecycleStateReconcileOptions().createMissingStates()).isTrue();
        assertThat(plan.lifecycleStateReconcileOptions().removeOrphanedStates()).isFalse();
    }

    @Test
    void nestedEventPruneConfigEnablesMaintenanceEventCompaction() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "maintenance", Map.of(
                                        "mode", "repair",
                                        "events", Map.of(
                                                "prune", Map.of(
                                                        "enabled", "true",
                                                        "keep-latest-events", "25",
                                                        "dry-run", "true")))))));

        assertThat(plan.eventPrunePolicy().enabled()).isTrue();
        assertThat(plan.eventPrunePolicy().options().keepLatestEvents()).isEqualTo(25);
        assertThat(plan.eventPrunePolicy().options().dryRun()).isTrue();
    }

    @Test
    void rootDryRunForcesConfiguredEventPrunePreview() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_MAINTENANCE_MODE", "repair",
                "WAYANG_SKILLS_MAINTENANCE_DRY_RUN", "true",
                "WAYANG_SKILLS_MAINTENANCE_EVENTS_PRUNE_ENABLED", "true",
                "WAYANG_SKILLS_MAINTENANCE_EVENTS_PRUNE_KEEP_LATEST_EVENTS", "5"));

        assertThat(plan.eventPrunePolicy().enabled()).isTrue();
        assertThat(plan.eventPrunePolicy().options().keepLatestEvents()).isEqualTo(5);
        assertThat(plan.eventPrunePolicy().options().dryRun()).isTrue();
    }

    @Test
    void defaultsToBootstrapPlan() {
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlanConfigs.fromMap(Map.of());

        assertThat(plan.definitionSyncOptions()).isEqualTo(SkillDefinitionStoreSyncOptions.bootstrap());
        assertThat(plan.artifactSyncOptions()).isEqualTo(SkillArtifactStoreSyncOptions.bootstrap());
        assertThat(plan.lifecycleStateReconcileOptions()).isEqualTo(SkillLifecycleStateReconcileOptions.createMissing());
        assertThat(plan.eventPrunePolicy().enabled()).isFalse();
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> SkillManagementMaintenancePlanConfigs.fromMap(Map.of(
                "wayang.skills.maintenance.mode", "mystery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown skill maintenance mode");
    }

    @Test
    void validatesUnknownModeWithoutThrowing() {
        SkillStoreConfigValidationResult result = SkillManagementMaintenancePlanConfigs.validateMap(Map.of(
                "wayang.skills.maintenance.mode", "mystery"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("Unknown skill maintenance mode: mystery");
    }
}
