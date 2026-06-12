package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Creates and validates the persistence stores used by skill-management workflows.
 */
final class SkillManagementStoreBundleFactory {

    private final SkillDefinitionStoreFactory definitionStoreFactory;
    private final SkillLifecycleStateStoreFactory lifecycleStateStoreFactory;
    private final SkillManagementEventSinkFactory eventSinkFactory;
    private final SkillArtifactStoreFactory artifactStoreFactory;
    private final SkillManagementMaintenanceStoreFactory maintenanceStoreFactory;

    SkillManagementStoreBundleFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        this(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                new SkillManagementEventSinkFactory(eventStoreFactory, eventSinkOverride),
                artifactStoreFactory);
    }

    private SkillManagementStoreBundleFactory(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillManagementEventSinkFactory eventSinkFactory,
            SkillArtifactStoreFactory artifactStoreFactory) {
        this.definitionStoreFactory = Objects.requireNonNull(definitionStoreFactory, "definitionStoreFactory");
        this.lifecycleStateStoreFactory =
                Objects.requireNonNull(lifecycleStateStoreFactory, "lifecycleStateStoreFactory");
        this.eventSinkFactory = Objects.requireNonNull(eventSinkFactory, "eventSinkFactory");
        this.artifactStoreFactory = Objects.requireNonNull(artifactStoreFactory, "artifactStoreFactory");
        this.maintenanceStoreFactory = new SkillManagementMaintenanceStoreFactory(
                this.definitionStoreFactory,
                this.artifactStoreFactory);
    }

    SkillManagementStoreBundle create(SkillManagementServiceConfig config) {
        SkillManagementServiceConfig resolved = SkillManagementConfigResolution.serviceConfig(config);
        SkillDefinitionStore definitionStore = definitionStoreFactory.create(resolved.definitionStore());
        SkillLifecycleStateStore lifecycleStateStore =
                lifecycleStateStoreFactory.create(resolved.lifecycleStateStore());
        SkillArtifactStore artifactStore = artifactStoreFactory.create(resolved.artifactStore());
        SkillManagementEventSink eventSink = eventSinkFactory.create(resolved.eventStore());
        return new SkillManagementStoreBundle(
                definitionStore,
                lifecycleStateStore,
                artifactStore,
                eventSink);
    }

    SkillStoreConfigValidationResult validateManagedStores(SkillManagementServiceConfig config) {
        SkillManagementServiceConfig resolved = SkillManagementConfigResolution.serviceConfig(config);
        return SkillStoreConfigValidationResult.combine(
                definitionStoreFactory.validate(resolved.definitionStore()),
                lifecycleStateStoreFactory.validate(resolved.lifecycleStateStore()),
                eventSinkFactory.validate(resolved.eventStore()),
                artifactStoreFactory.validate(resolved.artifactStore()));
    }

    SkillStoreConfigValidationResult validateMaintenanceSources(
            SkillManagementMaintenanceSourceConfig sourceConfig) {
        return maintenanceStoreFactory.validateSources(sourceConfig);
    }

    SkillStoreConfigValidationResult validatePlanCapabilities(
            SkillManagementServiceConfig serviceConfig,
            SkillManagementMaintenancePlan plan) {
        SkillManagementServiceConfig resolvedService = SkillManagementConfigResolution.serviceConfig(serviceConfig);
        return SkillManagementMaintenancePreflight.validateCapabilities(
                plan,
                () -> eventSinkFactory.validatePruneSupport(resolvedService.eventStore()));
    }

    SkillManagementMaintenanceStores maintenanceStores(
            SkillManagementMaintenanceSourceConfig sourceConfig,
            SkillManagementStoreBundle targetStores) {
        return maintenanceStoreFactory.create(sourceConfig, targetStores);
    }

    SkillManagementEventSink eventSinkOverride() {
        return eventSinkFactory.override();
    }
}
