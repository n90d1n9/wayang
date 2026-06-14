package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WayangPlatformReadinessProfileValidationReport(
        int totalProfiles,
        List<String> profileIds,
        WayangPlatformReadinessProfileValidationPolicySummary validationPolicy,
        List<String> defaultProfileIds,
        List<String> productionProfileIds,
        List<String> knownReadinessIds,
        List<String> coveredReadinessIds,
        List<String> uncoveredReadinessIds,
        List<WayangPlatformReadinessProfileValidationIssue> issues) {

    public WayangPlatformReadinessProfileValidationReport {
        totalProfiles = Math.max(0, totalProfiles);
        profileIds = SdkLists.copy(profileIds);
        validationPolicy = validationPolicy == null
                ? WayangPlatformReadinessProfileValidationPolicySummary.empty()
                : validationPolicy;
        defaultProfileIds = SdkLists.copy(defaultProfileIds);
        productionProfileIds = SdkLists.copy(productionProfileIds);
        knownReadinessIds = SdkLists.copy(knownReadinessIds);
        coveredReadinessIds = SdkLists.copy(coveredReadinessIds);
        uncoveredReadinessIds = SdkLists.copy(uncoveredReadinessIds);
        issues = SdkLists.copy(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int defaultProfileCount() {
        return defaultProfileIds.size();
    }

    public int productionProfileCount() {
        return productionProfileIds.size();
    }

    public int coveredReadinessCount() {
        return coveredReadinessIds.size();
    }

    public int uncoveredReadinessCount() {
        return uncoveredReadinessIds.size();
    }
}
