package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Builds skill-management event sinks from runtime dependencies.
 */
public final class SkillManagementEventStoreFactory {

    private static final String OBJECT_STORE_EVENT_STORE_MESSAGE =
            "Object-storage event store requires a SkillManagementObjectStore";
    private static final String JDBC_EVENT_STORE_MESSAGE = "JDBC event store requires a DataSource";

    private final DataSource jdbcDataSource;
    private final SkillManagementObjectStore objectStore;
    private final Map<String, SkillManagementEventSink> customStores;
    private final SkillStoreProviderRegistry<
            SkillManagementEventStoreConfig.Kind,
            SkillManagementEventStoreConfig,
            SkillManagementEventSink> providers;

    public SkillManagementEventStoreFactory() {
        this(null, null, Map.of());
    }

    public SkillManagementEventStoreFactory(DataSource jdbcDataSource) {
        this(jdbcDataSource, null, Map.of());
    }

    public SkillManagementEventStoreFactory(SkillManagementObjectStore objectStore) {
        this(null, objectStore, Map.of());
    }

    public SkillManagementEventStoreFactory(Map<String, SkillManagementEventSink> customStores) {
        this(null, null, customStores);
    }

    public SkillManagementEventStoreFactory(
            DataSource jdbcDataSource,
            SkillManagementObjectStore objectStore) {
        this(jdbcDataSource, objectStore, Map.of());
    }

    public SkillManagementEventStoreFactory(
            DataSource jdbcDataSource,
            Map<String, SkillManagementEventSink> customStores) {
        this(jdbcDataSource, null, customStores);
    }

    public SkillManagementEventStoreFactory(
            DataSource jdbcDataSource,
            SkillManagementObjectStore objectStore,
            Map<String, SkillManagementEventSink> customStores) {
        this.jdbcDataSource = jdbcDataSource;
        this.objectStore = objectStore;
        this.customStores = SkillStoreFactorySupport.customStores(customStores);
        this.providers = providers();
    }

    public SkillManagementEventSink create(SkillManagementEventStoreConfig config) {
        SkillManagementEventStoreConfig resolved =
                config == null ? SkillManagementEventStoreConfig.none() : config;
        return switch (resolved.kind()) {
            case NONE, MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.create(resolved.kind(), resolved);
            case HYBRID -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    SkillManagementEventSink::composite);
            case MIRRORED -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    MirroredSkillManagementEventSink::new);
        };
    }

    public SkillStoreConfigValidationResult validate(SkillManagementEventStoreConfig config) {
        SkillManagementEventStoreConfig resolved =
                config == null ? SkillManagementEventStoreConfig.none() : config;
        return SkillStoreConfigValidationResult.combine(
                resolved.validate(),
                validateRuntimeDependencies(resolved));
    }

    public SkillStoreConfigValidationResult validatePruneSupport(SkillManagementEventStoreConfig config) {
        SkillManagementEventStoreConfig resolved =
                config == null ? SkillManagementEventStoreConfig.none() : config;
        return switch (resolved.kind()) {
            case NONE -> SkillStoreCapabilityRequirement.eventStorePruning()
                    .validate(SkillStoreCapabilities.none());
            case MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC -> SkillStoreConfigValidationResult.valid();
            case CUSTOM -> validateCustomPruneSupport(resolved.customStoreName());
            case HYBRID, MIRRORED -> SkillStoreFactorySupport.validatePrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::validatePruneSupport);
        };
    }

    private SkillStoreConfigValidationResult validateRuntimeDependencies(SkillManagementEventStoreConfig config) {
        return switch (config.kind()) {
            case NONE, MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.validate(config.kind(), config);
            case HYBRID, MIRRORED -> SkillStoreFactorySupport.validatePrimaryFallback(
                    config.primary(),
                    config.fallback(),
                    this::validate);
        };
    }

    private SkillStoreProviderRegistry<
            SkillManagementEventStoreConfig.Kind,
            SkillManagementEventStoreConfig,
            SkillManagementEventSink> providers() {
        return SkillStoreProviderRegistry
                .<SkillManagementEventStoreConfig.Kind, SkillManagementEventStoreConfig, SkillManagementEventSink>builder()
                .register(
                        SkillManagementEventStoreConfig.Kind.NONE,
                        config -> SkillManagementEventSink.noop(),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillManagementEventStoreConfig.Kind.MEMORY,
                        config -> new InMemorySkillManagementEventSink(config.maxEvents()),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillManagementEventStoreConfig.Kind.FILESYSTEM,
                        config -> new FileSystemSkillManagementEventStore(
                                config.directory(),
                                config.maxEvents()),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillManagementEventStoreConfig.Kind.OBJECT_STORAGE,
                        config -> new ObjectStorageSkillManagementEventStore(
                                SkillStoreFactorySupport.requiredDependency(objectStoreRequirement()),
                                config.objectPrefix(),
                                config.maxEvents()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(objectStoreRequirement()))
                .register(
                        SkillManagementEventStoreConfig.Kind.JDBC,
                        config -> new JdbcSkillManagementEventStore(
                                SkillStoreFactorySupport.requiredDependency(jdbcDataSourceRequirement()),
                                config.jdbcTableName(),
                                config.initializeJdbcSchema(),
                                config.maxEvents()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(jdbcDataSourceRequirement()))
                .register(
                        SkillManagementEventStoreConfig.Kind.CUSTOM,
                        config -> SkillStoreFactorySupport.customStore(
                                customStores,
                                config.customStoreName(),
                                "event store"),
                        config -> SkillStoreFactorySupport.validateCustomStore(
                                customStores,
                                config.customStoreName(),
                                "event store"))
                .build();
    }

    private SkillStoreConfigValidationResult validateCustomPruneSupport(String name) {
        SkillManagementEventSink sink = customStores.get(name);
        if (sink == null) {
            return SkillStoreConfigValidationResult.valid();
        }
        return SkillStoreCapabilityRequirement.customEventStorePruning(name)
                .validate(SkillStoreCapabilities.eventStore(sink));
    }

    private SkillStoreFactorySupport.DependencyRequirement<SkillManagementObjectStore> objectStoreRequirement() {
        return SkillStoreFactorySupport.dependencyRequirement(
                objectStore,
                "objectStore",
                OBJECT_STORE_EVENT_STORE_MESSAGE);
    }

    private SkillStoreFactorySupport.DependencyRequirement<DataSource> jdbcDataSourceRequirement() {
        return SkillStoreFactorySupport.dependencyRequirement(
                jdbcDataSource,
                "jdbcDataSource",
                JDBC_EVENT_STORE_MESSAGE);
    }
}
