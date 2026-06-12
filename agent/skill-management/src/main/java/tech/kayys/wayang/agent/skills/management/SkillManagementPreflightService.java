package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Builds side-effect-free preflight reports for skill-management workflows.
 */
final class SkillManagementPreflightService {

    private final SkillManagementPreflightReportFactory reportFactory;

    SkillManagementPreflightService(SkillManagementStoreBundleFactory storeBundleFactory) {
        this(new SkillManagementPreflightReportFactory(storeBundleFactory));
    }

    private SkillManagementPreflightService(SkillManagementPreflightReportFactory reportFactory) {
        this.reportFactory = Objects.requireNonNull(reportFactory, "reportFactory");
    }

    SkillManagementDeploymentPreflightReport preflight(SkillManagementDeploymentConfig config) {
        return reportFactory.preflight(config);
    }

    SkillManagementDeploymentPreflightReport preflight(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return preflight(SkillManagementDeploymentConfig.of(config, sourceConfig, plan));
    }

    SkillManagementPreflightMatrix matrix(SkillManagementDeploymentConfig config) {
        return preflight(config).matrix();
    }

    SkillManagementPreflightMatrix matrix(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return matrix(SkillManagementDeploymentConfig.of(config, sourceConfig, plan));
    }

    SkillManagementPreflightReport validation(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return validation(SkillManagementDeploymentConfig.of(config, sourceConfig, plan));
    }

    SkillManagementPreflightReport validation(SkillManagementDeploymentConfig config) {
        return reportFactory.validation(config);
    }
}
