package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Builds lifecycle state stores from runtime dependencies.
 */
public final class SkillLifecycleStateStoreFactory {

    private final DataSource jdbcDataSource;
    private final SkillManagementObjectStore objectStore;
    private final Map<String, SkillLifecycleStateStore> customStores;
    private final SkillStoreProviderRegistry<
            SkillLifecycleStateStoreConfig.Kind,
            SkillLifecycleStateStoreConfig,
            SkillLifecycleStateStore> providers;

    public SkillLifecycleStateStoreFactory() {
        this((DataSource) null);
    }

    public SkillLifecycleStateStoreFactory(DataSource jdbcDataSource) {
        this(jdbcDataSource, Map.of());
    }

    public SkillLifecycleStateStoreFactory(SkillManagementObjectStore objectStore) {
        this(null, objectStore, Map.of());
    }

    public SkillLifecycleStateStoreFactory(
            DataSource jdbcDataSource,
            SkillManagementObjectStore objectStore) {
        this(jdbcDataSource, objectStore, Map.of());
    }

    public SkillLifecycleStateStoreFactory(
            DataSource jdbcDataSource,
            Map<String, SkillLifecycleStateStore> customStores) {
        this(jdbcDataSource, null, customStores);
    }

    public SkillLifecycleStateStoreFactory(
            DataSource jdbcDataSource,
            SkillManagementObjectStore objectStore,
            Map<String, SkillLifecycleStateStore> customStores) {
        this.jdbcDataSource = jdbcDataSource;
        this.objectStore = objectStore;
        this.customStores = SkillStoreFactorySupport.customStores(customStores);
        this.providers = providers();
    }

    public SkillLifecycleStateStore create(SkillLifecycleStateStoreConfig config) {
        SkillLifecycleStateStoreConfig resolved =
                config == null ? SkillLifecycleStateStoreConfig.memory() : config;
        return switch (resolved.kind()) {
            case MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.create(resolved.kind(), resolved);
            case HYBRID -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    HybridSkillLifecycleStateStore::new);
            case MIRRORED -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    MirroredSkillLifecycleStateStore::new);
        };
    }

    public SkillStoreConfigValidationResult validate(SkillLifecycleStateStoreConfig config) {
        SkillLifecycleStateStoreConfig resolved =
                config == null ? SkillLifecycleStateStoreConfig.memory() : config;
        return SkillStoreConfigValidationResult.combine(
                resolved.validate(),
                validateRuntimeDependencies(resolved));
    }

    private SkillStoreConfigValidationResult validateRuntimeDependencies(SkillLifecycleStateStoreConfig config) {
        return switch (config.kind()) {
            case MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.validate(config.kind(), config);
            case HYBRID, MIRRORED -> SkillStoreFactorySupport.validatePrimaryFallback(
                    config.primary(),
                    config.fallback(),
                    this::validate);
        };
    }

    private SkillStoreProviderRegistry<
            SkillLifecycleStateStoreConfig.Kind,
            SkillLifecycleStateStoreConfig,
            SkillLifecycleStateStore> providers() {
        return SkillStoreProviderRegistry
                .<SkillLifecycleStateStoreConfig.Kind, SkillLifecycleStateStoreConfig, SkillLifecycleStateStore>builder()
                .register(
                        SkillLifecycleStateStoreConfig.Kind.MEMORY,
                        config -> new InMemorySkillLifecycleStateStore(),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillLifecycleStateStoreConfig.Kind.FILESYSTEM,
                        config -> new FileSystemSkillLifecycleStateStore(config.directory()),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillLifecycleStateStoreConfig.Kind.OBJECT_STORAGE,
                        config -> new ObjectStorageSkillLifecycleStateStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        objectStoreRequirement(config.kind())),
                                config.objectPrefix()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                objectStoreRequirement(config.kind())))
                .register(
                        SkillLifecycleStateStoreConfig.Kind.JDBC,
                        config -> new JdbcSkillLifecycleStateStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        jdbcDataSourceRequirement(config.kind())),
                                config.jdbcTableName(),
                                config.initializeJdbcSchema()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                jdbcDataSourceRequirement(config.kind())))
                .register(
                        SkillLifecycleStateStoreConfig.Kind.CUSTOM,
                        config -> SkillStoreFactorySupport.customStore(
                                customStores,
                                config.customStoreName(),
                                "lifecycle state store"),
                        config -> SkillStoreFactorySupport.validateCustomStore(
                                customStores,
                                config.customStoreName(),
                                "lifecycle state store"))
                .build();
    }

    private SkillStoreFactorySupport.DependencyRequirement<SkillManagementObjectStore> objectStoreRequirement(
            SkillLifecycleStateStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(objectStore, "objectStore", kind, "Lifecycle store");
    }

    private SkillStoreFactorySupport.DependencyRequirement<DataSource> jdbcDataSourceRequirement(
            SkillLifecycleStateStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(
                jdbcDataSource,
                "jdbcDataSource",
                kind,
                "Lifecycle store");
    }
}
