package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Production preflight report for readiness profile registry configuration and backing readers.
 *
 * <p>The report combines config validation, external reader provider discovery,
 * and source resolution into one operator-facing view. Missing database or
 * object-storage readers are treated as failures only when no built-in fallback
 * can keep the registry usable.</p>
 */
public record WayangPlatformReadinessProfileRegistryPreflightReport(
        WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics,
        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport providerDiscovery,
        WayangPlatformReadinessProfileRegistryResolution registryResolution) {

    public WayangPlatformReadinessProfileRegistryPreflightReport {
        configDiagnostics = configDiagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : configDiagnostics;
        providerDiscovery = providerDiscovery == null
                ? WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport.empty()
                : providerDiscovery;
        registryResolution = registryResolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : registryResolution;
    }

    public static WayangPlatformReadinessProfileRegistryPreflightReport of(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics configDiagnostics,
            WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport providerDiscovery,
            WayangPlatformReadinessProfileRegistryResolution registryResolution) {
        return new WayangPlatformReadinessProfileRegistryPreflightReport(
                configDiagnostics,
                providerDiscovery,
                registryResolution);
    }

    public boolean ready() {
        return issues().isEmpty();
    }

    public int exitCode() {
        return ready() ? 0 : 1;
    }

    public int issueCount() {
        return issues().size();
    }

    public int warningCount() {
        return warnings().size();
    }

    public boolean fallbackToBuiltIn() {
        return configDiagnostics.config().fallbackToBuiltIn();
    }

    public boolean fallbackUsed() {
        return registryResolution.fallbackUsed();
    }

    public boolean registryReady() {
        return registryResolution.valid() && registryResolution.totalProfiles() > 0;
    }

    public String message() {
        if (!ready()) {
            return "Readiness profile registry preflight failed.";
        }
        if (warningCount() > 0) {
            return "Readiness profile registry preflight passed with warnings.";
        }
        return "Readiness profile registry preflight passed.";
    }

    public List<Map<String, Object>> issues() {
        java.util.ArrayList<Map<String, Object>> issues = new java.util.ArrayList<>();
        configDiagnostics.issues().stream()
                .map(this::configIssue)
                .forEach(issues::add);
        if (externalReaderRequiredButMissing() && !fallbackToBuiltIn()) {
            issues.add(externalReaderIssue());
        }
        if (!registryReady()) {
            issues.add(registryIssue());
        }
        return List.copyOf(issues);
    }

    public List<Map<String, Object>> warnings() {
        java.util.ArrayList<Map<String, Object>> warnings = new java.util.ArrayList<>();
        if (externalReaderRequiredButMissing() && fallbackToBuiltIn()) {
            warnings.add(externalReaderFallbackWarning());
        }
        if (fallbackUsed()) {
            warnings.add(fallbackUsedWarning());
        }
        return List.copyOf(warnings);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("exitCode", exitCode());
        values.put("message", message());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("configValid", configDiagnostics.valid());
        values.put("fallbackToBuiltIn", fallbackToBuiltIn());
        values.put("providerDiscoveryRequired", providerDiscovery.required());
        values.put("providerDiscoveryReady", providerDiscovery.ready());
        values.put("requiredReaderTypes", providerDiscovery.requiredReaderTypes());
        values.put("missingRequiredReaderTypes", providerDiscovery.missingRequiredReaderTypes());
        values.put("registryReady", registryReady());
        values.put("registryValid", registryResolution.valid());
        values.put("fallbackUsed", fallbackUsed());
        values.put("activeSourceId", registryResolution.activeSourceId());
        values.put("activeSourceType", registryResolution.activeSourceType());
        values.put("activeSourceLocation", registryResolution.activeSourceLocation());
        values.put("totalProfiles", registryResolution.totalProfiles());
        values.put("profileIds", registryResolution.profiles().stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("issues", issues());
        values.put("warnings", warnings());
        values.put("config", configDiagnostics.toMap());
        values.put("providerDiscovery", providerDiscovery.toMap());
        values.put("registryResolution", registryResolutionMap());
        return SdkMaps.orderedCopy(values);
    }

    private boolean externalReaderRequiredButMissing() {
        return providerDiscovery.required() && !providerDiscovery.ready();
    }

    private Map<String, Object> configIssue(
            WayangPlatformReadinessProfileRegistryConfigIssue issue) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("field", issue.field());
        return WayangReadinessReports.issue(
                issue.code(),
                "readiness-profile-registry-preflight",
                issue.message(),
                fields);
    }

    private Map<String, Object> externalReaderIssue() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requiredReaderTypes", providerDiscovery.requiredReaderTypes());
        fields.put("missingRequiredReaderTypes", providerDiscovery.missingRequiredReaderTypes());
        fields.put("providerCount", providerDiscovery.providerCount());
        fields.put("availableProviderCount", providerDiscovery.availableProviderCount());
        fields.put("fallbackToBuiltIn", fallbackToBuiltIn());
        return WayangReadinessReports.issue(
                "readiness_profile_external_reader_missing",
                "readiness-profile-registry-preflight",
                "Configured readiness profile registry requires external readers that are not available.",
                fields);
    }

    private Map<String, Object> registryIssue() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("activeSourceId", registryResolution.activeSourceId());
        fields.put("activeSourceType", registryResolution.activeSourceType());
        fields.put("activeSourceLocation", registryResolution.activeSourceLocation());
        fields.put("fallbackUsed", fallbackUsed());
        fields.put("sourceCount", registryResolution.sourceCount());
        fields.put("totalProfiles", registryResolution.totalProfiles());
        return WayangReadinessReports.issue(
                registryResolution.totalProfiles() == 0
                        ? "readiness_profile_registry_empty"
                        : "readiness_profile_registry_unavailable",
                "readiness-profile-registry-preflight",
                "Configured readiness profile registry did not resolve usable profiles.",
                fields);
    }

    private Map<String, Object> externalReaderFallbackWarning() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requiredReaderTypes", providerDiscovery.requiredReaderTypes());
        fields.put("missingRequiredReaderTypes", providerDiscovery.missingRequiredReaderTypes());
        fields.put("fallbackToBuiltIn", true);
        return warning(
                "readiness_profile_external_reader_missing_with_fallback",
                "External readiness profile readers are missing, but built-in fallback is enabled.",
                fields);
    }

    private Map<String, Object> registryResolutionMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", registryResolution.valid());
        values.put("activeSourceId", registryResolution.activeSourceId());
        values.put("activeSourceType", registryResolution.activeSourceType());
        values.put("activeSourceLocation", registryResolution.activeSourceLocation());
        values.put("fallbackUsed", registryResolution.fallbackUsed());
        values.put("sourceCount", registryResolution.sourceCount());
        values.put("totalProfiles", registryResolution.totalProfiles());
        values.put("profileIds", registryResolution.profiles().stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("sources", registryResolution.sources().stream()
                .map(WayangPlatformReadinessProfileRegistryPreflightReport::sourceMap)
                .toList());
        values.put("validation", validationMap(registryResolution.validation()));
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> sourceMap(
            WayangPlatformReadinessProfileSourceStatus source) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceId", source.sourceId());
        values.put("sourceType", source.sourceType());
        values.put("location", source.location());
        values.put("selected", source.selected());
        values.put("fallback", source.fallback());
        values.put("available", source.available());
        values.put("valid", source.valid());
        values.put("profileCount", source.profileCount());
        values.put("issueCount", source.issueCount());
        values.put("message", source.message());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> validationMap(
            WayangPlatformReadinessProfileValidationReport validation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", validation.valid());
        values.put("issueCount", validation.issueCount());
        values.put("totalProfiles", validation.totalProfiles());
        values.put("profileIds", validation.profileIds());
        values.put("validationPolicyId", validation.validationPolicy().policyId());
        values.put("defaultProfileIds", validation.defaultProfileIds());
        values.put("productionProfileIds", validation.productionProfileIds());
        values.put("coveredReadinessIds", validation.coveredReadinessIds());
        values.put("uncoveredReadinessIds", validation.uncoveredReadinessIds());
        values.put("issues", validation.issues().stream()
                .map(WayangPlatformReadinessProfileRegistryPreflightReport::validationIssueMap)
                .toList());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> validationIssueMap(
            WayangPlatformReadinessProfileValidationIssue issue) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", issue.kind());
        values.put("message", issue.message());
        values.put("profileId", issue.profileId());
        values.put("readinessId", issue.readinessId());
        return WayangReportMaps.copyMap(values);
    }

    private Map<String, Object> fallbackUsedWarning() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("activeSourceId", registryResolution.activeSourceId());
        fields.put("activeSourceType", registryResolution.activeSourceType());
        fields.put("activeSourceLocation", registryResolution.activeSourceLocation());
        return warning(
                "readiness_profile_registry_fallback_used",
                "Readiness profile registry used a fallback source.",
                fields);
    }

    private static Map<String, Object> warning(
            String code,
            String message,
            Map<String, Object> fields) {
        Map<String, Object> values = new LinkedHashMap<>(WayangReadinessReports.issue(
                code,
                "readiness-profile-registry-preflight",
                message,
                fields));
        values.put("severity", "warning");
        return WayangReportMaps.copyMap(values);
    }
}
