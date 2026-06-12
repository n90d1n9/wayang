package tech.kayys.wayang.agent.skills.management;

/**
 * Centralizes null-to-default policy for top-level skill-management configs.
 */
final class SkillManagementConfigResolution {

    private SkillManagementConfigResolution() {
    }

    static SkillManagementServiceConfig serviceConfig(SkillManagementServiceConfig config) {
        return config == null ? SkillManagementServiceConfig.defaults() : config;
    }

    static SkillManagementMaintenanceSourceConfig maintenanceSource(
            SkillManagementMaintenanceSourceConfig sourceConfig) {
        return sourceConfig == null ? SkillManagementMaintenanceSourceConfig.none() : sourceConfig;
    }

    static SkillManagementMaintenancePlan maintenancePlan(SkillManagementMaintenancePlan plan) {
        return plan == null ? SkillManagementMaintenancePlan.bootstrap() : plan;
    }

    static SkillManagementDeploymentConfig deploymentConfig(SkillManagementDeploymentConfig config) {
        return config == null ? SkillManagementDeploymentConfig.defaults() : config;
    }
}
