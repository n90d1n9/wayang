package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Hermes repair mutation approval storage from mode config.
 */
public final class HermesSkillLineageRepairApprovalStoreResolver {

    private HermesSkillLineageRepairApprovalStoreResolver() {
    }

    public static HermesSkillLineageRepairApprovalStore resolve(HermesAgentModeConfig config) {
        return resolve(config, Optional.empty(), Optional.empty());
    }

    public static HermesSkillLineageRepairApprovalStore resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService) {
        return resolve(config, objectStorageService, Optional.empty());
    }

    public static HermesSkillLineageRepairApprovalStore resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources resources = HermesPersistenceResources.of(objectStorageService, dataSource);
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.repairStore(
                effectiveConfig.skillLineageRepairApprovalStore(),
                "skillLineageRepairApprovalStore");
        return switch (store) {
            case NOOP -> HermesSkillLineageRepairApprovalStore.noop();
            case IN_MEMORY -> HermesSkillLineageRepairApprovalStore.inMemory(null);
            case FILE_SYSTEM -> fileSystemStore(effectiveConfig);
            case OBJECT_STORAGE -> objectStorageStore(
                    effectiveConfig,
                    resources.requireObjectStorage(
                            "Object-storage Hermes repair approval store requires an ObjectStorageService"));
            case DATABASE -> databaseStore(
                    effectiveConfig,
                    resources.requireDataSource("Database Hermes repair approval store requires a DataSource"));
            case HYBRID -> new HybridHermesSkillLineageRepairApprovalStore(
                    resources.databaseThenObjectStorageOr(
                            data -> (HermesSkillLineageRepairApprovalStore) databaseStore(effectiveConfig, data),
                            storage -> objectStorageStore(effectiveConfig, storage),
                            HermesSkillLineageRepairApprovalStore::noop),
                    fileSystemStore(effectiveConfig));
        };
    }

    public static Map<String, Object> metadata(HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.repairStore(
                effectiveConfig.skillLineageRepairApprovalStore(),
                "skillLineageRepairApprovalStore");
        HermesPersistenceStoreShape storeShape = HermesPersistenceStoreShape.of(store);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("approvalStore", store.configValue());
        values.put("approvalPath", effectiveConfig.skillLineageRepairApprovalPath());
        values.put("approvalObjectPrefix", effectiveConfig.skillLineageRepairApprovalObjectPrefix());
        values.put("approvalJdbcTableName", effectiveConfig.skillLineageRepairApprovalJdbcTableName());
        values.put(
                "approvalJdbcInitializeSchema",
                effectiveConfig.skillLineageRepairApprovalJdbcInitializeSchema());
        storeShape.putStorageMetadata(values);
        return Map.copyOf(values);
    }

    private static FileSystemHermesSkillLineageRepairApprovalStore fileSystemStore(
            HermesAgentModeConfig config) {
        return new FileSystemHermesSkillLineageRepairApprovalStore(
                Path.of(config.skillLineageRepairApprovalPath()));
    }

    private static ObjectStorageHermesSkillLineageRepairApprovalStore objectStorageStore(
            HermesAgentModeConfig config,
            ObjectStorageService objectStorageService) {
        return new ObjectStorageHermesSkillLineageRepairApprovalStore(
                objectStorageService,
                config.skillLineageRepairApprovalObjectPrefix());
    }

    private static DatabaseHermesSkillLineageRepairApprovalStore databaseStore(
            HermesAgentModeConfig config,
            DataSource dataSource) {
        return new DatabaseHermesSkillLineageRepairApprovalStore(
                dataSource,
                config.skillLineageRepairApprovalJdbcTableName(),
                config.skillLineageRepairApprovalJdbcInitializeSchema());
    }
}
