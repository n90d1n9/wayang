package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Maps aggregate inspection, bootstrap, and reconciliation diagnostics to stable admin DTOs.
 */
final class SkillManagementAdminInspectionViews {

    private SkillManagementAdminInspectionViews() {
    }

    static SkillManagementAdminInspection inspection(SkillManagementInspection inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return new SkillManagementAdminInspection(
                inspection.ready(),
                inspection.lifecycleStateConsistent(),
                SkillManagementAdminStoreViews.definitionStore(inspection.definitionStore()),
                SkillManagementAdminStoreViews.lifecycleStore(inspection.lifecycleStateStore()),
                SkillManagementAdminStoreViews.eventStore(inspection.eventStore()),
                SkillManagementAdminStoreViews.artifactStore(inspection.artifactStore()),
                reconcile(
                        inspection.lifecycleStateReconciliation(),
                        inspection.lifecycleStateReconciliationFailure()));
    }

    static SkillManagementAdminBootstrapReport bootstrap(SkillManagementBootstrapResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminBootstrapReport(
                inspection(result.initialInspection()),
                reconcile(result.lifecycleStateReconcileResult(), ""),
                inspection(result.finalInspection()));
    }

    static SkillManagementAdminReconcileStatus reconcile(
            SkillLifecycleStateReconcileResult result,
            String failure) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminReconcileStatus(
                result.definitionSkillIds(),
                result.persistedStateSkillIds(),
                result.missingStateSkillIds(),
                result.orphanedStateSkillIds(),
                result.createdStateSkillIds(),
                result.removedStateSkillIds(),
                failure);
    }
}
