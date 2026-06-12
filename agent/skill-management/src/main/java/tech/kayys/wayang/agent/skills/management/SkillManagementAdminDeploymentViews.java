package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Maps maintenance and deployment diagnostics to stable admin DTOs.
 */
final class SkillManagementAdminDeploymentViews {

    private SkillManagementAdminDeploymentViews() {
    }

    static SkillManagementAdminMaintenanceReport maintenance(SkillManagementMaintenanceResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminMaintenanceReport(
                SkillManagementAdminSyncViews.definitionSync(result.definitionSyncResult()),
                SkillManagementAdminSyncViews.artifactSync(result.artifactSyncResult()),
                SkillManagementAdminInspectionViews.reconcile(result.lifecycleStateReconcileResult(), ""),
                SkillManagementAdminEventPruneViews.eventPrune(result.eventPruneResult()),
                maintenanceSteps(result.stepDiagnostics()));
    }

    static SkillManagementAdminDeploymentReport deployment(SkillManagementDeploymentResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminDeploymentReport(
                maintenance(result.maintenanceResult()));
    }

    static SkillManagementAdminDeploymentPreflightReport deploymentPreflight(
            SkillManagementDeploymentPreflightReport report) {
        Objects.requireNonNull(report, "report");
        return new SkillManagementAdminDeploymentPreflightReport(
                validation(report.configurationValidation()),
                validation(report.targetStoreValidation()),
                validation(report.sourceStoreValidation()),
                validation(report.capabilityValidation()));
    }

    static SkillManagementAdminValidationReport validation(SkillStoreConfigValidationResult result) {
        SkillStoreConfigValidationResult resolved =
                result == null ? SkillStoreConfigValidationResult.valid() : result;
        return new SkillManagementAdminValidationReport(resolved.errors());
    }

    static List<SkillManagementAdminMaintenanceStepReport> maintenanceSteps(
            List<SkillManagementMaintenanceStepDiagnostic> diagnostics) {
        return SkillManagementAdminValueSupport.nonNullList(diagnostics).stream()
                .map(SkillManagementAdminDeploymentViews::maintenanceStep)
                .toList();
    }

    static SkillManagementAdminMaintenanceStepReport maintenanceStep(
            SkillManagementMaintenanceStepDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        return new SkillManagementAdminMaintenanceStepReport(
                diagnostic.step().id(),
                diagnostic.status().name(),
                diagnostic.dryRun(),
                diagnostic.skipped(),
                diagnostic.changed(),
                diagnostic.consistent(),
                diagnostic.changes(),
                diagnostic.conflicts(),
                diagnostic.failure());
    }
}
