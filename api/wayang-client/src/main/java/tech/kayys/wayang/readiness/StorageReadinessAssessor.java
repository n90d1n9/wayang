package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.store.AgentRunStoreBackupRetentionPolicy;
import tech.kayys.wayang.agent.store.AgentRunStoreRetentionPolicy;
import tech.kayys.wayang.client.WayangReportMaps;
import tech.kayys.wayang.client.WayangStorageBackend;
import tech.kayys.wayang.client.WayangStorageConfig;
import tech.kayys.wayang.client.WayangObjectStorageConfig;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for storage configuration and persistence.
 * Evaluates whether storage backends are properly configured with required fallbacks.
 */
public class StorageReadinessAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.storage.readiness";

    private static final String SOURCE = "storage";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return SOURCE;
    }

    @Override
    protected String buildProbeName() {
        return "storage.backend";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangStorageConfig storage = (WayangStorageConfig) input;
        WayangStorageConfig resolved = storage == null ? WayangStorageConfig.memory() : storage;
        return issues(resolved);
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangStorageConfig storage = (WayangStorageConfig) input;
        WayangStorageConfig resolved = storage == null ? WayangStorageConfig.memory() : storage;

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storage", resolved.toMap());
        values.put("backend", backend(resolved));
        values.put("persistent", resolved.backend() != WayangStorageBackend.MEMORY);
        values.put("fileFallbackEnabled", resolved.fallbackToFile());
        values.put("effectiveFilePath", resolved.effectiveFilePath());
        values.put("objectStorageConfigured", resolved.objectStorageConfigured());
        values.put("databaseConfigured", !resolved.databaseUrl().isBlank());
        values.put("retention", retentionAttributes(resolved.retentionPolicy()));
        values.put("backupRetention", AgentRunStoreBackupRetentionPolicy.toMap(resolved.backupRetentionPolicy()));
        return WayangReportMaps.copyMap(values);
    }

    @Override
    protected List<Map<String, Object>> buildProbes(Object input, List<Map<String, Object>> issues) {
        WayangStorageConfig storage = (WayangStorageConfig) input;
        WayangStorageConfig resolved = storage == null ? WayangStorageConfig.memory() : storage;

        return List.of(
                probe("storage.backend", true, true, resolved.toMap()),
                probe("storage.database", databaseRequired(resolved), !hasIssue(issues, "storage_database_url_missing"),
                        ordered("databaseUrlConfigured", !resolved.databaseUrl().isBlank())),
                probe("storage.object_storage", objectStorageRequired(resolved),
                        !hasIssue(issues, "storage_object_bucket_missing")
                                && !hasIssue(issues, "storage_object_endpoint_missing"),
                        resolved.objectStorage().toMap()),
                probe("storage.file_fallback", fileFallbackRequired(resolved),
                        !hasIssue(issues, "storage_file_fallback_missing"),
                        ordered(
                                "fallbackToFile", resolved.fallbackToFile(),
                                "fallbackFilePath", resolved.fallbackFilePath(),
                                "effectiveFilePath", resolved.effectiveFilePath())),
                probe("storage.retention", retentionRequired(resolved), true,
                        retentionAttributes(resolved.retentionPolicy())));
    }

    private static List<Map<String, Object>> issues(WayangStorageConfig storage) {
        List<Map<String, Object>> issues = new ArrayList<>();
        if (databaseRequired(storage) && storage.databaseUrl().isBlank()) {
            issues.add(issue(
                    "storage_database_url_missing",
                    "Database-backed storage requires a database URL.",
                    ordered("backend", backend(storage))));
        }
        if (objectStorageRequired(storage) && storage.objectStorage().bucket().isBlank()) {
            issues.add(issue(
                    "storage_object_bucket_missing",
                    "Object storage requires a bucket.",
                    ordered("backend", backend(storage), "provider", storage.objectStorage().provider())));
        }
        if (endpointRequired(storage.objectStorage()) && storage.objectStorage().endpoint().isBlank()) {
            issues.add(issue(
                    "storage_object_endpoint_missing",
                    "RustFS and MinIO object storage require an endpoint.",
                    ordered("provider", storage.objectStorage().provider())));
        }
        if (fileFallbackRequired(storage) && storage.effectiveFilePath().isBlank()) {
            issues.add(issue(
                    "storage_file_fallback_missing",
                    "Production storage should declare a file fallback path.",
                    ordered("backend", backend(storage))));
        }
        return List.copyOf(issues);
    }

    private static Map<String, Object> probe(
            String name,
            boolean required,
            boolean passed,
            Map<String, Object> attributes) {
        return WayangReadinessReports.probe(name, required, passed, passed ? 0 : 1, attributes);
    }

    private static Map<String, Object> issue(
            String code,
            String message,
            Map<String, Object> fields) {
        return WayangReadinessReports.issue(code, SOURCE, message, fields);
    }

    private static boolean databaseRequired(WayangStorageConfig storage) {
        return storage.backend() == WayangStorageBackend.DATABASE
                || storage.backend() == WayangStorageBackend.HYBRID;
    }

    private static boolean objectStorageRequired(WayangStorageConfig storage) {
        return storage.backend() == WayangStorageBackend.OBJECT_STORAGE
                || storage.backend() == WayangStorageBackend.HYBRID;
    }

    private static boolean fileFallbackRequired(WayangStorageConfig storage) {
        return storage.backend() == WayangStorageBackend.DATABASE
                || storage.backend() == WayangStorageBackend.OBJECT_STORAGE
                || storage.backend() == WayangStorageBackend.HYBRID;
    }

    private static boolean retentionRequired(WayangStorageConfig storage) {
        return storage.backend() != WayangStorageBackend.MEMORY || storage.fallbackToFile();
    }

    private static Map<String, Object> retentionAttributes(AgentRunStoreRetentionPolicy policy) {
        return AgentRunStoreRetentionPolicy.toMap(policy);
    }

    private static boolean endpointRequired(WayangObjectStorageConfig objectStorage) {
        return objectStorage.configured()
                && List.of("rustfs", "minio").contains(objectStorage.provider());
    }

    private static boolean hasIssue(List<Map<String, Object>> issues, String code) {
        return issues.stream().anyMatch(issue -> code.equals(issue.get("code")));
    }

    private static String backend(WayangStorageConfig storage) {
        return storage.backend().name().toLowerCase();
    }

    private static Map<String, Object> ordered(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return WayangReportMaps.copyMap(values);
    }
}
