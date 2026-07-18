package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Builds side-effect-free preflight reports from resolved deployment inputs.
 */
final class SkillManagementPreflightReportFactory {

    private final SkillManagementStoreBundleFactory storeBundleFactory;

    SkillManagementPreflightReportFactory(SkillManagementStoreBundleFactory storeBundleFactory) {
        this.storeBundleFactory = Objects.requireNonNull(storeBundleFactory, "storeBundleFactory");
    }

    SkillManagementDeploymentPreflightReport preflight(SkillManagementDeploymentConfig config) {
        SkillManagementDeploymentConfig resolved = SkillManagementConfigResolution.deploymentConfig(config);
        return new SkillManagementDeploymentPreflightReport(
                resolved,
                validationResolved(resolved));
    }

    SkillManagementPreflightReport validation(SkillManagementDeploymentConfig config) {
        return validationResolved(SkillManagementConfigResolution.deploymentConfig(config));
    }

    private SkillManagementPreflightReport validationResolved(SkillManagementDeploymentConfig resolved) {
        return new SkillManagementPreflightReport(
                resolved.validate(),
                storeBundleFactory.validateManagedStores(resolved.serviceConfig()),
                storeBundleFactory.validateMaintenanceSources(resolved.maintenanceSource()),
                storeBundleFactory.validatePlanCapabilities(resolved.serviceConfig(), resolved.maintenancePlan()));
    }
}
