package tech.kayys.wayang.storage.provider.gcs;

import java.util.Map;
import java.util.Properties;

/**
 * Typed Google Cloud Storage provider configuration.
 */
public record GcsStorageConfig(
        String bucketName,
        String projectId,
        String pathPrefix) {

    public static final String BUCKET_PROPERTY = "wayang.storage.gcs.bucket";
    public static final String PROJECT_ID_PROPERTY = "wayang.storage.gcs.project-id";
    public static final String PATH_PREFIX_PROPERTY = "wayang.storage.gcs.path-prefix";

    public static final String BUCKET_ENV = "WAYANG_STORAGE_GCS_BUCKET";
    public static final String PROJECT_ID_ENV = "WAYANG_STORAGE_GCS_PROJECT_ID";
    public static final String PATH_PREFIX_ENV = "WAYANG_STORAGE_GCS_PATH_PREFIX";

    public GcsStorageConfig {
        bucketName = require(bucketName, "bucketName");
        projectId = optional(projectId);
        pathPrefix = optional(pathPrefix);
    }

    public static GcsStorageConfig of(String bucketName, String pathPrefix) {
        return of(bucketName, "", pathPrefix);
    }

    public static GcsStorageConfig of(String bucketName, String projectId, String pathPrefix) {
        return new GcsStorageConfig(bucketName, projectId, pathPrefix);
    }

    public static GcsStorageConfig fromProperties(Properties properties) {
        return fromMap((Map<?, ?>) properties);
    }

    public static GcsStorageConfig fromMap(Map<?, ?> values) {
        return new GcsStorageConfig(
                value(values, BUCKET_PROPERTY),
                value(values, PROJECT_ID_PROPERTY),
                value(values, PATH_PREFIX_PROPERTY));
    }

    public static GcsStorageConfig fromEnvironment(Map<String, String> environment) {
        return new GcsStorageConfig(
                value(environment, BUCKET_ENV),
                value(environment, PROJECT_ID_ENV),
                value(environment, PATH_PREFIX_ENV));
    }

    private static String value(Map<?, ?> values, String key) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private static String require(String value, String name) {
        String resolved = optional(value);
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return resolved;
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
