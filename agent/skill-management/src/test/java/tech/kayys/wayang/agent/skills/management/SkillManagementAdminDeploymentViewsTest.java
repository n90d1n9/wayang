package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentViewsTest {

    @Test
    void mapsMaintenanceAndDeploymentResultsToStableAdminProjection() {
        SkillManagementMaintenanceResult maintenanceResult =
                TestSkillManagementAdminFixtures.maintenanceResult();
        SkillManagementDeploymentResult deploymentResult =
                TestSkillManagementAdminFixtures.deploymentResult(maintenanceResult);

        SkillManagementAdminMaintenanceReport maintenance =
                SkillManagementAdminDeploymentViews.maintenance(maintenanceResult);
        SkillManagementAdminDeploymentReport deployment =
                SkillManagementAdminDeploymentViews.deployment(deploymentResult);

        assertThat(maintenance.dryRun()).isFalse();
        assertThat(maintenance.changed()).isTrue();
        assertThat(maintenance.consistent()).isTrue();
        assertThat(maintenance.definitionSync().copied()).isEqualTo(1);
        assertThat(maintenance.definitionSync().changes()).extracting(SkillManagementAdminSyncChange::skillId)
                .containsExactly("planner");
        assertThat(maintenance.artifactSync().updated()).isEqualTo(1);
        assertThat(maintenance.artifactSync().changes())
                .extracting(SkillManagementAdminArtifactSyncChange::artifactReference)
                .containsExactly("planner:resource:prompt:v1");
        assertThat(maintenance.lifecycleStateReconciliation().createdStateSkillIds())
                .containsExactly("planner");
        assertThat(maintenance.eventPrune().skipped()).isTrue();
        assertThat(maintenance.steps())
                .extracting(SkillManagementAdminMaintenanceStepReport::step)
                .containsExactly(
                        "definition-sync",
                        "artifact-sync",
                        "lifecycle-reconcile",
                        "event-prune");
        assertThat(maintenance.steps())
                .extracting(SkillManagementAdminMaintenanceStepReport::status)
                .containsExactly("CHANGED", "CHANGED", "CHANGED", "SKIPPED");
        assertThat(maintenance.steps().get(0).changes()).isEqualTo(1);
        assertThat(maintenance.steps().get(3).skipped()).isTrue();
        assertThat(deployment.maintenance()).isEqualTo(maintenance);
        assertThat(deployment.changed()).isTrue();
        assertThat(deployment.consistent()).isTrue();
    }

    @Test
    void mapsDeploymentPreflightToStableAdminProjection() {
        SkillManagementDeploymentPreflightReport report = new SkillManagementDeploymentPreflightReport(
                SkillManagementDeploymentConfig.defaults(),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.error("No custom artifact store registered for: target-artifacts"),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.error(
                        "Event history pruning requires an event store with capability: prune-events"));

        SkillManagementAdminDeploymentPreflightReport view =
                SkillManagementAdminDeploymentViews.deploymentPreflight(report);

        assertThat(view.ready()).isFalse();
        assertThat(view.deployable()).isFalse();
        assertThat(view.errorCount()).isEqualTo(2);
        assertThat(view.errors()).containsExactly(
                "No custom artifact store registered for: target-artifacts",
                "Event history pruning requires an event store with capability: prune-events");
        assertThat(view.configuration().valid()).isTrue();
        assertThat(view.targetStores().valid()).isFalse();
        assertThat(view.sourceStores().valid()).isTrue();
        assertThat(view.capabilities().valid()).isFalse();
    }

    @Test
    void maintenanceAndDeploymentReportsDeriveSummaryFromChildren() {
        SkillManagementAdminDefinitionSyncStatus definitionSync = new SkillManagementAdminDefinitionSyncStatus(
                true,
                List.of(new SkillManagementAdminSyncChange("planner", "COPIED", false, "")));
        SkillManagementAdminArtifactSyncStatus artifactSync = new SkillManagementAdminArtifactSyncStatus(
                true,
                List.of(new SkillManagementAdminArtifactSyncChange(
                        "planner:resource:prompt:v1",
                        "CONFLICT",
                        true,
                        "")));
        SkillManagementAdminReconcileStatus reconcile = new SkillManagementAdminReconcileStatus(
                List.of("planner"),
                List.of("planner"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "");
        SkillManagementAdminEventPruneReport eventPrune = new SkillManagementAdminEventPruneReport(
                true,
                false,
                1,
                0,
                List.of(),
                "",
                List.of());

        SkillManagementAdminMaintenanceReport maintenance = new SkillManagementAdminMaintenanceReport(
                false,
                false,
                true,
                definitionSync,
                artifactSync,
                reconcile,
                eventPrune);
        SkillManagementAdminDeploymentReport deployment = new SkillManagementAdminDeploymentReport(
                false,
                false,
                true,
                maintenance);

        assertThat(maintenance.dryRun()).isTrue();
        assertThat(maintenance.changed()).isTrue();
        assertThat(maintenance.consistent()).isFalse();
        assertThat(deployment.dryRun()).isTrue();
        assertThat(deployment.changed()).isTrue();
        assertThat(deployment.consistent()).isFalse();
    }

    @Test
    void validationReportDerivesSummaryFromNormalizedErrors() {
        SkillManagementAdminValidationReport view = new SkillManagementAdminValidationReport(
                true,
                99,
                "stale message",
                java.util.Arrays.asList("Config is missing", "", null, "Store is unavailable"));

        assertThat(view.valid()).isFalse();
        assertThat(view.errorCount()).isEqualTo(2);
        assertThat(view.message()).isEqualTo("Config is missing; Store is unavailable");
        assertThat(view.errors()).containsExactly("Config is missing", "Store is unavailable");
    }

    @Test
    void deploymentPreflightReportDerivesSummaryFromValidationBuckets() {
        SkillManagementAdminValidationReport valid = new SkillManagementAdminValidationReport(List.of());
        SkillManagementAdminValidationReport configuration = new SkillManagementAdminValidationReport(
                false,
                99,
                "stale configuration",
                List.of("Missing definition store config"));
        SkillManagementAdminValidationReport capabilities = new SkillManagementAdminValidationReport(
                true,
                99,
                "stale capability",
                List.of("Event store cannot prune history"));

        SkillManagementAdminDeploymentPreflightReport view =
                new SkillManagementAdminDeploymentPreflightReport(
                        true,
                        true,
                        99,
                        "stale preflight",
                        List.of("stale preflight error"),
                        configuration,
                        valid,
                        valid,
                        capabilities);

        assertThat(view.ready()).isFalse();
        assertThat(view.deployable()).isFalse();
        assertThat(view.errorCount()).isEqualTo(2);
        assertThat(view.message()).isEqualTo(
                "Missing definition store config; Event store cannot prune history");
        assertThat(view.errors()).containsExactly(
                "Missing definition store config",
                "Event store cannot prune history");
    }

    @Test
    void mapsNullValidationResultToEmptyAdminValidationReport() {
        SkillManagementAdminValidationReport view = SkillManagementAdminDeploymentViews.validation(null);

        assertThat(view.valid()).isTrue();
        assertThat(view.errorCount()).isZero();
        assertThat(view.message()).isBlank();
        assertThat(view.errors()).isEmpty();
    }

    @Test
    void maintenanceStepReportNormalizesPublicFields() {
        SkillManagementAdminMaintenanceStepReport report = new SkillManagementAdminMaintenanceStepReport(
                " ",
                "",
                false,
                false,
                false,
                false,
                -1,
                -2,
                null);

        assertThat(report.step()).isBlank();
        assertThat(report.status()).isEqualTo("UNKNOWN");
        assertThat(report.changes()).isZero();
        assertThat(report.conflicts()).isZero();
        assertThat(report.failure()).isBlank();
    }
}
