package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistry;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistryResolution;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileSourceStatus;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileValidationIssue;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.client.WayangReportMaps;
import tech.kayys.wayang.client.WayangReadinessReports;
import tech.kayys.wayang.client.SdkText;

/**
 * Readiness assessor for platform readiness profile registry.
 * Evaluates whether readiness profiles are available and valid from the registry.
 */
public class PlatformRegistryAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.platform.readiness-profile-registry.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return READINESS_ID;
    }

    @Override
    protected String buildProbeName() {
        return "registry";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangPlatformReadinessProfileRegistryResolution resolution =
                (WayangPlatformReadinessProfileRegistryResolution) input;
        WayangPlatformReadinessProfileRegistryResolution model = resolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : resolution;

        if (model.valid() && model.totalProfiles() > 0) {
            return List.of();
        }

        return issues(model);
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangPlatformReadinessProfileRegistryResolution resolution =
                (WayangPlatformReadinessProfileRegistryResolution) input;
        WayangPlatformReadinessProfileRegistryResolution model = resolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : resolution;

        return attributes(model);
    }

    @Override
    protected List<Map<String, Object>> buildProbes(Object input, List<Map<String, Object>> issues) {
        WayangPlatformReadinessProfileRegistryResolution resolution =
                (WayangPlatformReadinessProfileRegistryResolution) input;
        WayangPlatformReadinessProfileRegistryResolution model = resolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : resolution;

        return probes(model);
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
                .map(PlatformRegistryAssessor::sourceIssue)
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
                .map(PlatformRegistryAssessor::sourceAttributes)
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
