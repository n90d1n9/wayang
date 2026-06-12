package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementConfigResolutionTest {

    @Test
    void defaultsNullTopLevelConfigs() {
        assertThat(SkillManagementConfigResolution.serviceConfig(null))
                .isEqualTo(SkillManagementServiceConfig.defaults());
        assertThat(SkillManagementConfigResolution.maintenanceSource(null))
                .isEqualTo(SkillManagementMaintenanceSourceConfig.none());
        assertThat(SkillManagementConfigResolution.maintenancePlan(null))
                .isEqualTo(SkillManagementMaintenancePlan.bootstrap());
        assertThat(SkillManagementConfigResolution.deploymentConfig(null))
                .isEqualTo(SkillManagementDeploymentConfig.defaults());
    }

    @Test
    void preservesProvidedTopLevelConfigs() {
        SkillManagementServiceConfig serviceConfig = SkillManagementServiceConfig.defaults();
        SkillManagementMaintenanceSourceConfig sourceConfig =
                SkillManagementMaintenanceSourceConfig.definitions(SkillDefinitionStoreConfig.registry());
        SkillManagementMaintenancePlan plan = SkillManagementMaintenancePlan.mirrorAndRepair();
        SkillManagementDeploymentConfig deploymentConfig =
                SkillManagementDeploymentConfig.of(serviceConfig, sourceConfig, plan);

        assertThat(SkillManagementConfigResolution.serviceConfig(serviceConfig)).isSameAs(serviceConfig);
        assertThat(SkillManagementConfigResolution.maintenanceSource(sourceConfig)).isSameAs(sourceConfig);
        assertThat(SkillManagementConfigResolution.maintenancePlan(plan)).isSameAs(plan);
        assertThat(SkillManagementConfigResolution.deploymentConfig(deploymentConfig)).isSameAs(deploymentConfig);
    }
}
