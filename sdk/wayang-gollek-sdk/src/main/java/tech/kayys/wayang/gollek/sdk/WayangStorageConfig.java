package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Storage backend configuration for durable run, skill, and readiness data.
 */
public record WayangStorageConfig(
        WayangStorageBackend backend,
        String filePath,
        String databaseUrl,
        WayangObjectStorageConfig objectStorage,
        boolean fallbackToFile,
        String fallbackFilePath,
        AgentRunStoreRetentionPolicy retentionPolicy,
        AgentRunStoreBackupRetentionPolicy backupRetentionPolicy) {

    public static final String DEFAULT_FILE_PATH = "wayang-runs.properties";

    public WayangStorageConfig(
            WayangStorageBackend backend,
            String filePath,
            String databaseUrl,
            WayangObjectStorageConfig objectStorage,
            boolean fallbackToFile,
            String fallbackFilePath) {
        this(backend, filePath, databaseUrl, objectStorage, fallbackToFile, fallbackFilePath, null, null);
    }

    public WayangStorageConfig(
            WayangStorageBackend backend,
            String filePath,
            String databaseUrl,
            WayangObjectStorageConfig objectStorage,
            boolean fallbackToFile,
            String fallbackFilePath,
            AgentRunStoreRetentionPolicy retentionPolicy) {
        this(backend, filePath, databaseUrl, objectStorage, fallbackToFile, fallbackFilePath, retentionPolicy, null);
    }

    public WayangStorageConfig {
        backend = backend == null ? WayangStorageBackend.MEMORY : backend;
        filePath = SdkText.trimToEmpty(filePath);
        databaseUrl = SdkText.trimToEmpty(databaseUrl);
        objectStorage = objectStorage == null ? WayangObjectStorageConfig.none() : objectStorage;
        fallbackFilePath = SdkText.trimToEmpty(fallbackFilePath);
        retentionPolicy = retentionPolicy == null ? AgentRunStoreRetentionPolicy.defaults() : retentionPolicy;
        backupRetentionPolicy = backupRetentionPolicy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : backupRetentionPolicy;
        fallbackToFile = fallbackToFile || backend == WayangStorageBackend.HYBRID || !fallbackFilePath.isBlank();
        if (backend == WayangStorageBackend.FILE && filePath.isBlank()) {
            filePath = DEFAULT_FILE_PATH;
        }
        if (fallbackToFile && fallbackFilePath.isBlank()) {
            fallbackFilePath = filePath.isBlank() ? DEFAULT_FILE_PATH : filePath;
        }
    }

    public static WayangStorageConfig memory() {
        return new WayangStorageConfig(WayangStorageBackend.MEMORY, "", "", null, false, "", null);
    }

    public static WayangStorageConfig file(String path) {
        return new WayangStorageConfig(WayangStorageBackend.FILE, path, "", null, false, "", null);
    }

    public static WayangStorageConfig database(String databaseUrl, String fallbackFilePath) {
        return new WayangStorageConfig(
                WayangStorageBackend.DATABASE,
                "",
                databaseUrl,
                null,
                true,
                fallbackFilePath,
                null);
    }

    public static WayangStorageConfig objectStorage(
            WayangObjectStorageConfig objectStorage,
            String fallbackFilePath) {
        return new WayangStorageConfig(
                WayangStorageBackend.OBJECT_STORAGE,
                "",
                "",
                objectStorage,
                true,
                fallbackFilePath,
                null);
    }

    public static WayangStorageConfig hybrid(
            String databaseUrl,
            WayangObjectStorageConfig objectStorage,
            String fallbackFilePath) {
        return new WayangStorageConfig(
                WayangStorageBackend.HYBRID,
                "",
                databaseUrl,
                objectStorage,
                true,
                fallbackFilePath,
                null);
    }

    public static WayangStorageConfig fromRunStorePath(String runStorePath) {
        String path = SdkText.trimToEmpty(runStorePath);
        return path.isBlank() ? memory() : file(path);
    }

    public static WayangStorageConfig fromMap(Map<String, Object> values) {
        Map<String, Object> source = values == null ? Map.of() : values;
        String backendName = text(source, "backend", "type", "kind");
        String filePath = text(source, "filePath", "path", "runStorePath");
        String databaseUrl = text(source, "databaseUrl", "jdbcUrl", "url");
        WayangObjectStorageConfig objectStorage = objectStorage(source, backendName);
        WayangStorageBackend backend = backendName.isBlank()
                ? inferBackend(filePath, databaseUrl, objectStorage)
                : WayangStorageBackend.from(backendName);
        return new WayangStorageConfig(
                backend,
                filePath,
                databaseUrl,
                objectStorage,
                bool(source, "fallbackToFile", "fileFallback", "fallbackEnabled"),
                text(source, "fallbackFilePath", "fallbackPath"),
                AgentRunStoreRetentionPolicy.fromMap(source),
                AgentRunStoreBackupRetentionPolicy.fromMap(source));
    }

    public WayangStorageConfig withRetentionPolicy(AgentRunStoreRetentionPolicy policy) {
        return new WayangStorageConfig(
                backend,
                filePath,
                databaseUrl,
                objectStorage,
                fallbackToFile,
                fallbackFilePath,
                policy,
                backupRetentionPolicy);
    }

    public WayangStorageConfig withBackupRetentionPolicy(AgentRunStoreBackupRetentionPolicy policy) {
        return new WayangStorageConfig(
                backend,
                filePath,
                databaseUrl,
                objectStorage,
                fallbackToFile,
                fallbackFilePath,
                retentionPolicy,
                policy);
    }

    public String effectiveFilePath() {
        if (backend == WayangStorageBackend.FILE) {
            return filePath;
        }
        return fallbackToFile ? fallbackFilePath : "";
    }

    public boolean objectStorageConfigured() {
        return objectStorage.configured();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backend", backend.name().toLowerCase());
        if (!filePath.isBlank()) {
            values.put("filePath", filePath);
        }
        if (!databaseUrl.isBlank()) {
            values.put("databaseUrl", WayangSecretRedactor.connectionString(databaseUrl));
        }
        if (objectStorageConfigured()) {
            values.put("objectStorage", objectStorage.toMap());
        }
        values.put("fallbackToFile", fallbackToFile);
        if (!fallbackFilePath.isBlank()) {
            values.put("fallbackFilePath", fallbackFilePath);
        }
        values.put("retention", AgentRunStoreRetentionPolicy.toMap(retentionPolicy));
        values.put("backupRetention", AgentRunStoreBackupRetentionPolicy.toMap(backupRetentionPolicy));
        values.put("effectiveFilePath", effectiveFilePath());
        return SdkMaps.copy(values);
    }

    private static WayangStorageBackend inferBackend(
            String filePath,
            String databaseUrl,
            WayangObjectStorageConfig objectStorage) {
        if (!databaseUrl.isBlank() && objectStorage.configured()) {
            return WayangStorageBackend.HYBRID;
        }
        if (!databaseUrl.isBlank()) {
            return WayangStorageBackend.DATABASE;
        }
        if (objectStorage.configured()) {
            return WayangStorageBackend.OBJECT_STORAGE;
        }
        if (!filePath.isBlank()) {
            return WayangStorageBackend.FILE;
        }
        return WayangStorageBackend.MEMORY;
    }

    private static WayangObjectStorageConfig objectStorage(Map<String, Object> source, String backendName) {
        Object nested = source.get("objectStorage");
        WayangObjectStorageConfig config;
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> values = new LinkedHashMap<>();
            nestedMap.forEach((key, value) -> values.put(String.valueOf(key), value));
            config = WayangObjectStorageConfig.fromMap(values);
        } else {
            config = WayangObjectStorageConfig.fromMap(source);
        }
        String provider = objectProviderAlias(backendName);
        if (provider.isBlank()) {
            return config;
        }
        return new WayangObjectStorageConfig(
                provider,
                config.endpoint(),
                config.bucket(),
                config.region(),
                config.keyPrefix(),
                config.pathStyleAccess(),
                config.credentialsRef());
    }

    private static String objectProviderAlias(String backendName) {
        String normalized = SdkText.trimToEmpty(backendName).replace('-', '_').toLowerCase();
        return switch (normalized) {
            case "s3", "rustfs", "minio" -> normalized;
            default -> "";
        };
    }

    private static String text(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return SdkText.trimToEmpty(String.valueOf(value));
            }
        }
        return "";
    }

    private static boolean bool(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value != null) {
                return Boolean.parseBoolean(SdkText.trimToEmpty(String.valueOf(value)));
            }
        }
        return false;
    }
}
