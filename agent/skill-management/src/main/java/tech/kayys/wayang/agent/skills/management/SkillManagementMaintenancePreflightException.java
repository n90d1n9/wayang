package tech.kayys.wayang.agent.skills.management;

/**
 * Raised when a maintenance plan requires capabilities the runner cannot provide.
 */
public class SkillManagementMaintenancePreflightException extends SkillManagementPreflightException {

    public SkillManagementMaintenancePreflightException(String message) {
        this(SkillStoreConfigValidationResult.error(message));
    }

    public SkillManagementMaintenancePreflightException(SkillStoreConfigValidationResult capabilityValidation) {
        this(new SkillManagementPreflightReport(
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.valid(),
                capabilityValidation));
    }

    public SkillManagementMaintenancePreflightException(SkillManagementPreflightReport preflightReport) {
        super(SkillManagementEventOperation.MAINTENANCE, preflightReport);
    }
}
