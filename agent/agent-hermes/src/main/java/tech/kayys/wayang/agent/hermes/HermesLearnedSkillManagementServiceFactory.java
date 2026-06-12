package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillArtifactStoreFactory;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreFactory;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreFactory;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementObjectStore;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.StorageServiceSkillManagementObjectStore;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a dedicated skill-management service for Hermes learned-skill stores.
 */
final class HermesLearnedSkillManagementServiceFactory {

    private HermesLearnedSkillManagementServiceFactory() {
    }

    static Optional<SkillManagementService> createIfAvailable(
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options,
            HermesPersistenceResources resources) {
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        if (!HermesLearnedSkillStoreConfigs.canUseDedicatedSkillManagementService(targetPlan)
                || !resourcesAvailable(targetPlan, effectiveResources)) {
            return Optional.empty();
        }
        return Optional.of(create(targetPlan, options, effectiveResources));
    }

    private static SkillManagementService create(
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options,
            HermesPersistenceResources resources) {
        SkillManagementServiceConfig config =
                HermesLearnedSkillStoreConfigs.serviceConfig(targetPlan, options);
        SkillManagementObjectStore objectStore = resources.objectStorageService()
                .map(StorageServiceSkillManagementObjectStore::new)
                .orElse(null);
        DataSource dataSource = resources.dataSource().orElse(null);
        return new SkillManagementService(
                new SkillDefinitionStoreFactory(null, objectStore, dataSource, Map.of())
                        .create(config.definitionStore()),
                new SkillDefinitionStoreInspector(),
                new SkillLifecycleStateStoreFactory(dataSource, objectStore)
                        .create(config.lifecycleStateStore()),
                new SkillLifecycleStateStoreInspector(),
                new SkillArtifactStoreFactory(objectStore, dataSource, Map.of())
                        .create(config.artifactStore()),
                SkillManagementEventSink.noop());
    }

    private static boolean resourcesAvailable(
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesPersistenceResources resources) {
        return (!HermesLearnedSkillStoreConfigs.requiresObjectStorage(targetPlan)
                        || resources.objectStorageService().isPresent())
                && (!HermesLearnedSkillStoreConfigs.requiresDataSource(targetPlan)
                        || resources.dataSource().isPresent());
    }
}
