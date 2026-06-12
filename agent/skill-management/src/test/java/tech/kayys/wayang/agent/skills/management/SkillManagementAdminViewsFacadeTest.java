package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminViewsFacadeTest {

    @Test
    void delegatesInspectionAndStoreProjectionsToFocusedMappers() {
        SkillLifecycleStateReconcileResult reconciliation = TestSkillManagementAdminFixtures.reconciliation();
        SkillManagementInspection inspection = TestSkillManagementAdminFixtures.inspection(reconciliation);
        SkillManagementBootstrapResult bootstrap = new SkillManagementBootstrapResult(
                TestSkillManagementAdminFixtures.service(),
                SkillManagementServiceConfig.defaults(),
                inspection,
                reconciliation,
                inspection);

        assertThat(SkillManagementAdminViews.inspection(inspection))
                .isEqualTo(SkillManagementAdminInspectionViews.inspection(inspection));
        assertThat(SkillManagementAdminViews.bootstrap(bootstrap))
                .isEqualTo(SkillManagementAdminInspectionViews.bootstrap(bootstrap));
        assertThat(SkillManagementAdminViews.reconcile(reconciliation, "state check failed"))
                .isEqualTo(SkillManagementAdminInspectionViews.reconcile(reconciliation, "state check failed"));
        assertThat(SkillManagementAdminViews.definitionStore(inspection.definitionStore()))
                .isEqualTo(SkillManagementAdminStoreViews.definitionStore(inspection.definitionStore()));
        assertThat(SkillManagementAdminViews.lifecycleStore(inspection.lifecycleStateStore()))
                .isEqualTo(SkillManagementAdminStoreViews.lifecycleStore(inspection.lifecycleStateStore()));
        assertThat(SkillManagementAdminViews.eventStore(inspection.eventStore()))
                .isEqualTo(SkillManagementAdminStoreViews.eventStore(inspection.eventStore()));
        assertThat(SkillManagementAdminViews.artifactStore(inspection.artifactStore()))
                .isEqualTo(SkillManagementAdminStoreViews.artifactStore(inspection.artifactStore()));
    }

    @Test
    void delegatesSyncAndDeploymentProjectionsToFocusedMappers() {
        SkillDefinitionStoreSyncChange definitionChange =
                TestSkillManagementAdminFixtures.copiedDefinitionChange();
        SkillDefinitionStoreSyncResult definitionSync = TestSkillManagementAdminFixtures.definitionSyncResult();
        SkillArtifactStoreSyncChange artifactChange =
                TestSkillManagementAdminFixtures.updatedPromptArtifactChange();
        SkillArtifactStoreSyncResult artifactSync = TestSkillManagementAdminFixtures.artifactSyncResult();
        SkillManagementMaintenanceResult maintenance = TestSkillManagementAdminFixtures.maintenanceResult(
                definitionSync,
                artifactSync,
                TestSkillManagementAdminFixtures.reconciliation());
        SkillManagementDeploymentResult deployment = TestSkillManagementAdminFixtures.deploymentResult(maintenance);
        SkillManagementDeploymentPreflightReport preflight = new SkillManagementDeploymentPreflightReport(
                SkillManagementDeploymentConfig.defaults(),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.error("target store unavailable"),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.valid());

        assertThat(SkillManagementAdminViews.definitionSync(definitionSync))
                .isEqualTo(SkillManagementAdminSyncViews.definitionSync(definitionSync));
        assertThat(SkillManagementAdminViews.definitionSyncChange(definitionChange))
                .isEqualTo(SkillManagementAdminSyncViews.definitionSyncChange(definitionChange));
        assertThat(SkillManagementAdminViews.artifactSync(artifactSync))
                .isEqualTo(SkillManagementAdminSyncViews.artifactSync(artifactSync));
        assertThat(SkillManagementAdminViews.artifactSyncChange(artifactChange))
                .isEqualTo(SkillManagementAdminSyncViews.artifactSyncChange(artifactChange));
        assertThat(SkillManagementAdminViews.maintenance(maintenance))
                .isEqualTo(SkillManagementAdminDeploymentViews.maintenance(maintenance));
        assertThat(SkillManagementAdminViews.maintenanceSteps(maintenance))
                .isEqualTo(SkillManagementAdminDeploymentViews.maintenanceSteps(maintenance.stepDiagnostics()));
        assertThat(SkillManagementAdminViews.maintenanceStep(maintenance.stepDiagnostics().get(0)))
                .isEqualTo(SkillManagementAdminDeploymentViews.maintenanceStep(maintenance.stepDiagnostics().get(0)));
        assertThat(SkillManagementAdminViews.deployment(deployment))
                .isEqualTo(SkillManagementAdminDeploymentViews.deployment(deployment));
        assertThat(SkillManagementAdminViews.deploymentPreflight(preflight))
                .isEqualTo(SkillManagementAdminDeploymentViews.deploymentPreflight(preflight));
        assertThat(SkillManagementAdminViews.validation(null))
                .isEqualTo(SkillManagementAdminDeploymentViews.validation(null));
        assertThat(SkillManagementAdminViews.persistenceStrategy(preflight.persistenceStrategy()))
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceStrategy(preflight.persistenceStrategy()));
        assertThat(SkillManagementAdminViews.persistenceStrategy(preflight.config().serviceConfig()))
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceStrategy(preflight.config().serviceConfig()));
        assertThat(SkillManagementAdminViews.persistenceStrategy(
                preflight.config().serviceConfig().persistenceContracts()))
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceStrategy(
                        preflight.config().serviceConfig().persistenceContracts()));
        assertThat(SkillManagementAdminViews.persistenceProfiles())
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceProfiles());
        assertThat(SkillManagementAdminViews.persistenceProfile(SkillManagementServiceProfile.OBJECT_STORAGE))
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceProfile(
                        SkillManagementServiceProfile.OBJECT_STORAGE));
        assertThat(SkillManagementAdminViews.persistenceProfile("rustfs"))
                .isEqualTo(SkillManagementAdminPersistenceViews.persistenceProfile("rustfs"));
    }

    @Test
    void delegatesEventHistoryAndTraceProjectionsToFocusedMappers() {
        SkillManagementEvent deployment = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", "deploy-1"));
        SkillManagementEvent child = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                false,
                Map.of(
                        "operationId", "maintenance-1",
                        "parentOperationId", "deploy-1"));
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(2, deployment, child);
        SkillManagementEventPruneResult prune = SkillManagementEventPruneResult.success(
                SkillManagementEventPruneOptions.keepLatest(1),
                2,
                List.of("old-event"));

        assertThat(SkillManagementAdminViews.eventPage(page))
                .isEqualTo(SkillManagementAdminEventViews.eventPage(page));
        assertThat(SkillManagementAdminViews.event(deployment))
                .isEqualTo(SkillManagementAdminEventViews.event(deployment));
        assertThat(SkillManagementAdminViews.eventSummary(page.summary()))
                .isEqualTo(SkillManagementAdminEventViews.eventSummary(page.summary()));
        assertThat(SkillManagementAdminViews.operationTrace("deploy-1", page))
                .isEqualTo(SkillManagementAdminOperationTraceViews.operationTrace("deploy-1", page));
        assertThat(SkillManagementAdminViews.operationTrace(
                "deploy-1",
                TestSkillManagementAdminFixtures.eventPage(1, deployment),
                TestSkillManagementAdminFixtures.eventPage(1, child)))
                .isEqualTo(SkillManagementAdminOperationTraceViews.operationTrace(
                        "deploy-1",
                        TestSkillManagementAdminFixtures.eventPage(1, deployment),
                        TestSkillManagementAdminFixtures.eventPage(1, child)));
        assertThat(SkillManagementAdminViews.deploymentHistory(page))
                .isEqualTo(SkillManagementAdminDeploymentHistoryViews.deploymentHistory(page));
        assertThat(SkillManagementAdminViews.deploymentHistoryEntry(deployment))
                .isEqualTo(SkillManagementAdminDeploymentHistoryViews.deploymentHistoryEntry(deployment));
        assertThat(SkillManagementAdminViews.eventPrune(prune))
                .isEqualTo(SkillManagementAdminEventPruneViews.eventPrune(prune));
    }
}
