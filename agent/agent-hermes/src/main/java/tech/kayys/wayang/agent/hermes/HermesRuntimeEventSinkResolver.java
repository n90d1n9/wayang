package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Hermes runtime event journals from mode config.
 */
public final class HermesRuntimeEventSinkResolver {

    private HermesRuntimeEventSinkResolver() {
    }

    public static HermesRuntimeEventSink compose(
            HermesAgentModeConfig config,
            Optional<HermesRuntimeEventSink> runtimeEventSink,
            Optional<ObjectStorageService> objectStorageService) {
        return compose(config, runtimeEventSink, objectStorageService, Optional.empty());
    }

    public static HermesRuntimeEventSink compose(
            HermesAgentModeConfig config,
            Optional<HermesRuntimeEventSink> runtimeEventSink,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        List<HermesRuntimeEventSink> sinks = new ArrayList<>();
        if (runtimeEventSink != null) {
            runtimeEventSink.ifPresent(sinks::add);
        }
        if (config != null && config.runtimeEventJournalEnabled()) {
            sinks.add(resolve(config, objectStorageService, dataSource));
        }
        if (sinks.isEmpty()) {
            return HermesRuntimeEventSink.noop();
        }
        return HermesRuntimeEventSink.composite(sinks);
    }

    public static HermesRuntimeEventSink resolve(HermesAgentModeConfig config) {
        return resolve(config, Optional.empty(), Optional.empty());
    }

    public static HermesRuntimeEventSink resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService) {
        return resolve(config, objectStorageService, Optional.empty());
    }

    public static HermesRuntimeEventSink resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources resources = HermesPersistenceResources.of(objectStorageService, dataSource);
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.runtimeEventJournal(
                effectiveConfig.runtimeEventJournalStore());
        return switch (store) {
            case FILE_SYSTEM -> fileSystemSink(effectiveConfig);
            case OBJECT_STORAGE -> objectStorageSink(
                    effectiveConfig,
                    resources.requireObjectStorage(
                            "Object-storage Hermes runtime event journal requires an ObjectStorageService"));
            case DATABASE -> databaseSink(
                    effectiveConfig,
                    resources.requireDataSource("Database Hermes runtime event journal requires a DataSource"));
            case HYBRID -> hybridSink(effectiveConfig, resources);
            case NOOP, IN_MEMORY -> throw new IllegalArgumentException(
                    "Unsupported Hermes runtime event journal store: " + store.configValue());
        };
    }

    public static Map<String, Object> metadata(HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceStoreKind store = HermesPersistenceStoreKind.runtimeEventJournal(
                effectiveConfig.runtimeEventJournalStore());
        HermesPersistenceStoreShape storeShape = HermesPersistenceStoreShape.of(store);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", effectiveConfig.runtimeEventJournalEnabled());
        values.put("journalStore", store.configValue());
        values.put("journalPath", effectiveConfig.runtimeEventJournalPath());
        values.put("journalObjectPrefix", effectiveConfig.runtimeEventJournalObjectPrefix());
        values.put("journalJdbcTableName", effectiveConfig.runtimeEventJournalJdbcTableName());
        values.put("journalJdbcInitializeSchema", effectiveConfig.runtimeEventJournalJdbcInitializeSchema());
        values.put("format", effectiveConfig.runtimeEventJournalFormat());
        values.put("maxEvents", effectiveConfig.runtimeEventJournalMaxEvents());
        storeShape.putRuntimeJournalMetadata(values, effectiveConfig.runtimeEventJournalEnabled());
        return Map.copyOf(values);
    }

    private static FileSystemHermesRuntimeEventSink fileSystemSink(HermesAgentModeConfig config) {
        return new FileSystemHermesRuntimeEventSink(
                Path.of(config.runtimeEventJournalPath()),
                config.runtimeEventJournalMaxEvents());
    }

    private static ObjectStorageHermesRuntimeEventSink objectStorageSink(
            HermesAgentModeConfig config,
            ObjectStorageService objectStorageService) {
        return new ObjectStorageHermesRuntimeEventSink(
                objectStorageService,
                config.runtimeEventJournalObjectPrefix(),
                config.runtimeEventJournalMaxEvents());
    }

    private static DatabaseHermesRuntimeEventSink databaseSink(
            HermesAgentModeConfig config,
            DataSource dataSource) {
        return new DatabaseHermesRuntimeEventSink(
                dataSource,
                config.runtimeEventJournalJdbcTableName(),
                config.runtimeEventJournalJdbcInitializeSchema(),
                config.runtimeEventJournalMaxEvents());
    }

    private static HermesRuntimeEventSink hybridSink(
            HermesAgentModeConfig config,
            HermesPersistenceResources resources) {
        HermesRuntimeEventSink fileSink = fileSystemSink(config);
        return resources.databaseThenObjectStorageOr(
                data -> HermesRuntimeEventSink.composite(databaseSink(config, data), fileSink),
                storage -> HermesRuntimeEventSink.composite(objectStorageSink(config, storage), fileSink),
                () -> fileSink);
    }
}
