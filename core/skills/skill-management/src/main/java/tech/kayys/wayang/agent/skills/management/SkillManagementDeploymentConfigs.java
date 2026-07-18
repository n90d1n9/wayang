package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Properties;

/**
 * Parses full deployable skill-management configuration from one source.
 */
public final class SkillManagementDeploymentConfigs {

    private SkillManagementDeploymentConfigs() {
    }

    public static SkillManagementDeploymentConfig fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    public static SkillManagementDeploymentConfig fromSystemEnvironment() {
        return fromEnvironment(System.getenv());
    }

    public static SkillManagementDeploymentConfig fromProperties(Properties properties) {
        return SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfigs.fromProperties(properties),
                SkillManagementMaintenanceSourceConfigs.fromProperties(properties),
                SkillManagementMaintenancePlanConfigs.fromProperties(properties));
    }

    public static SkillStoreConfigValidationResult validateProperties(Properties properties) {
        return SkillStoreConfigValidationResult.combine(
                SkillManagementServiceConfigs.validateProperties(properties),
                SkillManagementMaintenanceSourceConfigs.validateProperties(properties),
                SkillManagementMaintenancePlanConfigs.validateProperties(properties));
    }

    public static SkillManagementDeploymentConfig fromMap(Map<String, ?> values) {
        return SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfigs.fromMap(values),
                SkillManagementMaintenanceSourceConfigs.fromMap(values),
                SkillManagementMaintenancePlanConfigs.fromMap(values));
    }

    public static SkillStoreConfigValidationResult validateMap(Map<String, ?> values) {
        return SkillStoreConfigValidationResult.combine(
                SkillManagementServiceConfigs.validateMap(values),
                SkillManagementMaintenanceSourceConfigs.validateMap(values),
                SkillManagementMaintenancePlanConfigs.validateMap(values));
    }

    public static SkillManagementDeploymentConfig fromEnvironment(Map<String, String> environment) {
        return SkillManagementDeploymentConfig.of(
                SkillManagementServiceConfigs.fromEnvironment(environment),
                SkillManagementMaintenanceSourceConfigs.fromEnvironment(environment),
                SkillManagementMaintenancePlanConfigs.fromEnvironment(environment));
    }

    public static SkillStoreConfigValidationResult validateEnvironment(Map<String, String> environment) {
        return SkillStoreConfigValidationResult.combine(
                SkillManagementServiceConfigs.validateEnvironment(environment),
                SkillManagementMaintenanceSourceConfigs.validateEnvironment(environment),
                SkillManagementMaintenancePlanConfigs.validateEnvironment(environment));
    }
}
