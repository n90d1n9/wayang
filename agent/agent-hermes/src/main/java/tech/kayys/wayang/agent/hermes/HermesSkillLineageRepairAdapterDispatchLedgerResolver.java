package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Hermes repair adapter dispatch idempotency storage from mode config.
 */
public final class HermesSkillLineageRepairAdapterDispatchLedgerResolver {

    public static final String DEFAULT_FILE_SYSTEM_PATH =
            "var/hermes/repair-adapter-dispatch-ledger.jsonl";

    private HermesSkillLineageRepairAdapterDispatchLedgerResolver() {
    }

    public static HermesSkillLineageRepairAdapterDispatchLedger resolve(HermesAgentModeConfig config) {
        return resolve(config, Optional.empty(), Optional.empty());
    }

    public static HermesSkillLineageRepairAdapterDispatchLedger resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService) {
        return resolve(config, objectStorageService, Optional.empty());
    }

    public static HermesSkillLineageRepairAdapterDispatchLedger resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources resources = HermesPersistenceResources.of(objectStorageService, dataSource);
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.repairStore(
                effectiveConfig.skillLineageRepairDispatchLedgerStore(),
                "skillLineageRepairDispatchLedgerStore");
        return switch (store) {
            case IN_MEMORY -> HermesSkillLineageRepairAdapterDispatchLedger.inMemory();
            case FILE_SYSTEM -> fileSystemLedger(effectiveConfig);
            case OBJECT_STORAGE -> objectStorageLedger(
                    effectiveConfig,
                    resources.requireObjectStorage(
                            "Object-storage Hermes repair dispatch ledger requires an ObjectStorageService"));
            case DATABASE -> databaseLedger(
                    effectiveConfig,
                    resources.requireDataSource("Database Hermes repair dispatch ledger requires a DataSource"));
            case HYBRID -> new HybridHermesSkillLineageRepairAdapterDispatchLedger(
                    resources.databaseThenObjectStorageOr(
                            data -> (HermesSkillLineageRepairAdapterDispatchLedger) databaseLedger(
                                    effectiveConfig,
                                    data),
                            storage -> objectStorageLedger(effectiveConfig, storage),
                            HermesSkillLineageRepairAdapterDispatchLedger::noop),
                    fileSystemLedger(effectiveConfig));
            case NOOP -> HermesSkillLineageRepairAdapterDispatchLedger.noop();
        };
    }

    public static Map<String, Object> metadata(HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.repairStore(
                effectiveConfig.skillLineageRepairDispatchLedgerStore(),
                "skillLineageRepairDispatchLedgerStore");
        HermesPersistenceStoreShape storeShape = HermesPersistenceStoreShape.of(store);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerStore", store.configValue());
        values.put("ledgerPath", effectiveConfig.skillLineageRepairDispatchLedgerPath());
        values.put("ledgerObjectPrefix", effectiveConfig.skillLineageRepairDispatchLedgerObjectPrefix());
        values.put("ledgerJdbcTableName", effectiveConfig.skillLineageRepairDispatchLedgerJdbcTableName());
        values.put(
                "ledgerJdbcInitializeSchema",
                effectiveConfig.skillLineageRepairDispatchLedgerJdbcInitializeSchema());
        values.put("maxRecords", effectiveConfig.skillLineageRepairDispatchLedgerMaxRecords());
        storeShape.putStorageMetadata(values);
        values.put("replaySupported", storeShape.replaySupported());
        return Map.copyOf(values);
    }

    private static FileSystemHermesSkillLineageRepairAdapterDispatchLedger fileSystemLedger(
            HermesAgentModeConfig config) {
        return new FileSystemHermesSkillLineageRepairAdapterDispatchLedger(
                Path.of(config.skillLineageRepairDispatchLedgerPath()),
                config.skillLineageRepairDispatchLedgerMaxRecords());
    }

    private static ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger objectStorageLedger(
            HermesAgentModeConfig config,
            ObjectStorageService objectStorageService) {
        return new ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger(
                objectStorageService,
                config.skillLineageRepairDispatchLedgerObjectPrefix(),
                config.skillLineageRepairDispatchLedgerMaxRecords());
    }

    private static DatabaseHermesSkillLineageRepairAdapterDispatchLedger databaseLedger(
            HermesAgentModeConfig config,
            DataSource dataSource) {
        return new DatabaseHermesSkillLineageRepairAdapterDispatchLedger(
                dataSource,
                config.skillLineageRepairDispatchLedgerJdbcTableName(),
                config.skillLineageRepairDispatchLedgerJdbcInitializeSchema(),
                config.skillLineageRepairDispatchLedgerMaxRecords());
    }
}
