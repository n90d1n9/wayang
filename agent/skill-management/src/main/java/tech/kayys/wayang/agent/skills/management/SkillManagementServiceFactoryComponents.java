package tech.kayys.wayang.agent.skills.management;

/**
 * Runtime component graph used by the service factory facade.
 */
record SkillManagementServiceFactoryComponents(
        SkillManagementStoreBundleFactory storeBundleFactory,
        SkillManagementPreflightService preflightService,
        SkillManagementServiceAssembler serviceAssembler,
        SkillManagementWorkflowRunner workflowRunner) {

    static SkillManagementServiceFactoryComponents assemble(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        SkillManagementStoreBundleFactory storeBundleFactory = new SkillManagementStoreBundleFactory(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                eventStoreFactory,
                artifactStoreFactory,
                eventSinkOverride);
        SkillManagementPreflightService preflightService = new SkillManagementPreflightService(storeBundleFactory);
        SkillManagementServiceAssembler serviceAssembler = new SkillManagementServiceAssembler(
                definitionStoreInspector,
                lifecycleStateStoreInspector);
        SkillManagementWorkflowRunner workflowRunner = new SkillManagementWorkflowRunner(
                storeBundleFactory,
                preflightService,
                serviceAssembler);
        return new SkillManagementServiceFactoryComponents(
                storeBundleFactory,
                preflightService,
                serviceAssembler,
                workflowRunner);
    }

    SkillManagementService service(SkillManagementServiceConfig config) {
        return serviceAssembler.service(storeBundleFactory.create(config));
    }

    SkillStoreConfigValidationResult validateManagedStores(SkillManagementServiceConfig config) {
        return storeBundleFactory.validateManagedStores(config);
    }

    SkillManagementDeploymentPreflightReport preflight(SkillManagementDeploymentConfig config) {
        return preflightService.preflight(config);
    }

    SkillManagementDeploymentPreflightReport preflight(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return preflightService.preflight(config, sourceConfig, plan);
    }

    SkillManagementPreflightMatrix preflightMatrix(SkillManagementDeploymentConfig config) {
        return preflightService.matrix(config);
    }

    SkillManagementPreflightMatrix preflightMatrix(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return preflightService.matrix(config, sourceConfig, plan);
    }

    SkillManagementPreflightReport validation(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return preflightService.validation(config, sourceConfig, plan);
    }

    SkillManagementPreflightReport validation(SkillManagementDeploymentConfig config) {
        return preflightService.validation(config);
    }

    SkillManagementMaintenanceResult runMaintenance(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return workflowRunner.runMaintenance(config, sourceConfig, plan);
    }

    SkillManagementMaintenanceResult runMaintenance(SkillManagementDeploymentConfig config) {
        return workflowRunner.runMaintenance(config);
    }

    SkillManagementDeploymentResult deploy(SkillManagementDeploymentConfig config) {
        return workflowRunner.deploy(config);
    }
}
