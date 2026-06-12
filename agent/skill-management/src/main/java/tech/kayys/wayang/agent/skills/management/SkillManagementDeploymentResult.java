package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Result of applying configured maintenance and exposing the managed service.
 */
public record SkillManagementDeploymentResult(
        SkillManagementService service,
        SkillManagementDeploymentConfig config,
        SkillManagementMaintenanceResult maintenanceResult) {

    public SkillManagementDeploymentResult {
        service = Objects.requireNonNull(service, "service");
        config = SkillManagementConfigResolution.deploymentConfig(config);
        maintenanceResult = Objects.requireNonNull(maintenanceResult, "maintenanceResult");
    }

    public boolean dryRun() {
        return maintenanceResult.dryRun();
    }

    public boolean changed() {
        return maintenanceResult.changed();
    }

    public boolean consistent() {
        return maintenanceResult.consistent();
    }
}
