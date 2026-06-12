package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Non-executing readiness report for a configured skill-management deployment.
 */
public record SkillManagementDeploymentPreflightReport(
        SkillManagementDeploymentConfig config,
        SkillStoreConfigValidationResult configurationValidation,
        SkillStoreConfigValidationResult targetStoreValidation,
        SkillStoreConfigValidationResult sourceStoreValidation,
        SkillStoreConfigValidationResult capabilityValidation) {

    public static SkillManagementDeploymentPreflightReport empty() {
        return new SkillManagementDeploymentPreflightReport(
                SkillManagementConfigResolution.deploymentConfig(null),
                SkillManagementPreflightReport.empty());
    }

    public static SkillManagementDeploymentPreflightReport orEmpty(
            SkillManagementDeploymentPreflightReport report) {
        return report == null ? empty() : report;
    }

    public SkillManagementDeploymentPreflightReport(
            SkillManagementDeploymentConfig config,
            SkillManagementPreflightReport preflight) {
        this(
                config,
                preflight == null ? null : preflight.configurationValidation(),
                preflight == null ? null : preflight.targetStoreValidation(),
                preflight == null ? null : preflight.sourceStoreValidation(),
                preflight == null ? null : preflight.capabilityValidation());
    }

    public SkillManagementDeploymentPreflightReport {
        config = SkillManagementConfigResolution.deploymentConfig(config);
        SkillManagementPreflightReport preflight = new SkillManagementPreflightReport(
                configurationValidation,
                targetStoreValidation,
                sourceStoreValidation,
                capabilityValidation);
        configurationValidation = preflight.configurationValidation();
        targetStoreValidation = preflight.targetStoreValidation();
        sourceStoreValidation = preflight.sourceStoreValidation();
        capabilityValidation = preflight.capabilityValidation();
    }

    public SkillManagementPreflightReport validation() {
        return new SkillManagementPreflightReport(
                configurationValidation,
                targetStoreValidation,
                sourceStoreValidation,
                capabilityValidation);
    }

    public boolean ready() {
        return validation().ready();
    }

    public boolean deployable() {
        return ready();
    }

    public List<String> errors() {
        return validation().errors();
    }

    public String errorsMessage() {
        return validation().errorsMessage();
    }

    public SkillManagementPreflightMatrix matrix() {
        return SkillManagementPreflightMatrix.from(this);
    }

    public SkillPersistenceStrategySummary persistenceStrategy() {
        return config.persistenceStrategy();
    }

    public void throwIfNotReady() {
        if (!ready()) {
            throw new SkillManagementDeploymentPreflightException(this);
        }
    }
}
