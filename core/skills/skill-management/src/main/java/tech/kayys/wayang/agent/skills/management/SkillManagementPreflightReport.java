package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Operation-neutral readiness report for skill-management workflows.
 */
public record SkillManagementPreflightReport(
        SkillStoreConfigValidationResult configurationValidation,
        SkillStoreConfigValidationResult targetStoreValidation,
        SkillStoreConfigValidationResult sourceStoreValidation,
        SkillStoreConfigValidationResult capabilityValidation) {

    public static SkillManagementPreflightReport empty() {
        return new SkillManagementPreflightReport(null, null, null, null);
    }

    public static SkillManagementPreflightReport orEmpty(SkillManagementPreflightReport report) {
        return report == null ? empty() : report;
    }

    public SkillManagementPreflightReport {
        configurationValidation = normalize(configurationValidation);
        targetStoreValidation = normalize(targetStoreValidation);
        sourceStoreValidation = normalize(sourceStoreValidation);
        capabilityValidation = normalize(capabilityValidation);
    }

    public boolean ready() {
        return errors().isEmpty();
    }

    public List<String> errors() {
        return SkillStoreConfigValidationResult.combine(
                configurationValidation,
                targetStoreValidation,
                sourceStoreValidation,
                capabilityValidation)
                .errors();
    }

    public String errorsMessage() {
        return String.join("; ", errors());
    }

    public SkillManagementPreflightMatrix matrix(SkillManagementDeploymentConfig config) {
        return SkillManagementPreflightMatrix.from(config, this);
    }

    private static SkillStoreConfigValidationResult normalize(SkillStoreConfigValidationResult validation) {
        return validation == null ? SkillStoreConfigValidationResult.valid() : validation;
    }
}
