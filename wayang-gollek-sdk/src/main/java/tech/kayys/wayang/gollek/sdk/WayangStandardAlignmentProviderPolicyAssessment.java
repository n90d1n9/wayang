package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of applying provider readiness policy to standard-alignment provider diagnostics.
 */
public record WayangStandardAlignmentProviderPolicyAssessment(
        boolean ready,
        WayangStandardAlignmentProviderIssueMode issueMode,
        int minimumProviderCount,
        int providerCount,
        List<String> requiredProviderIds,
        List<String> presentProviderIds,
        List<String> missingProviderIds,
        int issueCount,
        List<String> recommendations) {

    public WayangStandardAlignmentProviderPolicyAssessment {
        issueMode = issueMode == null ? WayangStandardAlignmentProviderIssueMode.WARN : issueMode;
        minimumProviderCount = Math.max(0, minimumProviderCount);
        providerCount = Math.max(0, providerCount);
        requiredProviderIds = SdkLists.copy(requiredProviderIds);
        presentProviderIds = SdkLists.copy(presentProviderIds);
        missingProviderIds = SdkLists.copy(missingProviderIds);
        issueCount = Math.max(0, issueCount);
        recommendations = SdkLists.copy(recommendations);
    }

    public boolean blocked() {
        return !missingProviderIds.isEmpty()
                || providerCount < minimumProviderCount
                || issueMode == WayangStandardAlignmentProviderIssueMode.BLOCK && issueCount > 0;
    }

    public boolean warning() {
        return !blocked()
                && issueMode == WayangStandardAlignmentProviderIssueMode.WARN
                && issueCount > 0;
    }

    public boolean hasProviderRequirements() {
        return minimumProviderCount > 0 || !requiredProviderIds.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready);
        values.put("issueMode", issueMode.id());
        values.put("minimumProviderCount", minimumProviderCount);
        values.put("providerCount", providerCount);
        values.put("requiredProviderIds", requiredProviderIds);
        values.put("presentProviderIds", presentProviderIds);
        values.put("missingProviderIds", missingProviderIds);
        values.put("issueCount", issueCount);
        values.put("recommendations", recommendations);
        return SdkMaps.orderedCopy(values);
    }
}
