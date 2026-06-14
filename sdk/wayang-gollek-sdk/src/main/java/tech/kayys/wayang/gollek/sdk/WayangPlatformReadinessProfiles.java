package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Locale;

public final class WayangPlatformReadinessProfiles {

    public static final String DEFAULT = WayangPlatformReadinessProfile.DEFAULT_PROFILE_ID;
    public static final String PRODUCTION = "production";
    public static final String MINIMAL = "minimal";
    public static final String CONTRACTS = "contracts";
    public static final String CATALOGS = "catalogs";

    private WayangPlatformReadinessProfiles() {
    }

    public static WayangPlatformReadinessProfile defaultProfile() {
        return profile(DEFAULT);
    }

    public static WayangPlatformReadinessProfile profile(String profileId) {
        String resolved = SdkText.trimToDefault(profileId, DEFAULT).toLowerCase(Locale.ROOT);
        return switch (resolved) {
            case DEFAULT, PRODUCTION -> WayangPlatformReadinessProfile.of(resolved, defaultReadinessIds());
            case MINIMAL -> WayangPlatformReadinessProfile.of(
                    MINIMAL,
                    List.of(
                            WayangStorageReadiness.READINESS_ID,
                            WayangContractIntegrityReadiness.READINESS_ID));
            case CONTRACTS -> WayangPlatformReadinessProfile.of(
                    CONTRACTS,
                    List.of(
                            WayangContractIntegrityReadiness.READINESS_ID,
                            WayangContractCoverageReadiness.READINESS_ID,
                            WayangStandardAlignmentReadiness.READINESS_ID));
            case CATALOGS -> WayangPlatformReadinessProfile.of(
                    CATALOGS,
                    List.of(
                            WayangSkillCatalogReadiness.READINESS_ID,
                            WayangProviderCapabilityReadiness.READINESS_ID,
                            WayangStandardAlignmentReadiness.READINESS_ID));
            default -> throw unknownProfile(resolved);
        };
    }

    public static List<String> profileIds() {
        return List.of(DEFAULT, PRODUCTION, MINIMAL, CONTRACTS, CATALOGS);
    }

    private static List<String> defaultReadinessIds() {
        return WayangPlatformReadinessComponents.defaultReadinessIds();
    }

    private static IllegalArgumentException unknownProfile(String profileId) {
        return new IllegalArgumentException(
                "Unknown platform readiness profile '" + profileId + "'. Available profiles: "
                        + String.join(", ", profileIds()) + ".");
    }
}
