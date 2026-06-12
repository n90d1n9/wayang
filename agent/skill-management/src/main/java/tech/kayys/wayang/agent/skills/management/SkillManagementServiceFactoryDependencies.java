package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized dependency graph for source-compatible service factory constructors.
 */
record SkillManagementServiceFactoryDependencies(
        SkillDefinitionStoreFactory definitionStoreFactory,
        SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
        SkillDefinitionStoreInspector definitionStoreInspector,
        SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
        SkillManagementEventStoreFactory eventStoreFactory,
        SkillArtifactStoreFactory artifactStoreFactory,
        SkillManagementEventSink eventSinkOverride) {

    SkillManagementServiceFactoryDependencies {
        definitionStoreFactory = Objects.requireNonNull(definitionStoreFactory, "definitionStoreFactory");
        lifecycleStateStoreFactory = Objects.requireNonNull(lifecycleStateStoreFactory, "lifecycleStateStoreFactory");
        definitionStoreInspector = Objects.requireNonNull(definitionStoreInspector, "definitionStoreInspector");
        lifecycleStateStoreInspector =
                Objects.requireNonNull(lifecycleStateStoreInspector, "lifecycleStateStoreInspector");
        eventStoreFactory = Objects.requireNonNull(eventStoreFactory, "eventStoreFactory");
        artifactStoreFactory = Objects.requireNonNull(artifactStoreFactory, "artifactStoreFactory");
    }

    static SkillManagementServiceFactoryDependencies registry(SkillRegistry registry) {
        return explicit(
                SkillManagementServiceFactoryDefaults.definitionStoreFactory(registry),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreFactory(),
                SkillManagementServiceFactoryDefaults.definitionStoreInspector(),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreInspector(),
                SkillManagementServiceFactoryDefaults.eventStoreFactory(),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(),
                null);
    }

    static SkillManagementServiceFactoryDependencies registryJdbc(
            SkillRegistry registry,
            DataSource jdbcDataSource) {
        return explicit(
                SkillManagementServiceFactoryDefaults.definitionStoreFactory(registry, jdbcDataSource),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreFactory(jdbcDataSource),
                SkillManagementServiceFactoryDefaults.definitionStoreInspector(),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreInspector(),
                SkillManagementServiceFactoryDefaults.eventStoreFactory(jdbcDataSource),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(jdbcDataSource),
                null);
    }

    static SkillManagementServiceFactoryDependencies registryObjectJdbc(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource) {
        return registryCustomStores(registry, objectStore, jdbcDataSource, Map.of(), Map.of(), Map.of());
    }

    static SkillManagementServiceFactoryDependencies registryCustomStores(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillDefinitionStore> customDefinitionStores,
            Map<String, SkillLifecycleStateStore> customLifecycleStateStores,
            Map<String, SkillArtifactStore> customArtifactStores) {
        return explicit(
                SkillManagementServiceFactoryDefaults.definitionStoreFactory(
                        registry,
                        objectStore,
                        jdbcDataSource,
                        customDefinitionStores),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreFactory(
                        jdbcDataSource,
                        objectStore,
                        customLifecycleStateStores),
                SkillManagementServiceFactoryDefaults.definitionStoreInspector(),
                SkillManagementServiceFactoryDefaults.lifecycleStateStoreInspector(),
                SkillManagementServiceFactoryDefaults.eventStoreFactory(jdbcDataSource, objectStore),
                SkillManagementServiceFactoryDefaults.artifactStoreFactory(
                        objectStore,
                        jdbcDataSource,
                        customArtifactStores),
                null);
    }

    static SkillManagementServiceFactoryDependencies explicit(
            SkillDefinitionStoreFactory definitionStoreFactory,
            SkillLifecycleStateStoreFactory lifecycleStateStoreFactory,
            SkillDefinitionStoreInspector definitionStoreInspector,
            SkillLifecycleStateStoreInspector lifecycleStateStoreInspector,
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillArtifactStoreFactory artifactStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        return new SkillManagementServiceFactoryDependencies(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                artifactStoreFactory,
                eventSinkOverride);
    }

    SkillManagementServiceFactoryComponents components() {
        return SkillManagementServiceFactoryComponents.assemble(
                definitionStoreFactory,
                lifecycleStateStoreFactory,
                definitionStoreInspector,
                lifecycleStateStoreInspector,
                eventStoreFactory,
                artifactStoreFactory,
                eventSinkOverride);
    }
}
