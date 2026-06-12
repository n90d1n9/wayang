package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfigDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryResolution;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileSourceStatus;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationIssue;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationReport;
import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator-facing report for readiness profile object-storage preflight checks.
 */
public record WayangReadinessProfileObjectStoragePreflightReport(
        WayangObjectStorageConfig objectStorage,
        String objectKey,
        WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics,
        WayangReadinessProfileObjectStorageServiceResolution serviceResolution,
        WayangPlatformReadinessProfileRegistryResolution registryResolution,
        boolean objectReadable,
        int documentBytes,
        int documentCharacters,
        List<String> profileIds,
        List<String> issues,
        String message) {

    public WayangReadinessProfileObjectStoragePreflightReport {
        objectStorage = objectStorage == null ? WayangObjectStorageConfig.none() : objectStorage;
        objectKey = trimToEmpty(objectKey);
        configDiagnostics = configDiagnostics == null
                ? tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig
                        .objectStorage(objectStorage, false)
                        .diagnostics()
                : configDiagnostics;
        serviceResolution = serviceResolution == null
                ? WayangReadinessProfileObjectStorageServiceRegistry.empty().resolveReport(objectStorage)
                : serviceResolution;
        documentBytes = Math.max(0, documentBytes);
        documentCharacters = Math.max(0, documentCharacters);
        profileIds = copy(profileIds);
        issues = copy(issues);
        message = redact(message);
    }

    static WayangReadinessProfileObjectStoragePreflightReport from(
            WayangObjectStorageConfig objectStorage,
            String objectKey,
            WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics,
            WayangReadinessProfileObjectStorageServiceResolution serviceResolution,
            WayangPlatformReadinessProfileRegistryResolution registryResolution,
            String document,
            boolean objectReadable,
            Charset charset,
            Throwable readFailure,
            List<String> skippedReasons) {
        String resolvedDocument = trimToEmpty(document);
        List<String> profileIds = registryResolution == null
                ? List.of()
                : registryResolution.profiles().stream()
                        .map(WayangPlatformReadinessProfileDescriptor::profileId)
                        .toList();
        List<String> issues = issues(configDiagnostics, serviceResolution, registryResolution, readFailure, skippedReasons);
        return new WayangReadinessProfileObjectStoragePreflightReport(
                objectStorage,
                objectKey,
                configDiagnostics,
                serviceResolution,
                registryResolution,
                objectReadable,
                bytes(resolvedDocument, charset),
                resolvedDocument.length(),
                profileIds,
                issues,
                message(objectReadable, registryResolution, issues));
    }

    public boolean ready() {
        return configDiagnostics.valid()
                && serviceResolution.available()
                && objectReadable
                && registryResolution != null
                && registryResolution.valid();
    }

    public int exitCode() {
        return ready() ? 0 : 1;
    }

    public int profileCount() {
        return profileIds.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("exitCode", exitCode());
        values.put("message", message);
        values.put("objectStorage", objectStorage.toMap());
        values.put("objectKey", objectKey);
        values.put("configDiagnostics", configDiagnostics.toMap());
        values.put("serviceResolution", serviceResolution.toMap());
        values.put("objectReadable", objectReadable);
        values.put("documentBytes", documentBytes);
        values.put("documentCharacters", documentCharacters);
        values.put("profileCount", profileCount());
        values.put("profileIds", profileIds);
        values.put("registryResolution", registryResolutionMap(registryResolution));
        values.put("issues", issues);
        return java.util.Collections.unmodifiableMap(values);
    }

    private static List<String> issues(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics,
            WayangReadinessProfileObjectStorageServiceResolution serviceResolution,
            WayangPlatformReadinessProfileRegistryResolution registryResolution,
            Throwable readFailure,
            List<String> skippedReasons) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        if (skippedReasons != null) {
            skippedReasons.stream()
                    .map(WayangReadinessProfileObjectStoragePreflightReport::trimToEmpty)
                    .filter(reason -> !reason.isBlank())
                    .forEach(values::add);
        }
        configDiagnostics.issues().stream()
                .map(issue -> issue.code() + ": " + issue.message())
                .forEach(values::add);
        if (!serviceResolution.available() && !serviceResolution.message().isBlank()) {
            values.add(serviceResolution.message());
        }
        if (readFailure != null) {
            values.add("Readiness profile object read failed: " + redact(readFailure.getMessage()));
        }
        if (readFailure == null
                && serviceResolution.available()
                && registryResolution == null
                && values.isEmpty()) {
            values.add("Readiness profile object document is empty.");
        }
        if (registryResolution != null && !registryResolution.valid()) {
            registryResolution.validation().issues().stream()
                    .map(issue -> issue.kind() + ": " + issue.message())
                    .forEach(values::add);
            registryResolution.sources().stream()
                    .filter(source -> !source.available())
                    .map(WayangPlatformReadinessProfileSourceStatus::message)
                    .map(WayangReadinessProfileObjectStoragePreflightReport::trimToEmpty)
                    .filter(message -> !message.isBlank())
                    .forEach(values::add);
        }
        return List.copyOf(values);
    }

    private static String message(
            boolean objectReadable,
            WayangPlatformReadinessProfileRegistryResolution registryResolution,
            List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "Readiness profile object-storage preflight passed.";
        }
        if (!objectReadable) {
            return "Readiness profile object-storage preflight could not read the configured object.";
        }
        if (registryResolution == null || !registryResolution.valid()) {
            return "Readiness profile object-storage preflight read the object but validation failed.";
        }
        return "Readiness profile object-storage preflight failed.";
    }

    private static Map<String, Object> registryResolutionMap(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        if (resolution == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", resolution.valid());
        values.put("activeSourceId", resolution.activeSourceId());
        values.put("activeSourceType", resolution.activeSourceType());
        values.put("activeSourceLocation", redact(resolution.activeSourceLocation()));
        values.put("fallbackUsed", resolution.fallbackUsed());
        values.put("sourceCount", resolution.sourceCount());
        values.put("totalProfiles", resolution.totalProfiles());
        values.put("profileIds", resolution.profiles().stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("sources", resolution.sources().stream()
                .map(WayangReadinessProfileObjectStoragePreflightReport::sourceStatusMap)
                .toList());
        values.put("validation", validationMap(resolution.validation()));
        return java.util.Collections.unmodifiableMap(values);
    }

    private static Map<String, Object> sourceStatusMap(WayangPlatformReadinessProfileSourceStatus source) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceId", source.sourceId());
        values.put("sourceType", source.sourceType());
        values.put("location", redact(source.location()));
        values.put("selected", source.selected());
        values.put("fallback", source.fallback());
        values.put("available", source.available());
        values.put("valid", source.valid());
        values.put("profileCount", source.profileCount());
        values.put("issueCount", source.issueCount());
        values.put("message", redact(source.message()));
        return java.util.Collections.unmodifiableMap(values);
    }

    private static Map<String, Object> validationMap(WayangPlatformReadinessProfileValidationReport validation) {
        if (validation == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", validation.valid());
        values.put("issueCount", validation.issueCount());
        values.put("totalProfiles", validation.totalProfiles());
        values.put("profileIds", validation.profileIds());
        values.put("defaultProfileIds", validation.defaultProfileIds());
        values.put("productionProfileIds", validation.productionProfileIds());
        values.put("coveredReadinessIds", validation.coveredReadinessIds());
        values.put("uncoveredReadinessIds", validation.uncoveredReadinessIds());
        values.put("issues", validation.issues().stream()
                .map(WayangReadinessProfileObjectStoragePreflightReport::validationIssueMap)
                .toList());
        return java.util.Collections.unmodifiableMap(values);
    }

    private static Map<String, Object> validationIssueMap(WayangPlatformReadinessProfileValidationIssue issue) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", issue.kind());
        values.put("message", redact(issue.message()));
        values.put("profileId", issue.profileId());
        values.put("readinessId", issue.readinessId());
        return java.util.Collections.unmodifiableMap(values);
    }

    private static int bytes(String document, Charset charset) {
        if (document.isBlank()) {
            return 0;
        }
        return document.getBytes(charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset).length;
    }

    private static List<String> copy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangReadinessProfileObjectStoragePreflightReport::trimToEmpty)
                .map(WayangReadinessProfileObjectStoragePreflightReport::redact)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}
