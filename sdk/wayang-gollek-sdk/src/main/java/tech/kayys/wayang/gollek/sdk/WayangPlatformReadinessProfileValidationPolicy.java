package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record WayangPlatformReadinessProfileValidationPolicy(
        String policyId,
        List<String> knownReadinessIds,
        boolean requireDefaultProfile,
        boolean requireProductionProfile,
        boolean requireFullReadinessCoverage) {

    public static final String STRICT_POLICY_ID = "strict";
    public static final String RELAXED_POLICY_ID = "relaxed";
    public static final String CUSTOM_POLICY_ID = "custom";

    public WayangPlatformReadinessProfileValidationPolicy {
        policyId = SdkText.trimToDefault(policyId, CUSTOM_POLICY_ID);
        knownReadinessIds = normalizeKnownReadinessIds(knownReadinessIds);
    }

    public WayangPlatformReadinessProfileValidationPolicy(
            List<String> knownReadinessIds,
            boolean requireDefaultProfile,
            boolean requireProductionProfile,
            boolean requireFullReadinessCoverage) {
        this(
                CUSTOM_POLICY_ID,
                knownReadinessIds,
                requireDefaultProfile,
                requireProductionProfile,
                requireFullReadinessCoverage);
    }

    public static WayangPlatformReadinessProfileValidationPolicy defaultPolicy() {
        return strict(WayangPlatformReadinessComponents.defaultReadinessIds());
    }

    public static WayangPlatformReadinessProfileValidationPolicy strict(List<String> knownReadinessIds) {
        return new WayangPlatformReadinessProfileValidationPolicy(
                STRICT_POLICY_ID,
                knownReadinessIds,
                true,
                true,
                true);
    }

    public static WayangPlatformReadinessProfileValidationPolicy relaxed(List<String> knownReadinessIds) {
        return new WayangPlatformReadinessProfileValidationPolicy(
                RELAXED_POLICY_ID,
                knownReadinessIds,
                false,
                false,
                false);
    }

    public WayangPlatformReadinessProfileValidationPolicy withoutProfileRoleRequirements() {
        return new WayangPlatformReadinessProfileValidationPolicy(
                derivedPolicyId("without-profile-roles"),
                knownReadinessIds,
                false,
                false,
                requireFullReadinessCoverage);
    }

    public WayangPlatformReadinessProfileValidationPolicy withoutFullCoverageRequirement() {
        return new WayangPlatformReadinessProfileValidationPolicy(
                derivedPolicyId("without-full-coverage"),
                knownReadinessIds,
                requireDefaultProfile,
                requireProductionProfile,
                false);
    }

    public WayangPlatformReadinessProfileValidationPolicy withFullCoverageRequirement() {
        return new WayangPlatformReadinessProfileValidationPolicy(
                derivedPolicyId("with-full-coverage"),
                knownReadinessIds,
                requireDefaultProfile,
                requireProductionProfile,
                true);
    }

    public WayangPlatformReadinessProfileValidationPolicy withPolicyId(String policyId) {
        return new WayangPlatformReadinessProfileValidationPolicy(
                policyId,
                knownReadinessIds,
                requireDefaultProfile,
                requireProductionProfile,
                requireFullReadinessCoverage);
    }

    private String derivedPolicyId(String suffix) {
        return policyId + "-" + suffix;
    }

    private static List<String> normalizeKnownReadinessIds(List<String> readinessIds) {
        if (readinessIds == null || readinessIds.isEmpty()) {
            throw new IllegalArgumentException("At least one known readiness component id is required.");
        }
        Set<String> normalizedReadinessIds = new LinkedHashSet<>();
        for (String readinessId : readinessIds) {
            String normalized = SdkText.trimToEmpty(readinessId);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Known readiness component id is required.");
            }
            if (!normalizedReadinessIds.add(normalized)) {
                throw new IllegalArgumentException(
                        "Duplicate known readiness component id '" + normalized + "'.");
            }
        }
        return List.copyOf(normalizedReadinessIds);
    }
}
