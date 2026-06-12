package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Assembles skill-management services from configured persistence factories.
 */
public final class SkillManagementServiceFactory {

    private final SkillManagementServiceFactoryComponents components;

    public SkillManagementServiceFactory(SkillRegistry registry) {
        this(SkillManagementServiceFactoryDependencies.registry(registry));
    }

    public SkillManagementServiceFactory(SkillRegistry registry, DataSource jdbcDataSource) {
        this(SkillManagementServiceFactoryDependencies.registryJdbc(registry, jdbcDataSource));
    }

    public SkillManagementServiceFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillDefinitionStore> customStores) {
        this(SkillManagementServiceFactoryDependencies.registryCustomStores(
                registry,
                objectStore,
                jdbcDataSource,
                customStores,
                Map.of(),
                Map.of()));
    }

    public SkillManagementServiceFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillDefinitionStore> customStores,
            Map<String, SkillLifecycleStateStore> customLifecycleStateStores) {
        this(SkillManagementServiceFactoryDependencies.registryCustomStores(
                registry,
                objectStore,
                jdbcDataSource,
                customStores,
                customLifecycleStateStores,
                Map.of()));
    }

    public SkillManagementServiceFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillDefinitionStore> customStores,
            Map<String, SkillLifecycleStateStore> customLifecycleStateStores,
            Map<String, SkillArtifactStore> customArtifactStores) {
        this(SkillManagementServiceFactoryDependencies.registryCustomStores(
                registry,
                objectStore,
                jdbcDataSource,
                customStores,
                customLifecycleStateStores,
                customArtifactStores));
    }

    public SkillManagementServiceFactory(
            SkillRegistry registry,
            ObjectStorageService objectStorageService,
            DataSource jdbcDataSource) {
        this(registry, SkillManagementServiceFactoryDefaults.objectStore(objectStorageService), jdbcDataSource);
    }

    public SkillManagementServiceFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource) {
        this(SkillManagementServiceFactoryDependencies.registryObjectJdbc(registry, objectStore, jdbcDataSource));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                SkillManagementServiceFactoryDefaults.definitionStoreInspector(),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreInspector(),
                SkillManagementServiceFactoryDefaults.eventStoreFactory(),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                null));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                SkillManagementServiceFactoryDefaults.eventStoreFactory(),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                null));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventSink eventSink) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                SkillManagementServiceFactoryDefaults.eventStoreFactory(),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                eventSink));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                null));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                artifactStoreFactory,
                null));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                eventSinkOverride));
    }

    public SkillManagementServiceFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        this(SkillManagementServiceFactoryDependencies.explicit(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                artifactStoreFactory,
                eventSinkOverride));
    }

    private SkillManagementServiceFactory(SkillManagementServiceFactoryDependencies dependencies) {
        this.components = dependencies.components();
    }

    public SkillManagementService create() {
        return create(SkillManagementConfigResolution.serviceConfig(null));
    }

    public SkillManagementService create(SkillManagementServiceConfig config) {
        return components.service(config);
    }

    public SkillManagementService create(SkillManagementDeploymentConfig config) {
        SkillManagementDeploymentConfig resolved = SkillManagementConfigResolution.deploymentConfig(config);
        return create(resolved.serviceConfig());
    }

    public SkillManagementService create(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore) {
        return create(SkillManagementServiceConfig.of(definitionStore, lifecycleStateStore));
    }

    public SkillStoreConfigValidationResult validate(SkillManagementServiceConfig config) {
        return components.validateManagedStores(config);
    }

    public SkillManagementDeploymentPreflightReport preflight(SkillManagementDeploymentConfig config) {
        return components.preflight(config);
    }

    public SkillManagementDeploymentPreflightReport preflight(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return components.preflight(config, sourceConfig, plan);
    }

    public SkillManagementPreflightMatrix preflightMatrix(SkillManagementDeploymentConfig config) {
        return components.preflightMatrix(config);
    }

    public SkillManagementPreflightMatrix preflightMatrix(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return components.preflightMatrix(config, sourceConfig, plan);
    }

    public SkillManagementPreflightReport preflightValidation(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return components.validation(config, sourceConfig, plan);
    }

    public SkillManagementPreflightReport preflightValidation(SkillManagementDeploymentConfig config) {
        return components.validation(config);
    }

    public SkillManagementMaintenanceResult runMaintenance(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementMaintenancePlan plan) {
        return components.runMaintenance(config, sourceConfig, plan);
    }

    public SkillManagementDeploymentResult deploy(SkillManagementDeploymentConfig config) {
        return components.deploy(config);
    }

    public SkillManagementMaintenanceResult runMaintenance(SkillManagementDeploymentConfig config) {
        return components.runMaintenance(config);
    }

    public SkillManagementMaintenanceResult runMaintenance(
            SkillManagementServiceConfig config,
            SkillManagementMaintenanceSourceConfig sourceConfig) {
        return runMaintenance(config, sourceConfig, SkillManagementConfigResolution.maintenancePlan(null));
    }

    public SkillManagementMaintenanceResult runMaintenance(
            SkillManagementServiceConfig config,
            SkillManagementMaintenancePlan plan) {
        return runMaintenance(config, SkillManagementConfigResolution.maintenanceSource(null), plan);
    }

    public SkillManagementMaintenanceResult runMaintenance(SkillManagementServiceConfig config) {
        return runMaintenance(
                config,
                SkillManagementConfigResolution.maintenanceSource(null),
                SkillManagementConfigResolution.maintenancePlan(null));
    }
}
