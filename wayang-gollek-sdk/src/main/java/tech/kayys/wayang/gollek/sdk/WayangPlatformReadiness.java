package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WayangPlatformReadiness {

    public static final String READINESS_ID = "wayang.platform.readiness";
    public static final String CONTRACT_INTEGRITY_READINESS_ID = WayangContractIntegrityReadiness.READINESS_ID;
    public static final String CONTRACT_COVERAGE_READINESS_ID = WayangContractCoverageReadiness.READINESS_ID;
    public static final String SKILL_CATALOG_READINESS_ID = WayangSkillCatalogReadiness.READINESS_ID;
    public static final String PROVIDER_CAPABILITY_READINESS_ID = WayangProviderCapabilityReadiness.READINESS_ID;
    public static final String STANDARD_ALIGNMENT_READINESS_ID = WayangStandardAlignmentReadiness.READINESS_ID;

    private WayangPlatformReadiness() {
    }

    public static WayangReadinessReport assess(WayangGollekSdk sdk) {
        return assess(sdk, WayangPlatformReadinessProfiles.defaultProfile());
    }

    public static WayangReadinessReport assess(WayangGollekSdk sdk, String profileId) {
        return assess(sdk, WayangPlatformReadinessProfiles.profile(profileId));
    }

    public static WayangReadinessReport assess(
            WayangGollekSdk sdk,
            WayangPlatformReadinessProfile profile) {
        WayangGollekSdk resolved = sdk == null ? WayangGollekSdk.local() : sdk;
        return WayangReadinessReports.aggregate(
                READINESS_ID,
                WayangPlatformReadinessComponents.assessProfile(profile, resolved),
                profileAttributes(profile));
    }

    public static WayangReadinessReport assess(
            WayangGollekSdk sdk,
            WayangPlatformReadinessProfileDescriptor profile) {
        WayangPlatformReadinessProfileDescriptor resolvedProfile = profile == null
                ? WayangPlatformReadinessProfileCatalog.profile(WayangPlatformReadinessProfiles.DEFAULT)
                : profile;
        WayangGollekSdk resolved = sdk == null ? WayangGollekSdk.local() : sdk;
        return WayangReadinessReports.aggregate(
                READINESS_ID,
                WayangPlatformReadinessComponents.assessProfile(
                        WayangPlatformReadinessProfile.of(
                                resolvedProfile.profileId(),
                                resolvedProfile.readinessIds()),
                        resolved),
                profileAttributes(resolvedProfile));
    }

    public static WayangReadinessReport contractIntegrity(WayangContractIntegrityReport integrity) {
        return WayangContractIntegrityReadiness.assess(integrity);
    }

    public static WayangReadinessReport contractCoverage(WayangContractCommandCoverageReport coverage) {
        return WayangContractCoverageReadiness.assess(coverage);
    }

    public static WayangReadinessReport skillCatalog(AgentSkillDiscovery discovery) {
        return WayangSkillCatalogReadiness.assess(discovery);
    }

    public static WayangReadinessReport standardAlignment(WayangStandardAlignmentHealthReport health) {
        return WayangStandardAlignmentReadiness.assess(health);
    }

    public static WayangReadinessReport providerCapability(WayangProviderCapabilityDiscovery discovery) {
        return WayangProviderCapabilityReadiness.assess(discovery);
    }

    private static Map<String, Object> profileAttributes(WayangPlatformReadinessProfile profile) {
        WayangPlatformReadinessProfile resolved = profile == null
                ? WayangPlatformReadinessProfiles.defaultProfile()
                : profile;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("readinessProfileId", resolved.profileId());
        values.put("readinessProfileDefault", WayangPlatformReadinessProfiles.DEFAULT.equals(resolved.profileId()));
        values.put("readinessProfileProduction",
                WayangPlatformReadinessProfiles.PRODUCTION.equals(resolved.profileId()));
        values.put("readinessProfileComponentIds", resolved.readinessIds());
        return values;
    }

    private static Map<String, Object> profileAttributes(WayangPlatformReadinessProfileDescriptor profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("readinessProfileId", profile.profileId());
        values.put("readinessProfileDefault", profile.defaultProfile());
        values.put("readinessProfileProduction", profile.productionProfile());
        values.put("readinessProfileComponentIds", profile.readinessIds());
        return values;
    }
}
