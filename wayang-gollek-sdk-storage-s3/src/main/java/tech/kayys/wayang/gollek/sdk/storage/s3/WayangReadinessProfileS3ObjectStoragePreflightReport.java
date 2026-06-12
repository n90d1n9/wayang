package tech.kayys.wayang.gollek.sdk.storage.s3;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;
import tech.kayys.wayang.gollek.sdk.storage.WayangReadinessProfileObjectStoragePreflightReport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redacted preflight report for S3-compatible readiness profile object storage.
 */
public record WayangReadinessProfileS3ObjectStoragePreflightReport(
        WayangObjectStorageConfig objectStorage,
        WayangReadinessProfileS3CredentialsResolution credentialsResolution,
        List<Map<String, Object>> credentialSourceDiagnostics,
        boolean serviceCreated,
        WayangReadinessProfileObjectStoragePreflightReport objectStoragePreflight,
        List<String> issues,
        String message) {

    public WayangReadinessProfileS3ObjectStoragePreflightReport {
        objectStorage = objectStorage == null ? WayangObjectStorageConfig.none() : objectStorage;
        credentialsResolution = credentialsResolution == null
                ? new WayangReadinessProfileS3CredentialsResolution(
                        "",
                        objectStorage.provider(),
                        "",
                        "none",
                        false,
                        List.of(),
                        "S3 readiness profile credentials were not resolved.")
                : credentialsResolution;
        credentialSourceDiagnostics = credentialSourceDiagnostics == null || credentialSourceDiagnostics.isEmpty()
                ? List.of()
                : credentialSourceDiagnostics.stream()
                        .map(WayangReadinessProfileS3ObjectStoragePreflightReport::copyMap)
                        .toList();
        issues = copyList(issues);
        message = redact(message);
    }

    static WayangReadinessProfileS3ObjectStoragePreflightReport from(
            WayangObjectStorageConfig objectStorage,
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            List<Map<String, Object>> credentialSourceDiagnostics,
            boolean serviceCreated,
            Throwable serviceFailure,
            WayangReadinessProfileObjectStoragePreflightReport objectStoragePreflight) {
        List<String> issues = issues(credentialsResolution, serviceFailure, objectStoragePreflight);
        return new WayangReadinessProfileS3ObjectStoragePreflightReport(
                objectStorage,
                credentialsResolution,
                credentialSourceDiagnostics,
                serviceCreated,
                objectStoragePreflight,
                issues,
                message(credentialsResolution, serviceCreated, objectStoragePreflight, issues));
    }

    public boolean ready() {
        return credentialsResolution.available()
                && serviceCreated
                && objectStoragePreflight != null
                && objectStoragePreflight.ready();
    }

    public int exitCode() {
        return ready() ? 0 : 1;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("exitCode", exitCode());
        values.put("message", message);
        values.put("objectStorage", objectStorage.toMap());
        values.put("credentialsResolution", credentialsResolution.toMap());
        values.put("credentialSourceDiagnostics", credentialSourceDiagnostics);
        values.put("serviceCreated", serviceCreated);
        values.put("objectStoragePreflight", objectStoragePreflight == null
                ? Map.of()
                : objectStoragePreflight.toMap());
        values.put("issues", issues);
        return java.util.Collections.unmodifiableMap(values);
    }

    private static List<String> issues(
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            Throwable serviceFailure,
            WayangReadinessProfileObjectStoragePreflightReport objectStoragePreflight) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        if (credentialsResolution == null || !credentialsResolution.available()) {
            String message = credentialsResolution == null ? "" : credentialsResolution.message();
            values.add(message.isBlank()
                    ? "S3 readiness profile credentials are not available."
                    : message);
        }
        if (serviceFailure != null) {
            values.add("S3 readiness profile object-storage service could not be created: "
                    + serviceFailure.getMessage());
        }
        if (objectStoragePreflight != null && !objectStoragePreflight.ready()) {
            values.addAll(objectStoragePreflight.issues());
        }
        return List.copyOf(values);
    }

    private static String message(
            WayangReadinessProfileS3CredentialsResolution credentialsResolution,
            boolean serviceCreated,
            WayangReadinessProfileObjectStoragePreflightReport objectStoragePreflight,
            List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "S3 readiness profile object-storage preflight passed.";
        }
        if (credentialsResolution == null || !credentialsResolution.available()) {
            return "S3 readiness profile object-storage preflight could not resolve credentials.";
        }
        if (!serviceCreated) {
            return "S3 readiness profile object-storage preflight could not create the storage service.";
        }
        if (objectStoragePreflight == null || !objectStoragePreflight.ready()) {
            return "S3 readiness profile object-storage preflight failed object or profile validation.";
        }
        return "S3 readiness profile object-storage preflight failed.";
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangReadinessProfileS3ObjectStoragePreflightReport::trimToEmpty)
                .filter(value -> !value.isBlank())
                .map(WayangReadinessProfileS3ObjectStoragePreflightReport::redact)
                .toList();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}
