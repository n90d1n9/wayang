package tech.kayys.wayang.gollek.sdk;

public record WayangPlatformReadinessProfileValidationPolicyDescriptor(
        String policyId,
        String description,
        boolean defaultPolicy,
        boolean strict,
        int knownReadinessCount,
        boolean requireDefaultProfile,
        boolean requireProductionProfile,
        boolean requireFullReadinessCoverage) {

    public WayangPlatformReadinessProfileValidationPolicyDescriptor {
        policyId = SdkText.trimToDefault(
                policyId,
                WayangPlatformReadinessProfileValidationPolicy.CUSTOM_POLICY_ID);
        description = SdkText.trimToEmpty(description);
        knownReadinessCount = Math.max(0, knownReadinessCount);
    }

    public static WayangPlatformReadinessProfileValidationPolicyDescriptor from(
            WayangPlatformReadinessProfileValidationPolicy policy,
            String description) {
        WayangPlatformReadinessProfileValidationPolicy model = policy == null
                ? WayangPlatformReadinessProfileValidationPolicy.defaultPolicy()
                : policy;
        return new WayangPlatformReadinessProfileValidationPolicyDescriptor(
                model.policyId(),
                description,
                WayangPlatformReadinessProfileValidationPolicies.STRICT.equals(model.policyId()),
                model.requireDefaultProfile()
                        && model.requireProductionProfile()
                        && model.requireFullReadinessCoverage(),
                model.knownReadinessIds().size(),
                model.requireDefaultProfile(),
                model.requireProductionProfile(),
                model.requireFullReadinessCoverage());
    }
}
