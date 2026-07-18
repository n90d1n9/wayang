package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Builds configured skill definition stores from runtime dependencies.
 */
public final class SkillDefinitionStoreFactory {

    private final SkillRegistry registry;
    private final SkillManagementObjectStore objectStore;
    private final DataSource jdbcDataSource;
    private final Map<String, SkillDefinitionStore> customStores;
    private final SkillStoreProviderRegistry<
            SkillDefinitionStoreConfig.Kind,
            SkillDefinitionStoreConfig,
            SkillDefinitionStore> providers;

    public SkillDefinitionStoreFactory(SkillRegistry registry) {
        this(registry, null, Map.of());
    }

    public SkillDefinitionStoreFactory(SkillRegistry registry, SkillManagementObjectStore objectStore) {
        this(registry, objectStore, null, Map.of());
    }

    public SkillDefinitionStoreFactory(SkillRegistry registry, ObjectStorageService objectStorageService) {
        this(registry, new StorageServiceSkillManagementObjectStore(objectStorageService), null, Map.of());
    }

    public SkillDefinitionStoreFactory(SkillRegistry registry, DataSource jdbcDataSource) {
        this(registry, null, jdbcDataSource, Map.of());
    }

    public SkillDefinitionStoreFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            Map<String, SkillDefinitionStore> customStores) {
        this(registry, objectStore, null, customStores);
    }

    public SkillDefinitionStoreFactory(
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillDefinitionStore> customStores) {
        this.registry = registry;
        this.objectStore = objectStore;
        this.jdbcDataSource = jdbcDataSource;
        this.customStores = SkillStoreFactorySupport.customStores(customStores);
        this.providers = providers();
    }

    public SkillDefinitionStore create(SkillDefinitionStoreConfig config) {
        SkillDefinitionStoreConfig resolved = config == null ? SkillDefinitionStoreConfig.registry() : config;
        return switch (resolved.kind()) {
            case REGISTRY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.create(resolved.kind(), resolved);
            case HYBRID -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    HybridSkillDefinitionStore::new);
            case MIRRORED -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    MirroredSkillDefinitionStore::new);
        };
    }

    public SkillStoreConfigValidationResult validate(SkillDefinitionStoreConfig config) {
        SkillDefinitionStoreConfig resolved = config == null ? SkillDefinitionStoreConfig.registry() : config;
        return SkillStoreConfigValidationResult.combine(
                resolved.validate(),
                validateRuntimeDependencies(resolved));
    }

    private SkillStoreConfigValidationResult validateRuntimeDependencies(SkillDefinitionStoreConfig config) {
        return switch (config.kind()) {
            case REGISTRY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.validate(config.kind(), config);
            case HYBRID, MIRRORED -> SkillStoreFactorySupport.validatePrimaryFallback(
                    config.primary(),
                    config.fallback(),
                    this::validate);
        };
    }

    private SkillStoreProviderRegistry<
            SkillDefinitionStoreConfig.Kind,
            SkillDefinitionStoreConfig,
            SkillDefinitionStore> providers() {
        return SkillStoreProviderRegistry
                .<SkillDefinitionStoreConfig.Kind, SkillDefinitionStoreConfig, SkillDefinitionStore>builder()
                .register(
                        SkillDefinitionStoreConfig.Kind.REGISTRY,
                        config -> new RegistrySkillDefinitionStore(SkillStoreFactorySupport.requiredDependency(
                                registryRequirement(config.kind()))),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                registryRequirement(config.kind())))
                .register(
                        SkillDefinitionStoreConfig.Kind.FILESYSTEM,
                        config -> new FileSystemSkillDefinitionStore(config.directory()),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillDefinitionStoreConfig.Kind.OBJECT_STORAGE,
                        config -> new ObjectStorageSkillDefinitionStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        objectStoreRequirement(config.kind())),
                                config.objectPrefix()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                objectStoreRequirement(config.kind())))
                .register(
                        SkillDefinitionStoreConfig.Kind.JDBC,
                        config -> new JdbcSkillDefinitionStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        jdbcDataSourceRequirement(config.kind())),
                                config.jdbcTableName(),
                                config.initializeJdbcSchema()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                jdbcDataSourceRequirement(config.kind())))
                .register(
                        SkillDefinitionStoreConfig.Kind.CUSTOM,
                        config -> SkillStoreFactorySupport.customStore(
                                customStores,
                                config.customStoreName(),
                                "skill definition store"),
                        config -> SkillStoreFactorySupport.validateCustomStore(
                                customStores,
                                config.customStoreName(),
                                "skill definition store"))
                .build();
    }

    private SkillStoreFactorySupport.DependencyRequirement<SkillRegistry> registryRequirement(
            SkillDefinitionStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(registry, "registry", kind, "Skill store");
    }

    private SkillStoreFactorySupport.DependencyRequirement<SkillManagementObjectStore> objectStoreRequirement(
            SkillDefinitionStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(objectStore, "objectStore", kind, "Skill store");
    }

    private SkillStoreFactorySupport.DependencyRequirement<DataSource> jdbcDataSourceRequirement(
            SkillDefinitionStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(jdbcDataSource, "jdbcDataSource", kind, "Skill store");
    }

    public static SkillDefinitionStore create(
            SkillDefinitionStoreConfig config,
            SkillRegistry registry,
            SkillManagementObjectStore objectStore) {
        return new SkillDefinitionStoreFactory(registry, objectStore).create(config);
    }

    public static SkillDefinitionStore create(
            SkillDefinitionStoreConfig config,
            SkillRegistry registry,
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource) {
        return new SkillDefinitionStoreFactory(registry, objectStore, jdbcDataSource, Map.of()).create(config);
    }
}
