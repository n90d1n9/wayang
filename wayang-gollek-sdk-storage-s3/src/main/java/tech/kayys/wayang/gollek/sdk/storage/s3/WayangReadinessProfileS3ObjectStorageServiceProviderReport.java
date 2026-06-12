package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator-facing diagnostics for S3-compatible object-storage service discovery.
 */
public record WayangReadinessProfileS3ObjectStorageServiceProviderReport(
        WayangObjectStorageConfig objectStorage,
        boolean supportedProvider,
        boolean readinessProfileObjectConfigured,
        WayangReadinessProfileS3CredentialsResolution credentialsResolution,
        List<Map<String, Object>> credentialSourceDiagnostics,
        boolean serviceCreated,
        String serviceId,
        List<String> issues,
        String message) {

    public WayangReadinessProfileS3ObjectStorageServiceProviderReport {
        objectStorage = objectStorage == null ? WayangObjectStorageConfig.none() : objectStorage;
        credentialsResolution = credentialsResolution == null
                ? new WayangReadinessProfileS3CredentialsResolution(
                        "",
                        objectStorage.provider(),
                        "",
                        "none",
                        false,
                        List.of(),
                        "S3 readiness profile credentials were not evaluated.")
                : credentialsResolution;
        credentialSourceDiagnostics = credentialSourceDiagnostics == null || credentialSourceDiagnostics.isEmpty()
                ? List.of()
                : credentialSourceDiagnostics.stream()
                        .map(WayangReadinessProfileS3ObjectStorageServiceProviderReport::copyMap)
                        .toList();
        serviceId = trimToEmpty(serviceId);
        issues = copyIssues(issues);
        message = redact(message);
    }

    public boolean available() {
        return supportedProvider
                && readinessProfileObjectConfigured
                && credentialsResolution.available()
                && serviceCreated
                && issues.isEmpty();
    }

    public int exitCode() {
        return available() ? 0 : 1;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("available", available());
        values.put("exitCode", exitCode());
        values.put("message", message);
        values.put("objectStorage", objectStorage.toMap());
        values.put("supportedProvider", supportedProvider);
        values.put("readinessProfileObjectConfigured", readinessProfileObjectConfigured);
        values.put("credentialsResolution", credentialsResolution.toMap());
        values.put("credentialSourceDiagnostics", credentialSourceDiagnostics);
        values.put("serviceCreated", serviceCreated);
        values.put("serviceId", redact(serviceId));
        values.put("issues", issues);
        return java.util.Collections.unmodifiableMap(values);
    }

    static WayangReadinessProfileS3ObjectStorageServiceProviderReport from(
            WayangObjectStorageConfig objectStorage,
            boolean supportedProvider,
            boolean readinessProfileObjectConfigured,
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            List<Map<String, Object>> credentialSourceDiagnostics,
            boolean serviceCreated,
            String serviceId,
            Throwable serviceFailure) {
        List<String> issues = issues(
                objectStorage,
                supportedProvider,
                readinessProfileObjectConfigured,
                credentialsResolution,
                serviceFailure);
        return new WayangReadinessProfileS3ObjectStorageServiceProviderReport(
                objectStorage,
                supportedProvider,
                readinessProfileObjectConfigured,
                credentialsResolution,
                credentialSourceDiagnostics,
                serviceCreated,
                serviceId,
                issues,
                message(supportedProvider, readinessProfileObjectConfigured, credentialsResolution, serviceCreated, issues));
    }

    private static List<String> issues(
            WayangObjectStorageConfig objectStorage,
            boolean supportedProvider,
            boolean readinessProfileObjectConfigured,
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            Throwable serviceFailure) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        if (!supportedProvider) {
            values.add("Object-storage provider is not S3-compatible: " + objectStorage.provider() + ".");
        }
        if (!readinessProfileObjectConfigured) {
            values.add("Readiness profile object-storage bucket and object key must be configured.");
        }
        if (credentialsResolution == null || !credentialsResolution.available()) {
            String message = credentialsResolution == null ? "" : credentialsResolution.message();
            values.add(message.isBlank()
                    ? "S3 readiness profile credentials are not available."
                    : message);
        }
        if (serviceFailure != null) {
            values.add("S3-compatible readiness profile object-storage service could not be created: "
                    + serviceFailure.getMessage());
        }
        return List.copyOf(values);
    }

    private static String message(
            boolean supportedProvider,
            boolean readinessProfileObjectConfigured,
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            boolean serviceCreated,
            List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "S3-compatible readiness profile object-storage service is available.";
        }
        if (!supportedProvider) {
            return "S3-compatible readiness profile object-storage service skipped unsupported provider.";
        }
        if (!readinessProfileObjectConfigured) {
            return "S3-compatible readiness profile object-storage service skipped incomplete object config.";
        }
        if (credentialsResolution == null || !credentialsResolution.available()) {
            return "S3-compatible readiness profile object-storage service could not resolve credentials.";
        }
        if (!serviceCreated) {
            return "S3-compatible readiness profile object-storage service could not be created.";
        }
        return "S3-compatible readiness profile object-storage service is unavailable.";
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static List<String> copyIssues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangReadinessProfileS3ObjectStorageServiceProviderReport::trimToEmpty)
                .filter(value -> !value.isBlank())
                .map(WayangReadinessProfileS3ObjectStorageServiceProviderReport::redact)
                .toList();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}
