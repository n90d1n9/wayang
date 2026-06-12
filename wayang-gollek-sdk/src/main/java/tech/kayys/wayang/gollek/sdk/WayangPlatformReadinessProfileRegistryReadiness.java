package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangPlatformReadinessProfileRegistryReadiness {

    public static final String READINESS_ID = "wayang.platform.readiness-profile-registry.readiness";

    private WayangPlatformReadinessProfileRegistryReadiness() {
    }

    public static WayangReadinessReport assess(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        WayangPlatformReadinessProfileRegistryResolution model = resolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : resolution;
        boolean ready = model.valid() && model.totalProfiles() > 0;
        List<Map<String, Object>> issues = ready ? List.of() : issues(model);
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                probes(model),
                issues,
                attributes(model));
    }

    private static List<Map<String, Object>> probes(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        return resolution.sources().stream()
                .map(source -> WayangReadinessReports.probe(
                        source.sourceId(),
                        source.selected() || !resolution.fallbackUsed(),
                        source.valid(),
                        source.issueCount(),
                        sourceAttributes(source)))
                .toList();
    }

    private static List<Map<String, Object>> issues(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        List<Map<String, Object>> sourceIssues = resolution.sources().stream()
                .filter(source -> source.selected() && !source.valid())
                .map(WayangPlatformReadinessProfileRegistryReadiness::sourceIssue)
                .toList();
        if (!sourceIssues.isEmpty()) {
            return sourceIssues;
        }
        if (resolution.totalProfiles() == 0) {
            return List.of(WayangReadinessReports.issue(
                    "readiness_profile_registry_empty",
                    READINESS_ID,
                    "No platform readiness profiles are available from the configured registry.",
                    issueFields(resolution)));
        }
        return resolution.validation().issues().stream()
                .map(issue -> WayangReadinessReports.issue(
                        "readiness_profile_registry_validation_issue",
                        READINESS_ID,
                        issue.message(),
                        validationIssueFields(resolution, issue)))
                .toList();
    }

    private static Map<String, Object> sourceIssue(
            WayangPlatformReadinessProfileSourceStatus source) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceId", source.sourceId());
        fields.put("sourceType", source.sourceType());
        fields.put("location", source.location());
        fields.put("available", source.available());
        fields.put("sourceIssueCount", source.issueCount());
        return WayangReadinessReports.issue(
                source.available()
                        ? "readiness_profile_source_invalid"
                        : "readiness_profile_source_unavailable",
                READINESS_ID,
                SdkText.trimToDefault(
                        source.message(),
                        "Configured readiness profile source is not usable."),
                fields);
    }

    private static Map<String, Object> issueFields(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("activeSourceId", resolution.activeSourceId());
        values.put("activeSourceType", resolution.activeSourceType());
        values.put("activeSourceLocation", resolution.activeSourceLocation());
        values.put("fallbackUsed", resolution.fallbackUsed());
        values.put("sourceCount", resolution.sourceCount());
        values.put("validationPolicyId", resolution.validation().validationPolicy().policyId());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> validationIssueFields(
            WayangPlatformReadinessProfileRegistryResolution resolution,
            WayangPlatformReadinessProfileValidationIssue issue) {
        Map<String, Object> values = new LinkedHashMap<>(issueFields(resolution));
        values.put("kind", issue.kind());
        values.put("profileId", issue.profileId());
        values.put("readinessId", issue.readinessId());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> attributes(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        Map<String, Object> values = new LinkedHashMap<>(issueFields(resolution));
        values.put("valid", resolution.valid());
        values.put("totalProfiles", resolution.totalProfiles());
        values.put("profileIds", resolution.profiles().stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("sources", resolution.sources().stream()
                .map(WayangPlatformReadinessProfileRegistryReadiness::sourceAttributes)
                .toList());
        return WayangReportMaps.copyMap(values);
    }

    private static Map<String, Object> sourceAttributes(
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
}
