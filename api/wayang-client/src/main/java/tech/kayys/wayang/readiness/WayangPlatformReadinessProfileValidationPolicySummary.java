package tech.kayys.wayang.readiness;

import tech.kayys.wayang.client.SdkText;

public record WayangPlatformReadinessProfileValidationPolicySummary(
        String policyId,
        int knownReadinessCount,
        boolean requireDefaultProfile,
        boolean requireProductionProfile,
        boolean requireFullReadinessCoverage) {

    public WayangPlatformReadinessProfileValidationPolicySummary {
        policyId = SdkText.trimToDefault(
                policyId,
                WayangPlatformReadinessProfileValidationPolicy.CUSTOM_POLICY_ID);
        knownReadinessCount = Math.max(0, knownReadinessCount);
    }

    public static WayangPlatformReadinessProfileValidationPolicySummary from(
            WayangPlatformReadinessProfileValidationPolicy policy) {
        WayangPlatformReadinessProfileValidationPolicy model = policy == null
                ? WayangPlatformReadinessProfileValidationPolicy.defaultPolicy()
                : policy;
        return new WayangPlatformReadinessProfileValidationPolicySummary(
                model.policyId(),
                model.knownReadinessIds().size(),
                model.requireDefaultProfile(),
                model.requireProductionProfile(),
                model.requireFullReadinessCoverage());
    }

    public static WayangPlatformReadinessProfileValidationPolicySummary empty() {
        return new WayangPlatformReadinessProfileValidationPolicySummary(
                "none",
                0,
                false,
                false,
                false);
    }

    public boolean strict() {
        return requireDefaultProfile && requireProductionProfile && requireFullReadinessCoverage;
    }
}
