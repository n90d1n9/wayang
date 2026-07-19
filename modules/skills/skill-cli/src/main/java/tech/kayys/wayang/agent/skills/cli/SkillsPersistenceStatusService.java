package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminDeploymentPreflightReport;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;
import tech.kayys.wayang.agent.skills.management.SkillManagementDeploymentConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementMaintenancePlan;
import tech.kayys.wayang.agent.skills.management.SkillManagementMaintenanceSourceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import java.util.Objects;

final class SkillsPersistenceStatusService {

    private final SkillManagementServiceConfig defaultConfig;
    private final SkillManagementServiceFactory preflightFactory;

    SkillsPersistenceStatusService(
            SkillManagementServiceConfig defaultConfig,
            SkillManagementServiceFactory preflightFactory) {
        this.defaultConfig = defaultConfig == null
                ? SkillManagementServiceConfig.defaults()
                : defaultConfig;
        this.preflightFactory = Objects.requireNonNull(preflightFactory, "preflightFactory");
    }

    SkillsPersistenceStatusReport report(SkillsPersistenceStatusRequest request) {
        SkillsPersistenceStatusRequest resolved = request == null
                ? SkillsPersistenceStatusRequest.defaults()
                : request;
        SkillsPersistenceConfigSource source = SkillsPersistenceConfigSource.resolve(
                resolved.profileName(),
                resolved.runtimeConfig(),
                defaultConfig);
        return new SkillsPersistenceStatusReport(
                source.source(),
                source.profile(),
                source.runtime(),
                SkillManagementAdminViews.persistenceStrategy(source.config()),
                preflight(source.config(), resolved.includePreflight()),
                diagnostics(source.config(), resolved.includeDiagnostics()));
    }

    private SkillManagementAdminDeploymentPreflightReport preflight(
            SkillManagementServiceConfig config,
            boolean includePreflight) {
        if (!includePreflight) {
            return null;
        }
        return SkillManagementAdminViews.deploymentPreflight(preflightFactory.preflight(
                SkillManagementDeploymentConfig.of(
                        config,
                        SkillManagementMaintenanceSourceConfig.none(),
                        SkillManagementMaintenancePlan.bootstrap())));
    }

    private SkillsPersistenceConfigDiagnostics diagnostics(
            SkillManagementServiceConfig config,
            boolean includeDiagnostics) {
        return includeDiagnostics ? SkillsPersistenceConfigDiagnostics.from(config) : null;
    }
}
