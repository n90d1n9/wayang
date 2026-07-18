package tech.kayys.wayang.readiness;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangObjectStorageConfig;
import tech.kayys.wayang.client.WayangSecretRedactor;

public record WayangPlatformReadinessProfileRegistryConfig(
        WayangPlatformReadinessProfileRegistryMode mode,
        String filePath,
        String databaseUrl,
        WayangObjectStorageConfig objectStorage,
        boolean fallbackToBuiltIn,
        String validationPolicyId) {

    public WayangPlatformReadinessProfileRegistryConfig {
        mode = mode == null ? inferMode(filePath, databaseUrl, objectStorage) : mode;
        filePath = SdkText.trimToEmpty(filePath);
        databaseUrl = SdkText.trimToEmpty(databaseUrl);
        objectStorage = objectStorage == null ? WayangObjectStorageConfig.none() : objectStorage;
        fallbackToBuiltIn = fallbackToBuiltIn || mode == WayangPlatformReadinessProfileRegistryMode.HYBRID;
        validationPolicyId = SdkText.trimToDefault(
                validationPolicyId,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig builtin() {
        return new WayangPlatformReadinessProfileRegistryConfig(
                WayangPlatformReadinessProfileRegistryMode.BUILTIN,
                "",
                "",
                null,
                false,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig file(String path) {
        return new WayangPlatformReadinessProfileRegistryConfig(
                WayangPlatformReadinessProfileRegistryMode.FILE,
                path,
                "",
                null,
                false,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig hybridFile(String path) {
        return new WayangPlatformReadinessProfileRegistryConfig(
                WayangPlatformReadinessProfileRegistryMode.HYBRID,
                path,
                "",
                null,
                true,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig database(
            String databaseUrl,
            boolean fallbackToBuiltIn) {
        return new WayangPlatformReadinessProfileRegistryConfig(
                WayangPlatformReadinessProfileRegistryMode.DATABASE,
                "",
                databaseUrl,
                null,
                fallbackToBuiltIn,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig objectStorage(
            WayangObjectStorageConfig objectStorage,
            boolean fallbackToBuiltIn) {
        return new WayangPlatformReadinessProfileRegistryConfig(
                WayangPlatformReadinessProfileRegistryMode.OBJECT_STORAGE,
                "",
                "",
                objectStorage,
                fallbackToBuiltIn,
                WayangPlatformReadinessProfileValidationPolicies.STRICT);
    }

    public static WayangPlatformReadinessProfileRegistryConfig fromMap(Map<String, Object> values) {
        Map<String, Object> source = values == null ? Map.of() : values;
        String modeName = text(source, "mode", "backend", "source", "type");
        WayangPlatformReadinessProfileRegistryMode mode = modeName.isBlank()
                ? null
                : WayangPlatformReadinessProfileRegistryMode.from(modeName);
        return new WayangPlatformReadinessProfileRegistryConfig(
                mode,
                text(source, "filePath", "path", "profilePath", "readinessProfilePath"),
                text(source, "databaseUrl", "jdbcUrl", "url"),
                objectStorage(source, modeName),
                bool(source, "fallbackToBuiltIn", "builtinFallback", "fallbackEnabled"),
                text(source, "validationPolicyId", "validationPolicy", "policy"));
    }

    public WayangPlatformReadinessProfileRegistry registry() {
        return registry(WayangPlatformReadinessProfileExternalReaders.none());
    }

    public WayangPlatformReadinessProfileRegistry registry(
            WayangPlatformReadinessProfileObjectReader objectReader) {
        return registry(WayangPlatformReadinessProfileExternalReaders.objectStorage(objectReader));
    }

    public WayangPlatformReadinessProfileRegistry registry(
            WayangPlatformReadinessProfileExternalReaders readers) {
        WayangPlatformReadinessProfileExternalReaders resolvedReaders = readers == null
                ? WayangPlatformReadinessProfileExternalReaders.none()
                : readers;
        return WayangPlatformReadinessProfileRegistry.of(
                primarySource(resolvedReaders),
                fallbackToBuiltIn ? WayangPlatformReadinessProfileBuiltInSource.create() : null,
                validationPolicyOrDefault());
    }

    public WayangPlatformReadinessProfileValidationPolicy validationPolicy() {
        return WayangPlatformReadinessProfileValidationPolicies.policy(validationPolicyId);
    }

    public WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics() {
        List<WayangPlatformReadinessProfileRegistryConfigIssue> issues = new ArrayList<>();
        addModeIssues(issues);
        if (!WayangPlatformReadinessProfileValidationPolicies.policyIds().contains(validationPolicyId)) {
            issues.add(issue(
                    "readiness_profile_validation_policy_unknown",
                    "validationPolicyId",
                    "Unknown readiness profile validation policy '" + validationPolicyId + "'."));
        }
        return new WayangPlatformReadinessProfileRegistryConfigDiagnostics(this, issues);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode.name().toLowerCase());
        if (!filePath.isBlank()) {
            values.put("filePath", filePath);
        }
        if (!databaseUrl.isBlank()) {
            values.put("databaseUrl", WayangSecretRedactor.connectionString(databaseUrl));
        }
        if (objectStorage.configured()) {
            values.put("objectStorage", objectStorage.toMap());
        }
        values.put("fallbackToBuiltIn", fallbackToBuiltIn);
        values.put("validationPolicyId", validationPolicyId);
        return SdkMaps.copy(values);
    }

    private WayangPlatformReadinessProfileSource primarySource(
            WayangPlatformReadinessProfileExternalReaders readers) {
        return switch (mode) {
            case BUILTIN -> WayangPlatformReadinessProfileBuiltInSource.create();
            case FILE -> fileSource();
            case DATABASE -> databaseSource(readers.databaseReader());
            case OBJECT_STORAGE -> objectStorageSource(readers.objectReader());
            case HYBRID -> hybridSource(readers);
        };
    }

    private WayangPlatformReadinessProfileSource hybridSource(
            WayangPlatformReadinessProfileExternalReaders readers) {
        if (!filePath.isBlank()) {
            return fileSource();
        }
        if (objectStorage.configured()) {
            return objectStorageSource(readers.objectReader());
        }
        if (!databaseUrl.isBlank()) {
            return databaseSource(readers.databaseReader());
        }
        return WayangPlatformReadinessProfileBuiltInSource.create();
    }

    private WayangPlatformReadinessProfileSource fileSource() {
        if (filePath.isBlank()) {
            return WayangPlatformReadinessProfileUnavailableSource.of(
                    "file",
                    "file",
                    "",
                    "Readiness profile file path is not configured.");
        }
        return WayangPlatformReadinessProfileFileSource.of("file", Path.of(filePath));
    }

    private WayangPlatformReadinessProfileSource databaseSource(
            WayangPlatformReadinessProfileDatabaseReader databaseReader) {
        if (databaseUrl.isBlank()) {
            return WayangPlatformReadinessProfileUnavailableSource.of(
                    "database",
                    "database",
                    "",
                    "Readiness profile database URL is not configured.");
        }
        return WayangPlatformReadinessProfileDatabaseSource.of(databaseUrl, databaseReader);
    }

    private WayangPlatformReadinessProfileSource objectStorageSource(
            WayangPlatformReadinessProfileObjectReader objectReader) {
        if (objectStorage.bucket().isBlank()) {
            return WayangPlatformReadinessProfileUnavailableSource.of(
                    objectStorage.provider(),
                    "object_storage",
                    objectStorageLocation(),
                    "Readiness profile object-storage bucket is not configured.");
        }
        if (objectStorage.endpoint().isBlank() && endpointRequired(objectStorage.provider())) {
            return WayangPlatformReadinessProfileUnavailableSource.of(
                    objectStorage.provider(),
                    "object_storage",
                    objectStorageLocation(),
                    "Readiness profile object-storage endpoint is required for provider "
                            + objectStorage.provider()
                            + ".");
        }
        if (objectStorage.keyPrefix().isBlank()) {
            return WayangPlatformReadinessProfileUnavailableSource.of(
                    objectStorage.provider(),
                    "object_storage",
                    objectStorageLocation(),
                    "Readiness profile object-storage key is not configured.");
        }
        return WayangPlatformReadinessProfileObjectStorageSource.of(objectStorage, objectReader);
    }

    private String objectStorageLocation() {
        return WayangPlatformReadinessProfileObjectStorageSource.locationOf(objectStorage);
    }

    private WayangPlatformReadinessProfileValidationPolicy validationPolicyOrDefault() {
        if (WayangPlatformReadinessProfileValidationPolicies.policyIds().contains(validationPolicyId)) {
            return validationPolicy();
        }
        return WayangPlatformReadinessProfileValidationPolicies.defaultPolicy();
    }

    private void addModeIssues(List<WayangPlatformReadinessProfileRegistryConfigIssue> issues) {
        switch (mode) {
            case FILE -> {
                if (filePath.isBlank()) {
                    issues.add(issue(
                            "readiness_profile_file_path_required",
                            "filePath",
                            "Readiness profile file registry requires a file path."));
                }
            }
            case DATABASE -> {
                if (databaseUrl.isBlank()) {
                    issues.add(issue(
                            "readiness_profile_database_url_required",
                            "databaseUrl",
                            "Readiness profile database registry requires a database URL."));
                }
            }
            case OBJECT_STORAGE -> addObjectStorageIssues(issues);
            case BUILTIN, HYBRID -> {
            }
        }
    }

    private void addObjectStorageIssues(List<WayangPlatformReadinessProfileRegistryConfigIssue> issues) {
        if (objectStorage.bucket().isBlank()) {
            issues.add(issue(
                    "readiness_profile_object_bucket_required",
                    "objectStorage.bucket",
                    "Readiness profile object-storage registry requires a bucket."));
        }
        if (objectStorage.endpoint().isBlank() && endpointRequired(objectStorage.provider())) {
            issues.add(issue(
                    "readiness_profile_object_endpoint_required",
                    "objectStorage.endpoint",
                    "Readiness profile object-storage registry requires an endpoint for provider "
                            + objectStorage.provider()
                            + "."));
        }
        if (objectStorage.keyPrefix().isBlank()) {
            issues.add(issue(
                    "readiness_profile_object_key_required",
                    "objectStorage.keyPrefix",
                    "Readiness profile object-storage registry requires an object key."));
        }
    }

    private static boolean endpointRequired(String provider) {
        String normalized = SdkText.trimToEmpty(provider).toLowerCase();
        return normalized.equals("rustfs") || normalized.equals("minio");
    }

    private static WayangPlatformReadinessProfileRegistryConfigIssue issue(
            String code,
            String field,
            String message) {
        return new WayangPlatformReadinessProfileRegistryConfigIssue(code, field, message);
    }

    private static WayangPlatformReadinessProfileRegistryMode inferMode(
            String filePath,
            String databaseUrl,
            WayangObjectStorageConfig objectStorage) {
        if (!SdkText.trimToEmpty(databaseUrl).isBlank()
                && objectStorage != null
                && objectStorage.configured()) {
            return WayangPlatformReadinessProfileRegistryMode.HYBRID;
        }
        if (!SdkText.trimToEmpty(filePath).isBlank()) {
            return WayangPlatformReadinessProfileRegistryMode.FILE;
        }
        if (!SdkText.trimToEmpty(databaseUrl).isBlank()) {
            return WayangPlatformReadinessProfileRegistryMode.DATABASE;
        }
        if (objectStorage != null && objectStorage.configured()) {
            return WayangPlatformReadinessProfileRegistryMode.OBJECT_STORAGE;
        }
        return WayangPlatformReadinessProfileRegistryMode.BUILTIN;
    }

    private static WayangObjectStorageConfig objectStorage(Map<String, Object> source, String modeName) {
        Object nested = source.get("objectStorage");
        WayangObjectStorageConfig config;
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> values = new LinkedHashMap<>();
            nestedMap.forEach((key, value) -> values.put(String.valueOf(key), value));
            config = WayangObjectStorageConfig.fromMap(values);
        } else {
            config = WayangObjectStorageConfig.fromMap(source);
        }
        String provider = objectProviderAlias(modeName);
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

    private static String objectProviderAlias(String modeName) {
        String normalized = SdkText.trimToEmpty(modeName).replace('-', '_').toLowerCase();
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
