package tech.kayys.wayang.agent.skills.management;

/**
 * Top-level deployable skill-management configuration.
 */
public record SkillManagementDeploymentConfig(
        SkillManagementServiceConfig serviceConfig,
        SkillManagementMaintenanceSourceConfig maintenanceSource,
        SkillManagementMaintenancePlan maintenancePlan) {

    public SkillManagementDeploymentConfig {
        serviceConfig = SkillManagementConfigResolution.serviceConfig(serviceConfig);
        maintenanceSource = SkillManagementConfigResolution.maintenanceSource(maintenanceSource);
        maintenancePlan = SkillManagementConfigResolution.maintenancePlan(maintenancePlan);
        validate(serviceConfig, maintenanceSource).throwIfInvalid();
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(serviceConfig, maintenanceSource);
    }

    public SkillPersistenceStrategySummary persistenceStrategy() {
        return serviceConfig.persistenceStrategy();
    }

    public static SkillStoreConfigValidationResult validate(
            SkillManagementServiceConfig serviceConfig,
            SkillManagementMaintenanceSourceConfig maintenanceSource) {
        return SkillStoreConfigValidationResult.combine(
                serviceConfig == null ? SkillStoreConfigValidationResult.valid() : serviceConfig.validate(),
                maintenanceSource == null ? SkillStoreConfigValidationResult.valid() : maintenanceSource.validate());
    }

    public static SkillManagementDeploymentConfig defaults() {
        return new SkillManagementDeploymentConfig(
                SkillManagementServiceConfig.defaults(),
                SkillManagementMaintenanceSourceConfig.none(),
                SkillManagementMaintenancePlan.bootstrap());
    }

    public static SkillManagementDeploymentConfig of(
            SkillManagementServiceConfig serviceConfig,
            SkillManagementMaintenanceSourceConfig maintenanceSource,
            SkillManagementMaintenancePlan maintenancePlan) {
        return new SkillManagementDeploymentConfig(serviceConfig, maintenanceSource, maintenancePlan);
    }
}
