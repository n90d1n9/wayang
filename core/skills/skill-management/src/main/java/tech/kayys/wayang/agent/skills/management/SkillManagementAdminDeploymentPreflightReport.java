package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;

/**
 * Stable admin-facing projection of deployment preflight readiness.
 */
public record SkillManagementAdminDeploymentPreflightReport(
        boolean ready,
        boolean deployable,
        int errorCount,
        String message,
        List<String> errors,
        SkillManagementAdminValidationReport configuration,
        SkillManagementAdminValidationReport targetStores,
        SkillManagementAdminValidationReport sourceStores,
        SkillManagementAdminValidationReport capabilities) {

    public SkillManagementAdminDeploymentPreflightReport(
            SkillManagementAdminValidationReport configuration,
            SkillManagementAdminValidationReport targetStores,
            SkillManagementAdminValidationReport sourceStores,
            SkillManagementAdminValidationReport capabilities) {
        this(false, false, 0, "", List.of(), configuration, targetStores, sourceStores, capabilities);
    }

    public SkillManagementAdminDeploymentPreflightReport {
        configuration = Objects.requireNonNull(configuration, "configuration");
        targetStores = Objects.requireNonNull(targetStores, "targetStores");
        sourceStores = Objects.requireNonNull(sourceStores, "sourceStores");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
        errors = SkillManagementAdminValueSupport.validationErrors(
                configuration,
                targetStores,
                sourceStores,
                capabilities);
        ready = errors.isEmpty();
        deployable = ready;
        errorCount = errors.size();
        message = SkillManagementAdminValueSupport.joinedMessage(errors);
    }
}
