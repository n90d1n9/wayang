package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;
import tech.kayys.wayang.storage.provider.s3.S3StorageConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Redacting credential source for S3-compatible readiness profile storage.
 *
 * <p>The source captures where access-key credentials came from, exposes
 * operator-safe diagnostics, and materializes credentials only for the S3
 * reader factory or registry.
 */
public final class WayangReadinessProfileS3CredentialSource {

    public static final String ACCESS_KEY_ID_PROPERTY =
            "wayang.readiness-profile.s3.access-key-id";
    public static final String SECRET_ACCESS_KEY_PROPERTY =
            "wayang.readiness-profile.s3.secret-access-key";
    public static final String OBJECT_ACCESS_KEY_ID_PROPERTY =
            "wayang.readiness-profile.object.access-key-id";
    public static final String OBJECT_SECRET_ACCESS_KEY_PROPERTY =
            "wayang.readiness-profile.object.secret-access-key";

    public static final String ACCESS_KEY_ID_ENV =
            "WAYANG_READINESS_PROFILE_S3_ACCESS_KEY_ID";
    public static final String SECRET_ACCESS_KEY_ENV =
            "WAYANG_READINESS_PROFILE_S3_SECRET_ACCESS_KEY";
    public static final String OBJECT_ACCESS_KEY_ID_ENV =
            "WAYANG_READINESS_PROFILE_OBJECT_ACCESS_KEY_ID";
    public static final String OBJECT_SECRET_ACCESS_KEY_ENV =
            "WAYANG_READINESS_PROFILE_OBJECT_SECRET_ACCESS_KEY";
    public static final String AWS_ACCESS_KEY_ID_ENV = "AWS_ACCESS_KEY_ID";
    public static final String AWS_SECRET_ACCESS_KEY_ENV = "AWS_SECRET_ACCESS_KEY";
    public static final String MINIO_ROOT_USER_ENV = "MINIO_ROOT_USER";
    public static final String MINIO_ROOT_PASSWORD_ENV = "MINIO_ROOT_PASSWORD";

    private static final List<String> ACCESS_KEY_ID_KEYS = List.of(
            ACCESS_KEY_ID_PROPERTY,
            OBJECT_ACCESS_KEY_ID_PROPERTY,
            S3StorageConfig.ACCESS_KEY_ID_PROPERTY,
            "accessKeyId",
            "access-key-id",
            "access_key_id",
            "accessKey",
            ACCESS_KEY_ID_ENV,
            OBJECT_ACCESS_KEY_ID_ENV,
            S3StorageConfig.ACCESS_KEY_ID_ENV,
            AWS_ACCESS_KEY_ID_ENV,
            MINIO_ROOT_USER_ENV);
    private static final List<String> SECRET_ACCESS_KEY_KEYS = List.of(
            SECRET_ACCESS_KEY_PROPERTY,
            OBJECT_SECRET_ACCESS_KEY_PROPERTY,
            S3StorageConfig.SECRET_ACCESS_KEY_PROPERTY,
            "secretAccessKey",
            "secret-access-key",
            "secret_access_key",
            "secretKey",
            SECRET_ACCESS_KEY_ENV,
            OBJECT_SECRET_ACCESS_KEY_ENV,
            S3StorageConfig.SECRET_ACCESS_KEY_ENV,
            AWS_SECRET_ACCESS_KEY_ENV,
            MINIO_ROOT_PASSWORD_ENV);
    private static final List<String> ENV_ACCESS_KEY_ID_KEYS = List.of(
            ACCESS_KEY_ID_ENV,
            OBJECT_ACCESS_KEY_ID_ENV,
            S3StorageConfig.ACCESS_KEY_ID_ENV,
            AWS_ACCESS_KEY_ID_ENV,
            MINIO_ROOT_USER_ENV);
    private static final List<String> ENV_SECRET_ACCESS_KEY_KEYS = List.of(
            SECRET_ACCESS_KEY_ENV,
            OBJECT_SECRET_ACCESS_KEY_ENV,
            S3StorageConfig.SECRET_ACCESS_KEY_ENV,
            AWS_SECRET_ACCESS_KEY_ENV,
            MINIO_ROOT_PASSWORD_ENV);

    private final String credentialsId;
    private final String sourceType;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String accessKeyIdKey;
    private final String secretAccessKeyKey;

    private WayangReadinessProfileS3CredentialSource(
            String credentialsId,
            String sourceType,
            SourceValue accessKeyId,
            SourceValue secretAccessKey) {
        this.credentialsId = normalizeCredentialsId(credentialsId);
        this.sourceType = optional(sourceType).isBlank()
                ? "unknown"
                : optional(sourceType).toLowerCase(Locale.ROOT);
        this.accessKeyId = accessKeyId.value();
        this.secretAccessKey = secretAccessKey.value();
        this.accessKeyIdKey = accessKeyId.key();
        this.secretAccessKeyKey = secretAccessKey.key();
    }

    public static WayangReadinessProfileS3CredentialSource fromMap(
            String credentialsId,
            Map<?, ?> values) {
        return new WayangReadinessProfileS3CredentialSource(
                credentialsId,
                "map",
                firstValue(values, ACCESS_KEY_ID_KEYS),
                firstValue(values, SECRET_ACCESS_KEY_KEYS));
    }

    public static WayangReadinessProfileS3CredentialSource fromMap(Map<?, ?> values) {
        return fromMap(WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID, values);
    }

    public static WayangReadinessProfileS3CredentialSource fromEnvironment(
            String credentialsId,
            Map<String, String> environment) {
        return new WayangReadinessProfileS3CredentialSource(
                credentialsId,
                "environment",
                firstValue(environment, ENV_ACCESS_KEY_ID_KEYS),
                firstValue(environment, ENV_SECRET_ACCESS_KEY_KEYS));
    }

    public static WayangReadinessProfileS3CredentialSource fromEnvironment(
            Map<String, String> environment) {
        return fromEnvironment(
                WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID,
                environment);
    }

    public String credentialsId() {
        return credentialsId;
    }

    public String sourceType() {
        return sourceType;
    }

    public boolean available() {
        return accessKeyIdConfigured() && secretAccessKeyConfigured();
    }

    public boolean accessKeyIdConfigured() {
        return !accessKeyId.isBlank();
    }

    public boolean secretAccessKeyConfigured() {
        return !secretAccessKey.isBlank();
    }

    public String accessKeyIdKey() {
        return accessKeyIdKey;
    }

    public String secretAccessKeyKey() {
        return secretAccessKeyKey;
    }

    public Optional<WayangReadinessProfileS3Credentials> credentials() {
        if (!available()) {
            return Optional.empty();
        }
        return Optional.of(WayangReadinessProfileS3Credentials.of(
                accessKeyId,
                secretAccessKey));
    }

    public String message() {
        if (available()) {
            return "S3 readiness profile credential source is complete.";
        }
        if (!accessKeyIdConfigured() && !secretAccessKeyConfigured()) {
            return "S3 readiness profile credential source is missing access key id and secret access key.";
        }
        if (!accessKeyIdConfigured()) {
            return "S3 readiness profile credential source is missing access key id.";
        }
        return "S3 readiness profile credential source is missing secret access key.";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("credentialsId", redact(credentialsId));
        values.put("sourceType", sourceType);
        values.put("available", available());
        values.put("accessKeyIdConfigured", accessKeyIdConfigured());
        values.put("secretAccessKeyConfigured", secretAccessKeyConfigured());
        values.put("accessKeyIdKey", redact(accessKeyIdKey));
        values.put("secretAccessKeyKey", redact(secretAccessKeyKey));
        values.put("message", redact(message()));
        return java.util.Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return toMap().toString();
    }

    private static SourceValue firstValue(Map<?, ?> values, List<String> keys) {
        if (values == null || values.isEmpty()) {
            return SourceValue.none();
        }
        SourceValue firstBlank = SourceValue.none();
        for (String key : keys) {
            SourceValue value = value(values, key);
            if (value.configured()) {
                return value;
            }
            if (firstBlank.empty() && !value.empty()) {
                firstBlank = value;
            }
        }
        return firstBlank;
    }

    private static SourceValue value(Map<?, ?> values, String key) {
        if (values.containsKey(key)) {
            return SourceValue.of(key, values.get(key));
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            Object entryKey = entry.getKey();
            if (entryKey != null && key.equalsIgnoreCase(entryKey.toString())) {
                return SourceValue.of(entryKey.toString(), entry.getValue());
            }
        }
        return SourceValue.none();
    }

    private static String normalizeCredentialsId(String value) {
        String resolved = optional(value);
        if (resolved.isBlank()) {
            return WayangReadinessProfileS3CredentialsRegistry.DEFAULT_CREDENTIALS_ID;
        }
        return resolved.toLowerCase(Locale.ROOT);
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }

    private record SourceValue(String key, String value) {

        private static SourceValue of(String key, Object value) {
            return new SourceValue(optional(key), value == null ? "" : value.toString().trim());
        }

        private static SourceValue none() {
            return new SourceValue("", "");
        }

        private boolean configured() {
            return !value.isBlank();
        }

        private boolean empty() {
            return key.isBlank() && value.isBlank();
        }
    }
}
