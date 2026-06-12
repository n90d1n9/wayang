package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runs configured maintenance and deployment workflows.
 */
final class SkillManagementWorkflowRunner {

    private final SkillManagementStoreBundleFactory storeBundleFactory;
    private final SkillManagementPreflightService preflightService;
    private final SkillManagementMaintenanceExecution maintenanceExecution;
    private final SkillManagementDeploymentWorkflow deploymentWorkflow;

    SkillManagementWorkflowRunner(
            SkillManagementStoreBundleFactory storeBundleFactory,
            SkillManagementPreflightService preflightService,
            SkillManagementServiceAssembler serviceAssembler) {
        this(storeBundleFactory, preflightService, serviceAssembler, new SkillManagementMaintenanceRunnerFactory());
    }

    SkillManagementWorkflowRunner(
            SkillManagementStoreBundleFactory storeBundleFactory,
            SkillManagementPreflightService preflightService,
            SkillManagementServiceAssembler serviceAssembler,
            SkillManagementMaintenanceRunnerFactory maintenanceRunnerFactory) {
        this.storeBundleFactory = Objects.requireNonNull(storeBundleFactory, "storeBundleFactory");
        this.preflightService = Objects.requireNonNull(preflightService, "preflightService");
        SkillManagementServiceAssembler resolvedServiceAssembler =
                Objects.requireNonNull(serviceAssembler, "serviceAssembler");
        SkillManagementMaintenanceRunnerFactory resolvedMaintenanceRunnerFactory =
                Objects.requireNonNull(maintenanceRunnerFactory, "maintenanceRunnerFactory");
        this.maintenanceExecution = new SkillManagementMaintenanceExecution(
                this.storeBundleFactory,
                resolvedMaintenanceRunnerFactory);
        this.deploymentWorkflow = new SkillManagementDeploymentWorkflow(
                this.storeBundleFactory,
                this.preflightService,
                resolvedServiceAssembler,
                maintenanceExecution);
    }

    SkillManagementMaintenanceResult runMaintenance(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        SkillManagementDeploymentPreflightReport preflight = preflightService.preflight(config, sourceConfig, plan);
        SkillManagementPreflightEnforcer.enforce(
                SkillManagementEventOperation.MAINTENANCE,
                preflight,
                storeBundleFactory.eventSinkOverride(),
                context);
        SkillManagementDeploymentConfig resolved = preflight.config();
        return maintenanceExecution.run(
                storeBundleFactory.create(resolved.serviceConfig()),
                resolved.maintenanceSource(),
                resolved.maintenancePlan(),
                context);
    }

    SkillManagementMaintenanceResult runMaintenance(SkillManagementDeploymentConfig config) {
        SkillManagementDeploymentConfig resolved = SkillManagementConfigResolution.deploymentConfig(config);
        return runMaintenance(resolved.serviceConfig(), resolved.maintenanceSource(), resolved.maintenancePlan());
    }

    SkillManagementDeploymentResult deploy(SkillManagementDeploymentConfig config) {
        return deploymentWorkflow.deploy(config);
    }
}
