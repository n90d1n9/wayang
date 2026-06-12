package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public final class WayangPlatformReadinessProfileCatalog {

    private WayangPlatformReadinessProfileCatalog() {
    }

    public static List<WayangPlatformReadinessProfileDescriptor> defaultProfiles() {
        return WayangPlatformReadinessProfiles.profileIds().stream()
                .map(WayangPlatformReadinessProfileCatalog::profile)
                .toList();
    }

    public static WayangPlatformReadinessProfileDescriptor profile(String profileId) {
        WayangPlatformReadinessProfile profile = WayangPlatformReadinessProfiles.profile(profileId);
        return new WayangPlatformReadinessProfileDescriptor(
                profile.profileId(),
                description(profile.profileId()),
                WayangPlatformReadinessProfiles.DEFAULT.equals(profile.profileId()),
                WayangPlatformReadinessProfiles.PRODUCTION.equals(profile.profileId()),
                profile.readinessIds());
    }

    private static String description(String profileId) {
        return switch (profileId) {
            case WayangPlatformReadinessProfiles.PRODUCTION ->
                    "Full production readiness profile for release and operations gates.";
            case WayangPlatformReadinessProfiles.MINIMAL ->
                    "Smallest startup profile for storage and contract-integrity checks.";
            case WayangPlatformReadinessProfiles.CONTRACTS ->
                    "Contract-focused profile for schema, command coverage, and standard alignment checks.";
            case WayangPlatformReadinessProfiles.CATALOGS ->
                    "Catalog-focused profile for skills, provider capabilities, and standard alignment checks.";
            default -> "Default platform readiness profile for local and shared SDK checks.";
        };
    }
}
