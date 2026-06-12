package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configurable persistence strategy for transfer audit trails.
 */
public record AgenticCommerceWayangPersistenceTransferAuditConfig(
        String storageKind,
        String journalPath,
        int maxTrails,
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy,
        AgenticCommerceObjectStoreConfig objectStoreConfig,
        AgenticCommerceDatabasePersistenceConfig databaseConfig,
        List<AgenticCommerceWayangPersistenceTransferAuditConfig> children) {

    public static final String STORAGE_NOOP = "noop";
    public static final String STORAGE_IN_MEMORY = "in-memory";
    public static final String STORAGE_FILE = "file";
    public static final String STORAGE_COMPOSITE = "composite";
    public static final String STORAGE_OBJECT_STORE = ObjectStoreAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String STORAGE_DATABASE = DatabaseAgenticCommerceWayangPersistenceStore.STORAGE_KIND;
    public static final String DEFAULT_JOURNAL_PATH =
            AgenticCommerceWayangPersistenceConfig.DEFAULT_DIRECTORY + "/persistence-transfer-audit.jsonl";
    public static final int DEFAULT_MAX_TRAILS =
            InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS;

    public AgenticCommerceWayangPersistenceTransferAuditConfig(
            String storageKind,
            String journalPath,
            int maxTrails,
            AgenticCommerceObjectStoreConfig objectStoreConfig,
            AgenticCommerceDatabasePersistenceConfig databaseConfig,
            List<AgenticCommerceWayangPersistenceTransferAuditConfig> children) {
        this(
                storageKind,
                journalPath,
                maxTrails,
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(maxTrails),
                objectStoreConfig,
                databaseConfig,
                children);
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfig {
        List<AgenticCommerceWayangPersistenceTransferAuditConfig> normalizedChildren = normalizeChildren(children);
        storageKind = normalizeStorageKind(storageKind, normalizedChildren);
        retentionPolicy = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(maxTrails)
                : retentionPolicy;
        maxTrails = normalizeMaxTrails(retentionPolicy.maxTrails());
        if (STORAGE_COMPOSITE.equals(storageKind)) {
            if (normalizedChildren.isEmpty()) {
                throw new IllegalArgumentException(
                        "Composite Agentic Commerce persistence transfer audit requires child configs");
            }
            journalPath = "";
            objectStoreConfig = null;
            databaseConfig = null;
            children = normalizedChildren;
        } else if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            if (objectStoreConfig == null) {
                throw new IllegalArgumentException(
                        "Object-store Agentic Commerce persistence transfer audit requires object store config");
            }
            journalPath = normalizeJournalPath(journalPath, storageKind);
            databaseConfig = null;
            children = List.of();
        } else if (STORAGE_DATABASE.equals(storageKind)) {
            if (databaseConfig == null) {
                throw new IllegalArgumentException(
                        "Database Agentic Commerce persistence transfer audit requires database config");
            }
            journalPath = normalizeJournalPath(journalPath, storageKind);
            objectStoreConfig = null;
            children = List.of();
        } else {
            journalPath = normalizeJournalPath(journalPath, storageKind);
            objectStoreConfig = null;
            databaseConfig = null;
            children = List.of();
        }
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig defaults() {
        return memory();
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig noop() {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_NOOP,
                "",
                DEFAULT_MAX_TRAILS,
                null,
                null,
                List.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig memory() {
        return memory(DEFAULT_MAX_TRAILS);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig memory(int maxTrails) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_IN_MEMORY,
                "",
                maxTrails,
                null,
                null,
                List.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig file(Path journalPath) {
        return file(journalPath == null ? "" : journalPath.toString());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig file(String journalPath) {
        return file(journalPath, DEFAULT_MAX_TRAILS);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig file(String journalPath, int maxTrails) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_FILE,
                journalPath,
                maxTrails,
                null,
                null,
                List.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig objectStore(
            AgenticCommerceObjectStoreConfig objectStoreConfig) {
        return objectStore(objectStoreConfig, ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_OBJECT);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig objectStore(
            AgenticCommerceObjectStoreConfig objectStoreConfig,
            String auditObject) {
        return objectStore(objectStoreConfig, auditObject, DEFAULT_MAX_TRAILS);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig objectStore(
            AgenticCommerceObjectStoreConfig objectStoreConfig,
            String auditObject,
            int maxTrails) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_OBJECT_STORE,
                auditObject,
                maxTrails,
                java.util.Objects.requireNonNull(objectStoreConfig, "objectStoreConfig"),
                null,
                List.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig database(
            AgenticCommerceDatabasePersistenceConfig databaseConfig) {
        return database(databaseConfig, DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_DOCUMENT);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig database(
            AgenticCommerceDatabasePersistenceConfig databaseConfig,
            String auditDocument) {
        return database(databaseConfig, auditDocument, DEFAULT_MAX_TRAILS);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig database(
            AgenticCommerceDatabasePersistenceConfig databaseConfig,
            String auditDocument,
            int maxTrails) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_DATABASE,
                auditDocument,
                maxTrails,
                null,
                java.util.Objects.requireNonNull(databaseConfig, "databaseConfig"),
                List.of());
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig composite(
            List<AgenticCommerceWayangPersistenceTransferAuditConfig> children) {
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                STORAGE_COMPOSITE,
                "",
                DEFAULT_MAX_TRAILS,
                null,
                null,
                children);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        List<AgenticCommerceWayangPersistenceTransferAuditConfig> children = nestedConfigs(
                resolved,
                "children",
                "sinks",
                "stores",
                "auditSinks",
                "auditStores",
                "auditTargets");
        String storageKind = AgenticCommerceWayangMaps.firstText(
                resolved,
                "auditStorageKind",
                "auditKind",
                "storageKind",
                "kind",
                "type",
                "mode");
        if (storageKind.isBlank() && !children.isEmpty()) {
            storageKind = STORAGE_COMPOSITE;
        }
        if (storageKind.isBlank() && objectStoreConfigured(resolved)) {
            storageKind = STORAGE_OBJECT_STORE;
        }
        if (storageKind.isBlank() && databaseConfigured(resolved)) {
            storageKind = STORAGE_DATABASE;
        }
        String normalizedStorageKind = normalizeStorageKind(storageKind, children);
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.fromMap(
                        resolved,
                        DEFAULT_MAX_TRAILS);
        return new AgenticCommerceWayangPersistenceTransferAuditConfig(
                normalizedStorageKind,
                journalPathFrom(resolved, normalizedStorageKind),
                retentionPolicy.maxTrails(),
                retentionPolicy,
                objectStoreConfig(resolved, normalizedStorageKind, storageKind),
                databaseConfig(resolved, normalizedStorageKind, storageKind),
                children);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigSchema schema() {
        return AgenticCommerceWayangPersistenceTransferAuditConfigSchema.defaults();
    }

    public static AgenticCommerceWayangPersistenceTransferAuditConfigSchema schema(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        return AgenticCommerceWayangPersistenceTransferAuditConfigSchema.fromProviders(providers);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink() {
        return buildSink(AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults());
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders resolved = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        return resolved.build(this);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceObjectStoreClient objectStoreClient) {
        return buildSink(AgenticCommerceObjectStoreClientResolver.fixed(objectStoreClient));
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return buildSink(
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults(),
                objectStoreClientResolver,
                null);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceDatabasePersistenceClient databasePersistenceClient) {
        return buildSink(AgenticCommerceDatabasePersistenceClientResolver.fixed(databasePersistenceClient));
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        return buildSink(
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults(),
                null,
                databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver) {
        return buildSink(providers, objectStoreClientResolver, null);
    }

    public AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers,
            AgenticCommerceObjectStoreClientResolver objectStoreClientResolver,
            AgenticCommerceDatabasePersistenceClientResolver databasePersistenceClientResolver) {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders resolved = providers == null
                ? AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults()
                : providers;
        return resolved.build(this, objectStoreClientResolver, databasePersistenceClientResolver);
    }

    public AgenticCommerceWayangPersistenceTransferAuditReader buildReader() {
        return AgenticCommerceWayangPersistenceTransferAuditReader.forSink(buildSink());
    }

    public boolean noopStore() {
        return STORAGE_NOOP.equals(storageKind);
    }

    public boolean memoryStore() {
        return STORAGE_IN_MEMORY.equals(storageKind);
    }

    public boolean fileStore() {
        return STORAGE_FILE.equals(storageKind);
    }

    public boolean compositeStore() {
        return STORAGE_COMPOSITE.equals(storageKind);
    }

    public boolean objectStoreBacked() {
        return STORAGE_OBJECT_STORE.equals(storageKind);
    }

    public boolean databaseBacked() {
        return STORAGE_DATABASE.equals(storageKind);
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport validationReport() {
        return AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport.from(this);
    }

    public AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport validationReport(
            AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers) {
        return AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport.from(this, providers);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind);
        values.put("journalPath", journalPath);
        values.put("maxTrails", maxTrails);
        values.put("retentionPolicy", retentionPolicy.toMap());
        values.put("readable", !noopStore());
        values.put("fileBacked", fileStore());
        values.put("objectStoreBacked", objectStoreBacked());
        values.put("databaseBacked", databaseBacked());
        values.put("composite", compositeStore());
        values.put("validation", validationReport().toMap());
        if (objectStoreConfig != null) {
            values.put("objectStore", objectStoreConfig.toMap());
        }
        if (databaseConfig != null) {
            values.put("database", databaseConfig.toMap());
        }
        if (!children.isEmpty()) {
            values.put("children", children.stream()
                    .map(AgenticCommerceWayangPersistenceTransferAuditConfig::toMap)
                    .toList());
        }
        return Map.copyOf(values);
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditConfig> nestedConfigs(
            Map<String, Object> values,
            String... keys) {
        Object nested = AgenticCommerceWayangMaps.first(values, keys);
        if (!(nested instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(AgenticCommerceWayangPersistenceTransferAuditConfig::fromMap)
                .toList();
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditConfig> normalizeChildren(
            List<AgenticCommerceWayangPersistenceTransferAuditConfig> children) {
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        return children.stream()
                .filter(child -> child != null && !child.storageKind().isBlank())
                .toList();
    }

    private static String normalizeStorageKind(
            String storageKind,
            List<AgenticCommerceWayangPersistenceTransferAuditConfig> children) {
        String normalized = AgenticCommerceWayangMaps.text(storageKind).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return children == null || children.isEmpty() ? STORAGE_IN_MEMORY : STORAGE_COMPOSITE;
        }
        if ("none".equals(normalized)
                || "no-op".equals(normalized)
                || "disabled".equals(normalized)
                || "off".equals(normalized)) {
            return STORAGE_NOOP;
        }
        if ("memory".equals(normalized)
                || "inmemory".equals(normalized)
                || "mem".equals(normalized)
                || "ephemeral".equals(normalized)
                || "transient".equals(normalized)) {
            return STORAGE_IN_MEMORY;
        }
        if ("files".equals(normalized)
                || "filesystem".equals(normalized)
                || "file-system".equals(normalized)
                || "local-file".equals(normalized)
                || "local".equals(normalized)
                || "jsonl".equals(normalized)) {
            return STORAGE_FILE;
        }
        if ("multi".equals(normalized)
                || "fanout".equals(normalized)
                || "fan-out".equals(normalized)
                || "hybrid".equals(normalized)
                || "primary-fallback".equals(normalized)) {
            return STORAGE_COMPOSITE;
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
        if ("db".equals(normalized)
                || "sql".equals(normalized)
                || "jdbc".equals(normalized)
                || "postgres".equals(normalized)
                || "postgresql".equals(normalized)
                || "pg".equals(normalized)) {
            return STORAGE_DATABASE;
        }
        return normalized;
    }

    private static String normalizeJournalPath(String journalPath, String storageKind) {
        String normalized = AgenticCommerceWayangMaps.text(journalPath);
        if (STORAGE_FILE.equals(storageKind)) {
            return normalized.isBlank() ? DEFAULT_JOURNAL_PATH : normalized;
        }
        if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            if (normalized.isBlank()) {
                normalized = ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_OBJECT;
            }
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized.isBlank()
                    ? ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_OBJECT
                    : normalized;
        }
        if (STORAGE_DATABASE.equals(storageKind)) {
            if (normalized.isBlank()) {
                normalized = DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_DOCUMENT;
            }
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized.isBlank()
                    ? DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_DOCUMENT
                    : normalized;
        }
        return "";
    }

    private static String journalPathFrom(Map<String, Object> values, String storageKind) {
        String path;
        if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            path = AgenticCommerceWayangMaps.firstText(
                        values,
                        "journalObject",
                        "journalObjectKey",
                        "auditObject",
                        "auditObjectKey",
                        "objectKey",
                        "key",
                        "journalPath",
                        "journal",
                        "fileName");
        } else if (STORAGE_DATABASE.equals(storageKind)) {
            path = AgenticCommerceWayangMaps.firstText(
                        values,
                        "journalDocument",
                        "journalDocumentKey",
                        "auditDocument",
                        "auditDocumentKey",
                        "documentKey",
                        "key",
                        "journalPath",
                        "journal",
                        "fileName");
        } else {
            path = AgenticCommerceWayangMaps.firstText(
                        values,
                        "journalPath",
                        "journal",
                        "filePath",
                        "auditPath",
                        "path");
        }
        if (!path.isBlank()) {
            return path;
        }
        if (STORAGE_OBJECT_STORE.equals(storageKind)) {
            return ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_OBJECT;
        }
        if (STORAGE_DATABASE.equals(storageKind)) {
            return DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.DEFAULT_AUDIT_DOCUMENT;
        }
        String directory = AgenticCommerceWayangMaps.firstText(
                values,
                "directory",
                "baseDirectory",
                "root");
        if (directory.isBlank()) {
            return "";
        }
        String fileName = AgenticCommerceWayangMaps.firstText(values, "journalFile", "fileName");
        return Path.of(directory)
                .resolve(fileName.isBlank() ? Path.of(DEFAULT_JOURNAL_PATH).getFileName().toString() : fileName)
                .toString();
    }

    private static int normalizeMaxTrails(int maxTrails) {
        return maxTrails < 1 ? DEFAULT_MAX_TRAILS : maxTrails;
    }

    private static boolean objectStoreConfigured(Map<String, Object> values) {
        return values.containsKey("objectStore")
                || values.containsKey("objectStorage")
                || values.containsKey("s3")
                || values.containsKey("rustfs")
                || values.containsKey("cloudStorage");
    }

    private static boolean databaseConfigured(Map<String, Object> values) {
        return values.containsKey("database")
                || values.containsKey("db")
                || values.containsKey("jdbc")
                || values.containsKey("postgres")
                || values.containsKey("postgresql");
    }

    private static AgenticCommerceObjectStoreConfig objectStoreConfig(
            Map<String, Object> values,
            String storageKind,
            String rawStorageKind) {
        if (!STORAGE_OBJECT_STORE.equals(storageKind)) {
            return null;
        }
        Map<String, Object> objectStoreValues = new LinkedHashMap<>(values);
        objectStoreValues.putIfAbsent("provider", rawStorageKind);
        return AgenticCommerceObjectStoreConfig.fromMap(objectStoreValues);
    }

    private static AgenticCommerceDatabasePersistenceConfig databaseConfig(
            Map<String, Object> values,
            String storageKind,
            String rawStorageKind) {
        if (!STORAGE_DATABASE.equals(storageKind)) {
            return null;
        }
        Map<String, Object> databaseValues = new LinkedHashMap<>(values);
        databaseValues.putIfAbsent("provider", rawStorageKind);
        return AgenticCommerceDatabasePersistenceConfig.fromMap(databaseValues);
    }
}
