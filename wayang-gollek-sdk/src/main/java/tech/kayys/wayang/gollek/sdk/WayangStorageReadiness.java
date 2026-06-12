package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Readiness assessment for configured storage backends and local fallback
 * persistence behavior.
 */
public final class WayangStorageReadiness {

    public static final String READINESS_ID = "wayang.storage.readiness";

    private static final String SOURCE = "storage";

    private WayangStorageReadiness() {
    }

    public static WayangReadinessReport assess(WayangStorageConfig storage) {
        WayangStorageConfig resolved = storage == null ? WayangStorageConfig.memory() : storage;
        List<Map<String, Object>> issues = issues(resolved);
        boolean ready = issues.isEmpty();
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                probes(resolved, issues),
                issues,
                attributes(resolved));
    }

    private static List<Map<String, Object>> probes(
            WayangStorageConfig storage,
            List<Map<String, Object>> issues) {
        return List.of(
                probe("storage.backend", true, true, storage.toMap()),
                probe("storage.database", databaseRequired(storage), !hasIssue(issues, "storage_database_url_missing"),
                        ordered("databaseUrlConfigured", !storage.databaseUrl().isBlank())),
                probe("storage.object_storage", objectStorageRequired(storage),
                        !hasIssue(issues, "storage_object_bucket_missing")
                                && !hasIssue(issues, "storage_object_endpoint_missing"),
                        storage.objectStorage().toMap()),
                probe("storage.file_fallback", fileFallbackRequired(storage),
                        !hasIssue(issues, "storage_file_fallback_missing"),
                        ordered(
                                "fallbackToFile", storage.fallbackToFile(),
                                "fallbackFilePath", storage.fallbackFilePath(),
                                "effectiveFilePath", storage.effectiveFilePath())),
                probe("storage.retention", retentionRequired(storage), true,
                        retentionAttributes(storage.retentionPolicy())));
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

    private static Map<String, Object> attributes(WayangStorageConfig storage) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storage", storage.toMap());
        values.put("backend", backend(storage));
        values.put("persistent", storage.backend() != WayangStorageBackend.MEMORY);
        values.put("fileFallbackEnabled", storage.fallbackToFile());
        values.put("effectiveFilePath", storage.effectiveFilePath());
        values.put("objectStorageConfigured", storage.objectStorageConfigured());
        values.put("databaseConfigured", !storage.databaseUrl().isBlank());
        values.put("retention", retentionAttributes(storage.retentionPolicy()));
        values.put("backupRetention", AgentRunStoreBackupRetentionPolicy.toMap(storage.backupRetentionPolicy()));
        return WayangReportMaps.copyMap(values);
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
