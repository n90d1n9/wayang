package tech.kayys.wayang.agent.skills.management;

/**
 * Service-level skill-management persistence configuration.
 */
public record SkillManagementServiceConfig(
        SkillDefinitionStoreConfig definitionStore,
        SkillLifecycleStateStoreConfig lifecycleStateStore,
        SkillManagementEventStoreConfig eventStore,
        SkillArtifactStoreConfig artifactStore,
        SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {

    public SkillManagementServiceConfig(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore) {
        this(definitionStore, lifecycleStateStore, SkillManagementEventStoreConfig.none());
    }

    public SkillManagementServiceConfig(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        this(definitionStore,
                lifecycleStateStore,
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.memory(),
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceConfig(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore) {
        this(definitionStore,
                lifecycleStateStore,
                eventStore,
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public SkillManagementServiceConfig(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        this(definitionStore,
                lifecycleStateStore,
                eventStore,
                SkillArtifactStoreConfig.memory(),
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceConfig {
        definitionStore = definitionStore == null ? SkillDefinitionStoreConfig.registry() : definitionStore;
        lifecycleStateStore =
                lifecycleStateStore == null ? SkillLifecycleStateStoreConfig.memory() : lifecycleStateStore;
        eventStore = eventStore == null ? SkillManagementEventStoreConfig.none() : eventStore;
        artifactStore = artifactStore == null ? SkillArtifactStoreConfig.memory() : artifactStore;
        lifecycleStateReconcileOptions = lifecycleStateReconcileOptions == null
                ? SkillLifecycleStateReconcileOptions.inspectOnly()
                : lifecycleStateReconcileOptions;
        validate(definitionStore, lifecycleStateStore, eventStore, artifactStore).throwIfInvalid();
    }

    public SkillStoreConfigValidationResult validate() {
        return validate(definitionStore, lifecycleStateStore, eventStore, artifactStore);
    }

    public SkillPersistenceContractMatrix persistenceContracts() {
        return SkillPersistenceContractMatrix.from(this);
    }

    public SkillPersistenceStrategySummary persistenceStrategy() {
        return SkillPersistenceStrategySummary.from(this);
    }

    public static SkillStoreConfigValidationResult validate(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore) {
        return validate(definitionStore, lifecycleStateStore, eventStore, null);
    }

    public static SkillStoreConfigValidationResult validate(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore,
            SkillArtifactStoreConfig artifactStore) {
        SkillDefinitionStoreConfig resolvedDefinitionStore =
                definitionStore == null ? SkillDefinitionStoreConfig.registry() : definitionStore;
        SkillLifecycleStateStoreConfig resolvedLifecycleStore =
                lifecycleStateStore == null ? SkillLifecycleStateStoreConfig.memory() : lifecycleStateStore;
        SkillManagementEventStoreConfig resolvedEventStore =
                eventStore == null ? SkillManagementEventStoreConfig.none() : eventStore;
        SkillArtifactStoreConfig resolvedArtifactStore =
                artifactStore == null ? SkillArtifactStoreConfig.memory() : artifactStore;
        return SkillStoreConfigValidationResult.combine(
                resolvedDefinitionStore.validate(),
                resolvedLifecycleStore.validate(),
                resolvedEventStore.validate(),
                resolvedArtifactStore.validate());
    }

    public static SkillManagementServiceConfig defaults() {
        return new SkillManagementServiceConfig(
                SkillDefinitionStoreConfig.registry(),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                SkillArtifactStoreConfig.memory(),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public static SkillManagementServiceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore) {
        return new SkillManagementServiceConfig(definitionStore, lifecycleStateStore);
    }

    public static SkillManagementServiceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        return new SkillManagementServiceConfig(
                definitionStore,
                lifecycleStateStore,
                lifecycleStateReconcileOptions);
    }

    public static SkillManagementServiceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore) {
        return new SkillManagementServiceConfig(
                definitionStore,
                lifecycleStateStore,
                eventStore);
    }

    public static SkillManagementServiceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        return new SkillManagementServiceConfig(
                definitionStore,
                lifecycleStateStore,
                eventStore,
                SkillArtifactStoreConfig.memory(),
                lifecycleStateReconcileOptions);
    }

    public static SkillManagementServiceConfig of(
            SkillDefinitionStoreConfig definitionStore,
            SkillLifecycleStateStoreConfig lifecycleStateStore,
            SkillManagementEventStoreConfig eventStore,
            SkillArtifactStoreConfig artifactStore,
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        return new SkillManagementServiceConfig(
                definitionStore,
                lifecycleStateStore,
                eventStore,
                artifactStore,
                lifecycleStateReconcileOptions);
    }
}
