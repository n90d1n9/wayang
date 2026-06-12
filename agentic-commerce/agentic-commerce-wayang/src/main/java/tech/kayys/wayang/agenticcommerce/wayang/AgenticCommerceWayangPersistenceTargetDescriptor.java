package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-neutral description of where Agentic Commerce persistence documents live.
 */
public record AgenticCommerceWayangPersistenceTargetDescriptor(
        String storageKind,
        String targetKind,
        String provider,
        String location,
        boolean durable,
        boolean ephemeral,
        boolean cloudStorage,
        boolean database,
        boolean hybrid,
        boolean mirrorWritesToFallback,
        AgenticCommerceWayangPersistenceTargetDescriptor primary,
        AgenticCommerceWayangPersistenceTargetDescriptor fallback,
        Map<String, Object> details) {

    public AgenticCommerceWayangPersistenceTargetDescriptor {
        storageKind = AgenticCommerceWayangMaps.text(storageKind);
        targetKind = AgenticCommerceWayangMaps.text(targetKind);
        provider = AgenticCommerceWayangMaps.text(provider);
        location = AgenticCommerceWayangMaps.text(location);
        details = AgenticCommerceWayangMaps.copy(details);
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor fromConfig(
            AgenticCommerceWayangPersistenceConfig config) {
        AgenticCommerceWayangPersistenceConfig resolved = Objects.requireNonNull(config, "config");
        if (resolved.hybrid()) {
            return hybrid(
                    fromConfig(resolved.primary()),
                    fromConfig(resolved.fallback()),
                    resolved.mirrorWritesToFallback());
        }
        if (resolved.objectStore()) {
            return objectStore(resolved.objectStoreConfig());
        }
        if (resolved.database()) {
            return database(resolved.databaseConfig());
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(resolved.storageKind())) {
            return memory();
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_FILE.equals(resolved.storageKind())) {
            return file(resolved.directory());
        }
        return generic(resolved.storageKind(), resolved.directory());
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor fromStore(
            AgenticCommerceWayangPersistenceStore store) {
        AgenticCommerceWayangPersistenceStore resolved = Objects.requireNonNull(store, "store");
        if (resolved instanceof FileAgenticCommerceWayangPersistenceStore fileStore) {
            return file(fileStore.directory().toString());
        }
        if (resolved instanceof InMemoryAgenticCommerceWayangPersistenceStore) {
            return memory();
        }
        if (resolved instanceof ObjectStoreAgenticCommerceWayangPersistenceStore objectStore) {
            return objectStore(objectStore.config());
        }
        if (resolved instanceof DatabaseAgenticCommerceWayangPersistenceStore databaseStore) {
            return database(databaseStore.config());
        }
        if (resolved instanceof HybridAgenticCommerceWayangPersistenceStore hybridStore) {
            return hybrid(
                    fromStore(hybridStore.primary()),
                    fromStore(hybridStore.fallback()),
                    hybridStore.mirrorWritesToFallback());
        }
        return generic(resolved.storageKind(), "");
    }

    public static Map<String, Object> mapFromStatus(
            Map<?, ?> status,
            String storageKind) {
        Object target = status == null ? null : status.get("target");
        if (target instanceof Map<?, ?> targetMap) {
            return AgenticCommerceWayangMaps.copy(targetMap);
        }
        String resolvedStorageKind = AgenticCommerceWayangMaps.text(storageKind);
        if (resolvedStorageKind.isBlank()) {
            resolvedStorageKind = AgenticCommerceWayangMaps.firstText(status, "storageKind");
        }
        String location = AgenticCommerceWayangMaps.firstText(
                status,
                "directory",
                "runtimeConfigKey",
                "runtimeConfigPath",
                "runtimeConfigObjectKey");
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_FILE.equals(resolvedStorageKind)) {
            return file(location).toMap();
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(resolvedStorageKind)) {
            return memory().toMap();
        }
        Map<String, Object> objectStore = map(status, "objectStore");
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_OBJECT_STORE.equals(resolvedStorageKind)
                && !objectStore.isEmpty()) {
            return objectStore(AgenticCommerceObjectStoreConfig.fromMap(objectStore)).toMap();
        }
        Map<String, Object> database = map(status, "database");
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_DATABASE.equals(resolvedStorageKind)
                && !database.isEmpty()) {
            return database(AgenticCommerceDatabasePersistenceConfig.fromMap(database)).toMap();
        }
        return generic(resolvedStorageKind, location).toMap();
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor file(String directory) {
        String resolvedDirectory = AgenticCommerceWayangMaps.text(directory);
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                AgenticCommerceWayangPersistenceConfig.STORAGE_FILE,
                "file",
                "local-file",
                resolvedDirectory,
                true,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                Map.of("directory", resolvedDirectory));
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor memory() {
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY,
                "memory",
                "in-memory",
                "",
                false,
                true,
                false,
                false,
                false,
                false,
                null,
                null,
                Map.of("ephemeral", true));
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor objectStore(
            AgenticCommerceObjectStoreConfig config) {
        AgenticCommerceObjectStoreConfig resolved = Objects.requireNonNull(config, "config");
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                AgenticCommerceWayangPersistenceConfig.STORAGE_OBJECT_STORE,
                "object-store",
                resolved.provider(),
                objectStoreLocation(resolved),
                true,
                false,
                true,
                false,
                false,
                false,
                null,
                null,
                resolved.toMap());
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor database(
            AgenticCommerceDatabasePersistenceConfig config) {
        AgenticCommerceDatabasePersistenceConfig resolved = Objects.requireNonNull(config, "config");
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                AgenticCommerceWayangPersistenceConfig.STORAGE_DATABASE,
                "database",
                resolved.provider(),
                resolved.tableName() + "/" + resolved.namespace(),
                true,
                false,
                false,
                true,
                false,
                false,
                null,
                null,
                resolved.toMap());
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor hybrid(
            AgenticCommerceWayangPersistenceTargetDescriptor primary,
            AgenticCommerceWayangPersistenceTargetDescriptor fallback,
            boolean mirrorWritesToFallback) {
        AgenticCommerceWayangPersistenceTargetDescriptor resolvedPrimary =
                Objects.requireNonNull(primary, "primary");
        AgenticCommerceWayangPersistenceTargetDescriptor resolvedFallback =
                Objects.requireNonNull(fallback, "fallback");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("primaryStorageKind", resolvedPrimary.storageKind());
        details.put("fallbackStorageKind", resolvedFallback.storageKind());
        details.put("mirrorWritesToFallback", mirrorWritesToFallback);
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                AgenticCommerceWayangPersistenceConfig.STORAGE_HYBRID,
                "hybrid",
                "hybrid",
                resolvedPrimary.location() + " -> " + resolvedFallback.location(),
                resolvedPrimary.durable() || resolvedFallback.durable(),
                resolvedPrimary.ephemeral() && resolvedFallback.ephemeral(),
                resolvedPrimary.cloudStorage() || resolvedFallback.cloudStorage(),
                resolvedPrimary.database() || resolvedFallback.database(),
                true,
                mirrorWritesToFallback,
                resolvedPrimary,
                resolvedFallback,
                details);
    }

    public static AgenticCommerceWayangPersistenceTargetDescriptor generic(
            String storageKind,
            String location) {
        String resolvedStorageKind = AgenticCommerceWayangMaps.text(storageKind);
        String resolvedLocation = AgenticCommerceWayangMaps.text(location);
        return new AgenticCommerceWayangPersistenceTargetDescriptor(
                resolvedStorageKind,
                resolvedStorageKind,
                resolvedStorageKind,
                resolvedLocation,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                resolvedLocation.isBlank() ? Map.of() : Map.of("location", resolvedLocation));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind);
        values.put("targetKind", targetKind);
        values.put("provider", provider);
        values.put("location", location);
        values.put("durable", durable);
        values.put("ephemeral", ephemeral);
        values.put("cloudStorage", cloudStorage);
        values.put("database", database);
        values.put("hybrid", hybrid);
        if (hybrid) {
            values.put("mirrorWritesToFallback", mirrorWritesToFallback);
            values.put("primaryStorageKind", primary.storageKind());
            values.put("fallbackStorageKind", fallback.storageKind());
            values.put("primary", primary.toMap());
            values.put("fallback", fallback.toMap());
        }
        if (!details.isEmpty()) {
            values.put("details", details);
        }
        return Map.copyOf(values);
    }

    private static String objectStoreLocation(AgenticCommerceObjectStoreConfig config) {
        return config.keyPrefix().isBlank()
                ? config.bucket()
                : config.bucket() + "/" + config.keyPrefix();
    }

    private static Map<String, Object> map(Map<?, ?> values, String key) {
        if (values != null && values.get(key) instanceof Map<?, ?> map) {
            return AgenticCommerceWayangMaps.copy(map);
        }
        return Map.of();
    }
}
