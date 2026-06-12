package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configurable persistence strategy for Agentic Commerce Wayang state.
 */
public record AgenticCommerceWayangPersistenceConfig(
        String storageKind,
        String directory,
        AgenticCommerceObjectStoreConfig objectStoreConfig,
        AgenticCommerceDatabasePersistenceConfig databaseConfig,
        AgenticCommerceWayangPersistenceConfig primary,
        AgenticCommerceWayangPersistenceConfig fallback,
        boolean mirrorWritesToFallback) {

    public static final String STORAGE_FILE = FileAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String STORAGE_HYBRID = HybridAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String STORAGE_IN_MEMORY = InMemoryAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String STORAGE_OBJECT_STORE = ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String STORAGE_DATABASE = DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String DEFAULT_DIRECTORY = "agentic-commerce";

    public AgenticCommerceWayangPersistenceConfig {
        storageKind = normalizeStorageKind(storageKind, primary, fallback);
        directory = defaultDirectory(directory);
        if (STORAGE_HYBRID.equals(storageKind)) {
            if (primary == null || fallback == null) {
                throw new IllegalArgumentException("Hybrid Agentic Commerce persistence requires primary and fallback configs");
            }
            objectStoreConfig = null;
            databaseConfig = null;
        } else if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            if (objectStoreConfig == null) {
                throw new IllegalArgumentException("Object-store Agentic Commerce persistence requires object store config");
            }
            databaseConfig = null;
            directory = objectStoreConfig.keyPrefix().isBlank()
                    ? DEFAULT_DIRECTORY
                    : objectStoreConfig.keyPrefix();
            primary = null;
            fallback = null;
        } else if (STORAGE_DATABASE.equals(storageKind)) {
            if (databaseConfig == null) {
                throw new IllegalArgumentException("Database Agentic Commerce persistence requires database config");
            }
            objectStoreConfig = null;
            directory = databaseConfig.namespace();
            primary = null;
            fallback = null;
        } else {
            objectStoreConfig = null;
            databaseConfig = null;
            if (STORAGE_IN_MEMORY.equals(storageKind)) {
                directory = "";
            }
            primary = null;
            fallback = null;
        }
    }

    public static AgenticCommerceWayangPersistenceConfig defaults() {
        return file(DEFAULT_DIRECTORY);
    }

    public static AgenticCommerceWayangPersistenceConfig file(String directory) {
        return new AgenticCommerceWayangPersistenceConfig(STORAGE_FILE, directory, null, null, null, null, true);
    }

    public static AgenticCommerceWayangPersistenceConfig memory() {
        return new AgenticCommerceWayangPersistenceConfig(STORAGE_IN_MEMORY, "", null, null, null, null, true);
    }

    public static AgenticCommerceWayangPersistenceConfig objectStore(
            AgenticCommerceObjectStoreConfig objectStoreConfig) {
        AgenticCommerceObjectStoreConfig config = java.util.Objects.requireNonNull(
                objectStoreConfig,
                "objectStoreConfig");
        return new AgenticCommerceWayangPersistenceConfig(
                STORAGE_OBJECT_STORE,
                config.keyPrefix(),
                config,
                null,
                null,
                null,
                true);
    }

    public static AgenticCommerceWayangPersistenceConfig database(
            AgenticCommerceDatabasePersistenceConfig databaseConfig) {
        AgenticCommerceDatabasePersistenceConfig config = java.util.Objects.requireNonNull(
                databaseConfig,
                "databaseConfig");
        return new AgenticCommerceWayangPersistenceConfig(
                STORAGE_DATABASE,
                config.namespace(),
                null,
                config,
                null,
                null,
                true);
    }

    public static AgenticCommerceWayangPersistenceConfig hybrid(
            AgenticCommerceWayangPersistenceConfig primary,
            AgenticCommerceWayangPersistenceConfig fallback,
            boolean mirrorWritesToFallback) {
        return new AgenticCommerceWayangPersistenceConfig(
                STORAGE_HYBRID,
                DEFAULT_DIRECTORY,
                null,
                null,
                primary,
                fallback,
                mirrorWritesToFallback);
    }

    public static AgenticCommerceWayangPersistenceConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Optional<AgenticCommerceWayangPersistenceConfig> primary = nestedConfig(
                resolved,
                "primary",
                "primaryStore",
                "primaryPersistence");
        Optional<AgenticCommerceWayangPersistenceConfig> fallback = nestedConfig(
                resolved,
                "fallback",
                "fallbackStore",
                "fallbackPersistence");
        String rawStorageKind = AgenticCommerceWayangMaps.firstText(
                resolved,
                "storageKind",
                "kind",
                "type",
                "mode");
        String storageKind = rawStorageKind;
        if (storageKind.isBlank()) {
            storageKind = primary.isPresent() || fallback.isPresent() ? STORAGE_HYBRID : STORAGE_FILE;
        }
        storageKind = normalizeStorageKind(storageKind, primary.orElse(null), fallback.orElse(null));
        if (STORAGE_HYBRID.equals(storageKind)) {
            boolean mirrorWrites = AgenticCommerceWayangMaps.firstBoolean(
                    resolved,
                    "mirrorWritesToFallback",
                    "mirrorWrites",
                    "writeFallback").orElse(true);
            return hybrid(primary.orElse(null), fallback.orElse(null), mirrorWrites);
        }
        String directory = AgenticCommerceWayangMaps.firstText(
                resolved,
                "directory",
                "path",
                "baseDirectory",
                "root");
        if (STORAGE_FILE.equals(storageKind)) {
            return file(directory);
        }
        if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            Map<String, Object> objectStoreValues = new LinkedHashMap<>(resolved);
            objectStoreValues.putIfAbsent("provider", rawStorageKind);
            return objectStore(AgenticCommerceObjectStoreConfig.fromMap(objectStoreValues));
        }
        if (STORAGE_DATABASE.equals(storageKind)) {
            Map<String, Object> databaseValues = new LinkedHashMap<>(resolved);
            databaseValues.putIfAbsent("provider", rawStorageKind);
            return database(AgenticCommerceDatabasePersistenceConfig.fromMap(databaseValues));
        }
        return new AgenticCommerceWayangPersistenceConfig(storageKind, directory, null, null, null, null, true);
    }

    public AgenticCommerceWayangPersistenceStore buildStore() {
        return buildStore(AgenticCommerceWayangPersistenceStoreProviders.defaults());
    }

    public AgenticCommerceWayangPersistenceStore buildStore(AgenticCommerceObjectStoreClient objectStoreClient) {
        return buildStore(AgenticCommerceObjectStoreClientResolver.fixed(objectStoreClient));
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceDatabasePersistenceClient databasePersistenceClient) {
        return buildStore(AgenticCommerceDatabasePersistenceClientResolver.fixed(databasePersistenceClient));
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return buildStore(AgenticCommerceWayangPersistenceStoreProviders.defaults(), objectStoreClientResolver);
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        return buildStore(
                AgenticCommerceWayangPersistenceStoreProviders.defaults(),
                null,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceWayangPersistenceStoreProviders providers) {
        return buildStore(providers, null);
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceWayangPersistenceStoreProviders providers,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return buildStore(providers, objectStoreClientResolver, null);
    }

    public AgenticCommerceWayangPersistenceStore buildStore(
            AgenticCommerceWayangPersistenceStoreProviders providers,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        AgenticCommerceWayangPersistenceStoreProviders resolved = providers == null
                ? AgenticCommerceWayangPersistenceStoreProviders.defaults()
                : providers;
        return resolved.build(this, objectStoreClientResolver, databasePersistenceClientResolver);
    }

    public boolean hybrid() {
        return STORAGE_HYBRID.equals(storageKind);
    }

    public boolean objectStore() {
        return STORAGE_OBJECT_STORE.equals(storageKind);
    }

    public boolean database() {
        return STORAGE_DATABASE.equals(storageKind);
    }

    public AgenticCommerceWayangPersistenceTargetDescriptor targetDescriptor() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.fromConfig(this);
    }

    public AgenticCommerceWayangPersistenceConfigValidationReport validationReport() {
        return AgenticCommerceWayangPersistenceConfigValidationReport.from(this);
    }

    public AgenticCommerceWayangPersistenceConfigValidationReport validationReport(
            AgenticCommerceWayangPersistenceStoreProviders providers) {
        return AgenticCommerceWayangPersistenceConfigValidationReport.from(this, providers);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind);
        values.put("directory", directory);
        values.put("mirrorWritesToFallback", mirrorWritesToFallback);
        values.put("target", targetDescriptor().toMap());
        values.put("validation", validationReport().toMap());
        if (objectStoreConfig != null) {
            values.put("objectStore", objectStoreConfig.toMap());
        }
        if (databaseConfig != null) {
            values.put("database", databaseConfig.toMap());
        }
        if (primary != null) {
            values.put("primary", primary.toMap());
        }
        if (fallback != null) {
            values.put("fallback", fallback.toMap());
        }
        return Map.copyOf(values);
    }

    private static Optional<AgenticCommerceWayangPersistenceConfig> nestedConfig(
            Map<String, Object> values,
            String... keys) {
        Object nested = AgenticCommerceWayangMaps.first(values, keys);
        if (nested instanceof Map<?, ?> map) {
            return Optional.of(fromMap(map));
        }
        return Optional.empty();
    }

    private static String normalizeStorageKind(
            String storageKind,
            AgenticCommerceWayangPersistenceConfig primary,
            AgenticCommerceWayangPersistenceConfig fallback) {
        String normalized = AgenticCommerceWayangMaps.text(storageKind).toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            return primary != null || fallback != null ? STORAGE_HYBRID : STORAGE_FILE;
        }
        if ("files".equals(normalized) || "local-file".equals(normalized) || "local".equals(normalized)) {
            return STORAGE_FILE;
        }
        if ("memory".equals(normalized)
                || "inmemory".equals(normalized)
                || "in-memory".equals(normalized)
                || "mem".equals(normalized)
                || "ephemeral".equals(normalized)
                || "transient".equals(normalized)) {
            return STORAGE_IN_MEMORY;
        }
        if ("fallback".equals(normalized) || "primary-fallback".equals(normalized)) {
            return STORAGE_HYBRID;
        }
        if ("database".equals(normalized)
                || "db".equals(normalized)
                || "sql".equals(normalized)
                || "jdbc".equals(normalized)
                || "postgres".equals(normalized)
                || "postgresql".equals(normalized)
                || "pg".equals(normalized)) {
            return STORAGE_DATABASE;
        }
        if ("cloud".equals(normalized)
                || "cloud-storage".equals(normalized)
                || "object-storage".equals(normalized)
                || "objectstore".equals(normalized)
                || "s3".equals(normalized)
                || "aws-s3".equals(normalized)
                || "s3-compatible".equals(normalized)
                || "minio".equals(normalized)
                || "rustfs".equals(normalized)
                || "rust-fs".equals(normalized)) {
            return STORAGE_OBJECT_STORE;
        }
        return normalized;
    }

    private static String defaultDirectory(String directory) {
        String normalized = AgenticCommerceWayangMaps.text(directory);
        return normalized.isBlank() ? DEFAULT_DIRECTORY : normalized;
    }
}
