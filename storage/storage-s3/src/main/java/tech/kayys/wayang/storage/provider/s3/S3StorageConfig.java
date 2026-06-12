package tech.kayys.wayang.storage.provider.s3;

import java.util.Map;
import java.util.Properties;

/**
 * Typed S3-compatible object-storage provider configuration.
 */
public record S3StorageConfig(
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        String region,
        String endpoint,
        String pathPrefix,
        boolean pathStyleAccess) {

    public static final String ACCESS_KEY_ID_PROPERTY = "wayang.storage.s3.access-key-id";
    public static final String SECRET_ACCESS_KEY_PROPERTY = "wayang.storage.s3.secret-access-key";
    public static final String BUCKET_PROPERTY = "wayang.storage.s3.bucket";
    public static final String REGION_PROPERTY = "wayang.storage.s3.region";
    public static final String ENDPOINT_PROPERTY = "wayang.storage.s3.endpoint";
    public static final String PATH_PREFIX_PROPERTY = "wayang.storage.s3.path-prefix";
    public static final String PATH_STYLE_ACCESS_PROPERTY = "wayang.storage.s3.path-style-access";

    public static final String ACCESS_KEY_ID_ENV = "WAYANG_STORAGE_S3_ACCESS_KEY_ID";
    public static final String SECRET_ACCESS_KEY_ENV = "WAYANG_STORAGE_S3_SECRET_ACCESS_KEY";
    public static final String BUCKET_ENV = "WAYANG_STORAGE_S3_BUCKET";
    public static final String REGION_ENV = "WAYANG_STORAGE_S3_REGION";
    public static final String ENDPOINT_ENV = "WAYANG_STORAGE_S3_ENDPOINT";
    public static final String PATH_PREFIX_ENV = "WAYANG_STORAGE_S3_PATH_PREFIX";
    public static final String PATH_STYLE_ACCESS_ENV = "WAYANG_STORAGE_S3_PATH_STYLE_ACCESS";

    public S3StorageConfig {
        accessKeyId = require(accessKeyId, "accessKeyId");
        secretAccessKey = require(secretAccessKey, "secretAccessKey");
        bucketName = require(bucketName, "bucketName");
        region = require(region, "region");
        endpoint = optional(endpoint);
        pathPrefix = optional(pathPrefix);
    }

    public static S3StorageConfig of(
            String accessKeyId,
            String secretAccessKey,
            String bucketName,
            String region,
            String endpoint,
            String pathPrefix) {
        return of(accessKeyId, secretAccessKey, bucketName, region, endpoint, pathPrefix, false);
    }

    public static S3StorageConfig of(
            String accessKeyId,
            String secretAccessKey,
            String bucketName,
            String region,
            String endpoint,
            String pathPrefix,
            boolean pathStyleAccess) {
        return new S3StorageConfig(
                accessKeyId,
                secretAccessKey,
                bucketName,
                region,
                endpoint,
                pathPrefix,
                pathStyleAccess);
    }

    public static S3StorageConfig fromProperties(Properties properties) {
        return fromMap((Map<?, ?>) properties);
    }

    public static S3StorageConfig fromMap(Map<?, ?> values) {
        return new S3StorageConfig(
                value(values, ACCESS_KEY_ID_PROPERTY),
                value(values, SECRET_ACCESS_KEY_PROPERTY),
                value(values, BUCKET_PROPERTY),
                value(values, REGION_PROPERTY),
                value(values, ENDPOINT_PROPERTY),
                value(values, PATH_PREFIX_PROPERTY),
                booleanValue(values, PATH_STYLE_ACCESS_PROPERTY));
    }

    public static S3StorageConfig fromEnvironment(Map<String, String> environment) {
        return new S3StorageConfig(
                value(environment, ACCESS_KEY_ID_ENV),
                value(environment, SECRET_ACCESS_KEY_ENV),
                value(environment, BUCKET_ENV),
                value(environment, REGION_ENV),
                value(environment, ENDPOINT_ENV),
                value(environment, PATH_PREFIX_ENV),
                booleanValue(environment, PATH_STYLE_ACCESS_ENV));
    }

    private static String value(Map<?, ?> values, String key) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private static boolean booleanValue(Map<?, ?> values, String key) {
        String value = value(values, key);
        return !value.isBlank() && Boolean.parseBoolean(value.trim());
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
