package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Default dependencies used by source-compatible service-factory constructors.
 */
final class SkillManagementServiceFactoryDefaults {

    private SkillManagementServiceFactoryDefaults() {
    }

    static SkillDefinitionStoreFactory definitionStoreFactory(SkillRegistry registry) {
        return new SkillDefinitionStoreFactory(registry);
    }

    static SkillDefinitionStoreFactory definitionStoreFactory(SkillRegistry registry, DataSource dataSource) {
        return new SkillDefinitionStoreFactory(registry, dataSource);
    }

    static SkillDefinitionStoreFactory definitionStoreFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource dataSource,
            Map<String, SkillDefinitionStore> customStores) {
        return new SkillDefinitionStoreFactory(registry, objectStore, dataSource, customStores);
    }

    static SkillLifecycleStateStoreFactory lifecycleStateStoreFactory() {
        return new SkillLifecycleStateStoreFactory();
    }

    static SkillLifecycleStateStoreFactory lifecycleStateStoreFactory(DataSource dataSource) {
        return new SkillLifecycleStateStoreFactory(dataSource);
    }

    static SkillLifecycleStateStoreFactory lifecycleStateStoreFactory(
            DataSource dataSource,
            SkillManagementObjectStore objectStore) {
        return new SkillLifecycleStateStoreFactory(dataSource, objectStore);
    }

    static SkillLifecycleStateStoreFactory lifecycleStateStoreFactory(
            DataSource dataSource,
            SkillManagementObjectStore objectStore,
            Map<String, SkillLifecycleStateStore> customStores) {
        return new SkillLifecycleStateStoreFactory(dataSource, objectStore, customStores);
    }

    static SkillDefinitionStoreInspector definitionStoreInspector() {
        return new SkillDefinitionStoreInspector();
    }

    static SkillLifecycleStateStoreInspector lifecycleStateStoreInspector() {
        return new SkillLifecycleStateStoreInspector();
    }

    static SkillManagementEventStoreFactory eventStoreFactory() {
        return new SkillManagementEventStoreFactory();
    }

    static SkillManagementEventStoreFactory eventStoreFactory(DataSource dataSource) {
        return new SkillManagementEventStoreFactory(dataSource);
    }

    static SkillManagementEventStoreFactory eventStoreFactory(
            DataSource dataSource,
            SkillManagementObjectStore objectStore) {
        return new SkillManagementEventStoreFactory(dataSource, objectStore);
    }

    static SkillArtifactStoreFactory artifactStoreFactory() {
        return new SkillArtifactStoreFactory();
    }

    static SkillArtifactStoreFactory artifactStoreFactory(DataSource dataSource) {
        return new SkillArtifactStoreFactory(dataSource);
    }

    static SkillArtifactStoreFactory artifactStoreFactory(
            SkillManagementObjectStore objectStore,
            DataSource dataSource,
            Map<String, SkillArtifactStore> customStores) {
        return new SkillArtifactStoreFactory(objectStore, dataSource, customStores);
    }

    static SkillManagementObjectStore objectStore(ObjectStorageService objectStorageService) {
        return new StorageServiceSkillManagementObjectStore(objectStorageService);
    }
}
