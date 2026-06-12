package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Builds artifact stores from runtime dependencies.
 */
public final class SkillArtifactStoreFactory {

    private final SkillManagementObjectStore objectStore;
    private final DataSource jdbcDataSource;
    private final Map<String, SkillArtifactStore> customStores;
    private final SkillStoreProviderRegistry<
            SkillArtifactStoreConfig.Kind,
            SkillArtifactStoreConfig,
            SkillArtifactStore> providers;

    public SkillArtifactStoreFactory() {
        this((SkillManagementObjectStore) null);
    }

    public SkillArtifactStoreFactory(SkillManagementObjectStore objectStore) {
        this(objectStore, null, Map.of());
    }

    public SkillArtifactStoreFactory(DataSource jdbcDataSource) {
        this(null, jdbcDataSource, Map.of());
    }

    public SkillArtifactStoreFactory(ObjectStorageService objectStorageService) {
        this(new StorageServiceSkillManagementObjectStore(objectStorageService), null, Map.of());
    }

    public SkillArtifactStoreFactory(Map<String, SkillArtifactStore> customStores) {
        this(null, null, customStores);
    }

    public SkillArtifactStoreFactory(
            SkillManagementObjectStore objectStore,
            Map<String, SkillArtifactStore> customStores) {
        this(objectStore, null, customStores);
    }

    public SkillArtifactStoreFactory(
            SkillManagementObjectStore objectStore,
            DataSource jdbcDataSource,
            Map<String, SkillArtifactStore> customStores) {
        this.objectStore = objectStore;
        this.jdbcDataSource = jdbcDataSource;
        this.customStores = SkillStoreFactorySupport.customStores(customStores);
        this.providers = providers();
    }

    public SkillArtifactStore create(SkillArtifactStoreConfig config) {
        SkillArtifactStoreConfig resolved = config == null ? SkillArtifactStoreConfig.memory() : config;
        return switch (resolved.kind()) {
            case MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.create(resolved.kind(), resolved);
            case HYBRID -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    HybridSkillArtifactStore::new);
            case MIRRORED -> SkillStoreFactorySupport.createPrimaryFallback(
                    resolved.primary(),
                    resolved.fallback(),
                    this::create,
                    MirroredSkillArtifactStore::new);
        };
    }

    public SkillStoreConfigValidationResult validate(SkillArtifactStoreConfig config) {
        SkillArtifactStoreConfig resolved = config == null ? SkillArtifactStoreConfig.memory() : config;
        return SkillStoreConfigValidationResult.combine(
                resolved.validate(),
                validateRuntimeDependencies(resolved));
    }

    private SkillStoreConfigValidationResult validateRuntimeDependencies(SkillArtifactStoreConfig config) {
        return switch (config.kind()) {
            case MEMORY, FILESYSTEM, OBJECT_STORAGE, JDBC, CUSTOM -> providers.validate(config.kind(), config);
            case HYBRID, MIRRORED -> SkillStoreFactorySupport.validatePrimaryFallback(
                    config.primary(),
                    config.fallback(),
                    this::validate);
        };
    }

    private SkillStoreProviderRegistry<
            SkillArtifactStoreConfig.Kind,
            SkillArtifactStoreConfig,
            SkillArtifactStore> providers() {
        return SkillStoreProviderRegistry
                .<SkillArtifactStoreConfig.Kind, SkillArtifactStoreConfig, SkillArtifactStore>builder()
                .register(
                        SkillArtifactStoreConfig.Kind.MEMORY,
                        config -> new InMemorySkillArtifactStore(),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillArtifactStoreConfig.Kind.FILESYSTEM,
                        config -> new FileSystemSkillArtifactStore(config.directory()),
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        SkillArtifactStoreConfig.Kind.OBJECT_STORAGE,
                        config -> new ObjectStorageSkillArtifactStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        objectStoreRequirement(config.kind())),
                                config.objectPrefix()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                objectStoreRequirement(config.kind())))
                .register(
                        SkillArtifactStoreConfig.Kind.JDBC,
                        config -> new JdbcSkillArtifactStore(
                                SkillStoreFactorySupport.requiredDependency(
                                        jdbcDataSourceRequirement(config.kind())),
                                config.jdbcTableName(),
                                config.initializeJdbcSchema()),
                        config -> SkillStoreFactorySupport.validateRequiredDependency(
                                jdbcDataSourceRequirement(config.kind())))
                .register(
                        SkillArtifactStoreConfig.Kind.CUSTOM,
                        config -> SkillStoreFactorySupport.customStore(
                                customStores,
                                config.customStoreName(),
                                "artifact store"),
                        config -> SkillStoreFactorySupport.validateCustomStore(
                                customStores,
                                config.customStoreName(),
                                "artifact store"))
                .build();
    }

    private SkillStoreFactorySupport.DependencyRequirement<SkillManagementObjectStore> objectStoreRequirement(
            SkillArtifactStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(objectStore, "objectStore", kind, "Artifact store");
    }

    private SkillStoreFactorySupport.DependencyRequirement<DataSource> jdbcDataSourceRequirement(
            SkillArtifactStoreConfig.Kind kind) {
        return SkillStoreFactorySupport.dependencyRequirement(jdbcDataSource, "jdbcDataSource", kind, "Artifact store");
    }

    public static SkillArtifactStore create(
            SkillArtifactStoreConfig config,
            SkillManagementObjectStore objectStore) {
        return new SkillArtifactStoreFactory(objectStore).create(config);
    }
}
