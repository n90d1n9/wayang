package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillArtifactStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateReconcileOptions;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import java.util.Set;

/**
 * Translates Hermes learned-skill targets into skill-management store configs.
 */
public final class HermesLearnedSkillStoreConfigs {

    private static final Set<String> DEDICATED_STORAGE_FAMILIES =
            Set.of("database", "object-storage", "file-system");

    private HermesLearnedSkillStoreConfigs() {
    }

    public static SkillManagementServiceConfig serviceConfig(
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        HermesLearnedSkillPersistenceAdapterResolverOptions effectiveOptions =
                options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
        return SkillManagementServiceConfig.of(
                definitionStoreConfig(effectivePlan.definitions(), effectiveOptions),
                SkillLifecycleStateStoreConfig.memory(),
                SkillManagementEventStoreConfig.none(),
                artifactStoreConfig(effectivePlan.artifacts(), effectiveOptions),
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public static boolean canUseDedicatedSkillManagementService(HermesSkillPersistenceTargetPlan targetPlan) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        return dedicatedBackend(effectivePlan.definitions())
                && dedicatedBackend(effectivePlan.artifacts());
    }

    public static boolean requiresObjectStorage(HermesSkillPersistenceTargetPlan targetPlan) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        return backendSelected(effectivePlan.definitions(), "object-storage")
                || backendSelected(effectivePlan.artifacts(), "object-storage");
    }

    public static boolean requiresDataSource(HermesSkillPersistenceTargetPlan targetPlan) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        return backendSelected(effectivePlan.definitions(), "database")
                || backendSelected(effectivePlan.artifacts(), "database");
    }

    static SkillDefinitionStoreConfig definitionStoreConfig(
            HermesSkillPersistenceTarget target,
            HermesLearnedSkillPersistenceAdapterResolverOptions options) {
        HermesLearnedSkillPersistenceAdapterResolverOptions effectiveOptions =
                options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
        SkillDefinitionStoreConfig primary = switch (selectedStorageFamily(target)) {
            case "database" -> SkillDefinitionStoreConfig.jdbc(
                    effectiveOptions.jdbcDefinitionTableName(),
                    effectiveOptions.jdbcInitializeSchema());
            case "object-storage" -> SkillDefinitionStoreConfig.objectStorage(
                    effectiveOptions.objectStorageDefinitionPrefix());
            case "file-system" -> SkillDefinitionStoreConfig.fileSystem(
                    effectiveOptions.fileSystemDefinitionDirectory());
            default -> SkillDefinitionStoreConfig.registry();
        };
        if (usesFileFallback(target) && !"file-system".equals(selectedStorageFamily(target))) {
            return SkillDefinitionStoreConfig.hybridWithFileFallback(
                    primary,
                    effectiveOptions.fileSystemDefinitionDirectory());
        }
        return primary;
    }

    static SkillArtifactStoreConfig artifactStoreConfig(
            HermesSkillPersistenceTarget target,
            HermesLearnedSkillPersistenceAdapterResolverOptions options) {
        HermesLearnedSkillPersistenceAdapterResolverOptions effectiveOptions =
                options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
        SkillArtifactStoreConfig primary = switch (selectedStorageFamily(target)) {
            case "database" -> SkillArtifactStoreConfig.jdbc(
                    effectiveOptions.jdbcArtifactTableName(),
                    effectiveOptions.jdbcInitializeSchema());
            case "object-storage" -> SkillArtifactStoreConfig.objectStorage(
                    effectiveOptions.objectStorageArtifactPrefix());
            case "file-system" -> SkillArtifactStoreConfig.fileSystem(
                    effectiveOptions.fileSystemArtifactDirectory());
            default -> SkillArtifactStoreConfig.memory();
        };
        if (usesFileFallback(target) && !"file-system".equals(selectedStorageFamily(target))) {
            return SkillArtifactStoreConfig.hybridWithFileFallback(
                    primary,
                    effectiveOptions.fileSystemArtifactDirectory());
        }
        return primary;
    }

    private static boolean dedicatedBackend(HermesSkillPersistenceTarget target) {
        return DEDICATED_STORAGE_FAMILIES.contains(selectedStorageFamily(target));
    }

    private static boolean backendSelected(HermesSkillPersistenceTarget target, String backendId) {
        return selectedStorageFamily(target).equals(HermesSkillPersistenceBackendProfile.normalizeBackendId(backendId));
    }

    private static boolean usesFileFallback(HermesSkillPersistenceTarget target) {
        return target != null
                && target.fallbackBackendIds().stream()
                        .map(HermesSkillPersistenceBackendProfile::normalizeBackendId)
                        .anyMatch("file-system"::equals);
    }

    private static String selectedStorageFamily(HermesSkillPersistenceTarget target) {
        return target == null
                ? ""
                : target.selectedProfile()
                        .map(HermesSkillPersistenceBackendProfile::storageFamily)
                        .map(HermesSkillPersistenceBackendProfile::normalizeBackendId)
                        .orElse("");
    }
}
