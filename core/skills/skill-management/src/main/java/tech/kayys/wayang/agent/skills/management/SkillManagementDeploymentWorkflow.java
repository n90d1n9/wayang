package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runs deployment preflight, maintenance, service assembly, and deployment events.
 */
final class SkillManagementDeploymentWorkflow {

    private final SkillManagementStoreBundleFactory storeBundleFactory;
    private final SkillManagementPreflightService preflightService;
    private final SkillManagementServiceAssembler serviceAssembler;
    private final SkillManagementMaintenanceExecution maintenanceExecution;

    SkillManagementDeploymentWorkflow(
            SkillManagementStoreBundleFactory storeBundleFactory,
            SkillManagementPreflightService preflightService,
            SkillManagementServiceAssembler serviceAssembler,
            SkillManagementMaintenanceExecution maintenanceExecution) {
        this.storeBundleFactory = Objects.requireNonNull(storeBundleFactory, "storeBundleFactory");
        this.preflightService = Objects.requireNonNull(preflightService, "preflightService");
        this.serviceAssembler = Objects.requireNonNull(serviceAssembler, "serviceAssembler");
        this.maintenanceExecution = Objects.requireNonNull(maintenanceExecution, "maintenanceExecution");
    }

    SkillManagementDeploymentResult deploy(SkillManagementDeploymentConfig config) {
        SkillManagementOperationContext deploymentContext = SkillManagementOperationContext.root();
        SkillManagementDeploymentPreflightReport preflight = preflightService.preflight(config);
        SkillManagementPreflightEnforcer.enforce(
                SkillManagementEventOperation.DEPLOYMENT,
                preflight,
                storeBundleFactory.eventSinkOverride(),
                deploymentContext);
        SkillManagementDeploymentConfig resolved = preflight.config();
        SkillManagementStoreBundle targetStores = storeBundleFactory.create(resolved.serviceConfig());
        SkillManagementEventRecorder eventRecorder = new SkillManagementEventRecorder(targetStores.eventSink());
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                deploymentContext,
                () -> {
                    SkillManagementMaintenanceResult maintenanceResult = maintenanceExecution.run(
                            targetStores,
                            resolved.maintenanceSource(),
                            resolved.maintenancePlan(),
                            deploymentContext.child());
                    return new SkillManagementDeploymentResult(
                            serviceAssembler.service(targetStores),
                            resolved,
                            maintenanceResult);
                },
                SkillManagementEventAttributes::deployment);
    }
}
