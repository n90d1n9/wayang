package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified provider visibility view for standard-alignment discovery and health payloads.
 */
public record WayangStandardAlignmentProviderDiagnostics(
        List<String> providerIds,
        List<WayangStandardAlignmentProviderSummary> providers,
        List<WayangStandardAlignmentProviderIssue> issues) {

    public static WayangStandardAlignmentProviderDiagnostics empty() {
        return new WayangStandardAlignmentProviderDiagnostics(List.of(), List.of(), List.of());
    }

    public WayangStandardAlignmentProviderDiagnostics {
        providers = WayangStandardAlignmentProviderSummaries.copy(providers);
        providerIds = WayangStandardAlignmentProviderSummaries.resolveProviderIds(providerIds, providers);
        issues = WayangStandardAlignmentProviderIssues.copy(issues);
    }

    public int providerCount() {
        return providerIds.size();
    }

    public int issueCount() {
        return issues.size();
    }

    public boolean hasProviders() {
        return providerCount() > 0;
    }

    public boolean hasIssues() {
        return issueCount() > 0;
    }

    public boolean healthy() {
        return !hasIssues();
    }

    public List<Map<String, Object>> providerMaps() {
        return WayangStandardAlignmentProviderSummaries.toMaps(providers);
    }

    public List<Map<String, Object>> issueMaps() {
        return WayangStandardAlignmentProviderIssues.toMaps(issues);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("healthy", healthy());
        values.put("providerCount", providerCount());
        values.put("providerIds", providerIds);
        values.put("providers", providerMaps());
        values.put("issueCount", issueCount());
        values.put("issues", issueMaps());
        return SdkMaps.orderedCopy(values);
    }
}
