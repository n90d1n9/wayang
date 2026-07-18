package tech.kayys.wayang.readiness;

import java.util.List;

import tech.kayys.wayang.client.SdkText;

public final class WayangPlatformReadinessProfileValidationPolicies {

    public static final String STRICT = WayangPlatformReadinessProfileValidationPolicy.STRICT_POLICY_ID;
    public static final String RELAXED = WayangPlatformReadinessProfileValidationPolicy.RELAXED_POLICY_ID;
    public static final String STRICT_WITHOUT_PROFILE_ROLES = "strict-without-profile-roles";
    public static final String STRICT_WITHOUT_FULL_COVERAGE = "strict-without-full-coverage";
    public static final String RELAXED_WITH_FULL_COVERAGE = "relaxed-with-full-coverage";

    private static final List<String> POLICY_IDS = List.of(
            STRICT,
            RELAXED,
            STRICT_WITHOUT_PROFILE_ROLES,
            STRICT_WITHOUT_FULL_COVERAGE,
            RELAXED_WITH_FULL_COVERAGE);

    private WayangPlatformReadinessProfileValidationPolicies() {
    }

    public static List<String> policyIds() {
        return POLICY_IDS;
    }

    public static List<WayangPlatformReadinessProfileValidationPolicyDescriptor> descriptors() {
        return POLICY_IDS.stream()
                .map(WayangPlatformReadinessProfileValidationPolicies::descriptor)
                .toList();
    }

    public static WayangPlatformReadinessProfileValidationPolicyDescriptor descriptor(String policyId) {
        WayangPlatformReadinessProfileValidationPolicy policy = policy(policyId);
        return WayangPlatformReadinessProfileValidationPolicyDescriptor.from(
                policy,
                description(policy.policyId()));
    }

    public static WayangPlatformReadinessProfileValidationPolicy defaultPolicy() {
        return policy(STRICT);
    }

    public static WayangPlatformReadinessProfileValidationPolicy policy(String policyId) {
        String normalized = normalizePolicyId(policyId);
        List<String> readinessIds = WayangPlatformReadinessComponents.defaultReadinessIds();
        return switch (normalized) {
            case STRICT -> WayangPlatformReadinessProfileValidationPolicy.strict(readinessIds);
            case RELAXED -> WayangPlatformReadinessProfileValidationPolicy.relaxed(readinessIds);
            case STRICT_WITHOUT_PROFILE_ROLES ->
                    WayangPlatformReadinessProfileValidationPolicy.strict(readinessIds)
                            .withoutProfileRoleRequirements()
                            .withPolicyId(STRICT_WITHOUT_PROFILE_ROLES);
            case STRICT_WITHOUT_FULL_COVERAGE ->
                    WayangPlatformReadinessProfileValidationPolicy.strict(readinessIds)
                            .withoutFullCoverageRequirement()
                            .withPolicyId(STRICT_WITHOUT_FULL_COVERAGE);
            case RELAXED_WITH_FULL_COVERAGE ->
                    WayangPlatformReadinessProfileValidationPolicy.relaxed(readinessIds)
                            .withFullCoverageRequirement()
                            .withPolicyId(RELAXED_WITH_FULL_COVERAGE);
            default -> throw new IllegalArgumentException(
                    "Unknown platform readiness profile validation policy '" + normalized
                            + "'. Available policies: " + String.join(", ", POLICY_IDS) + ".");
        };
    }

    private static String normalizePolicyId(String policyId) {
        return SdkText.trimToDefault(policyId, STRICT);
    }

    private static String description(String policyId) {
        return switch (policyId) {
            case STRICT ->
                    "Strict production profile validation requiring one default profile, one production profile, and full readiness coverage.";
            case RELAXED ->
                    "Relaxed validation that checks known readiness bindings without requiring default, production, or full coverage rules.";
            case STRICT_WITHOUT_PROFILE_ROLES ->
                    "Strict coverage validation without requiring default or production profile role markers.";
            case STRICT_WITHOUT_FULL_COVERAGE ->
                    "Strict profile-role validation without requiring every known readiness component to be covered.";
            case RELAXED_WITH_FULL_COVERAGE ->
                    "Relaxed profile-role validation with full readiness component coverage required.";
            default -> "Custom platform readiness profile validation policy.";
        };
    }
}
