package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Source-compatible facade for stable skill-management admin DTO projections.
 *
 * <p>New mapping logic should live in the focused mapper classes. This class
 * keeps the historical public entry points stable while delegating to those
 * smaller projection boundaries.
 */
public final class SkillManagementAdminViews {

    private SkillManagementAdminViews() {
    }

    public static SkillManagementAdminInspection inspection(SkillManagementInspection inspection) {
        return SkillManagementAdminInspectionViews.inspection(inspection);
    }

    public static SkillManagementAdminBootstrapReport bootstrap(SkillManagementBootstrapResult result) {
        return SkillManagementAdminInspectionViews.bootstrap(result);
    }

    public static SkillManagementAdminMaintenanceReport maintenance(SkillManagementMaintenanceResult result) {
        return SkillManagementAdminDeploymentViews.maintenance(result);
    }

    public static SkillManagementAdminDeploymentReport deployment(SkillManagementDeploymentResult result) {
        return SkillManagementAdminDeploymentViews.deployment(result);
    }

    public static List<SkillManagementAdminMaintenanceStepReport> maintenanceSteps(
            SkillManagementMaintenanceResult result) {
        return maintenance(result).steps();
    }

    public static SkillManagementAdminMaintenanceStepReport maintenanceStep(
            SkillManagementMaintenanceStepDiagnostic diagnostic) {
        return SkillManagementAdminDeploymentViews.maintenanceStep(diagnostic);
    }

    public static SkillManagementAdminDeploymentPreflightReport deploymentPreflight(
            SkillManagementDeploymentPreflightReport report) {
        return SkillManagementAdminDeploymentViews.deploymentPreflight(report);
    }

    public static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillManagementServiceConfig config) {
        return SkillManagementAdminPersistenceViews.persistenceStrategy(config);
    }

    public static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillPersistenceContractMatrix matrix) {
        return SkillManagementAdminPersistenceViews.persistenceStrategy(matrix);
    }

    public static SkillManagementAdminPersistenceStrategy persistenceStrategy(
            SkillPersistenceStrategySummary summary) {
        return SkillManagementAdminPersistenceViews.persistenceStrategy(summary);
    }

    public static SkillManagementAdminPersistenceProfileCatalog persistenceProfiles() {
        return SkillManagementAdminPersistenceViews.persistenceProfiles();
    }

    public static SkillManagementAdminPersistenceProfile persistenceProfile(
            SkillManagementServiceProfile profile) {
        return SkillManagementAdminPersistenceViews.persistenceProfile(profile);
    }

    public static SkillManagementAdminPersistenceProfile persistenceProfile(String profileName) {
        return SkillManagementAdminPersistenceViews.persistenceProfile(profileName);
    }

    public static SkillManagementAdminDeploymentHistoryPage deploymentHistory(
            SkillManagementEventPage page) {
        return SkillManagementAdminDeploymentHistoryViews.deploymentHistory(page);
    }

    public static SkillManagementAdminDeploymentHistoryEntry deploymentHistoryEntry(
            SkillManagementEvent event) {
        return SkillManagementAdminDeploymentHistoryViews.deploymentHistoryEntry(event);
    }

    public static SkillManagementAdminDefinitionSyncStatus definitionSync(
            SkillDefinitionStoreSyncResult result) {
        return SkillManagementAdminSyncViews.definitionSync(result);
    }

    public static SkillManagementAdminSyncChange definitionSyncChange(
            SkillDefinitionStoreSyncChange change) {
        return SkillManagementAdminSyncViews.definitionSyncChange(change);
    }

    public static SkillManagementAdminArtifactSyncStatus artifactSync(
            SkillArtifactStoreSyncResult result) {
        return SkillManagementAdminSyncViews.artifactSync(result);
    }

    public static SkillManagementAdminArtifactSyncChange artifactSyncChange(
            SkillArtifactStoreSyncChange change) {
        return SkillManagementAdminSyncViews.artifactSyncChange(change);
    }

    public static SkillManagementAdminEventPage eventPage(SkillManagementEventPage page) {
        return SkillManagementAdminEventViews.eventPage(page);
    }

    public static SkillManagementAdminOperationTrace operationTrace(
            String operationId,
            SkillManagementEventPage page) {
        return SkillManagementAdminOperationTraceViews.operationTrace(operationId, page);
    }

    public static SkillManagementAdminOperationTrace operationTrace(
            String operationId,
            SkillManagementEventPage rootPage,
            SkillManagementEventPage childPage) {
        return SkillManagementAdminOperationTraceViews.operationTrace(operationId, rootPage, childPage);
    }

    public static SkillManagementAdminEventPruneReport eventPrune(SkillManagementEventPruneResult result) {
        return SkillManagementAdminEventPruneViews.eventPrune(result);
    }

    public static SkillManagementAdminEvent event(SkillManagementEvent event) {
        return SkillManagementAdminEventViews.event(event);
    }

    public static SkillManagementAdminEventSummary eventSummary(SkillManagementEventSummary summary) {
        return SkillManagementAdminEventViews.eventSummary(summary);
    }

    public static SkillManagementAdminValidationReport validation(SkillStoreConfigValidationResult result) {
        return SkillManagementAdminDeploymentViews.validation(result);
    }

    public static SkillManagementAdminStoreStatus definitionStore(SkillDefinitionStoreInspection inspection) {
        return SkillManagementAdminStoreViews.definitionStore(inspection);
    }

    public static SkillManagementAdminStoreStatus lifecycleStore(SkillLifecycleStateStoreInspection inspection) {
        return SkillManagementAdminStoreViews.lifecycleStore(inspection);
    }

    public static SkillManagementAdminStoreStatus eventStore(SkillManagementEventStoreInspection inspection) {
        return SkillManagementAdminStoreViews.eventStore(inspection);
    }

    public static SkillManagementAdminStoreStatus artifactStore(SkillArtifactStoreInspection inspection) {
        return SkillManagementAdminStoreViews.artifactStore(inspection);
    }

    public static SkillManagementAdminReconcileStatus reconcile(
            SkillLifecycleStateReconcileResult result,
            String failure) {
        return SkillManagementAdminInspectionViews.reconcile(result, failure);
    }

}
