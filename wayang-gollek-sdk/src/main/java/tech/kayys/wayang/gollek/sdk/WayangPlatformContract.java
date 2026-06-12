package tech.kayys.wayang.gollek.sdk;

public record WayangPlatformContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.platform.catalog";
    public static final int VERSION = 1;
    public static final String PLATFORM_STATUS = "platform-status";
    public static final String PRODUCT_CATALOG = "product-catalog";
    public static final String PROFILE_LIST = "profile-list";
    public static final String PROFILE_DETAIL = "profile-detail";
    public static final String SDK_BOUNDARY_CATALOG = "sdk-boundary-catalog";
    public static final String SDK_BOUNDARY_DETAIL = "sdk-boundary-detail";
    public static final String READINESS_PROFILE_LIST = "readiness-profile-list";
    public static final String READINESS_PROFILE_DETAIL = "readiness-profile-detail";
    public static final String READINESS_PROFILE_VALIDATION = "readiness-profile-validation";
    public static final String READINESS_PROFILE_VALIDATION_POLICY_LIST =
            "readiness-profile-validation-policy-list";
    public static final String READINESS_PROFILE_REGISTRY_RESOLUTION =
            "readiness-profile-registry-resolution";
    public static final String READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS =
            "readiness-profile-registry-config-diagnostics";

    public WayangPlatformContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangPlatformContract platformStatus() {
        return new WayangPlatformContract(SCHEMA, VERSION, PLATFORM_STATUS);
    }

    public static WayangPlatformContract productCatalog() {
        return new WayangPlatformContract(SCHEMA, VERSION, PRODUCT_CATALOG);
    }

    public static WayangPlatformContract profileList() {
        return new WayangPlatformContract(SCHEMA, VERSION, PROFILE_LIST);
    }

    public static WayangPlatformContract profileDetail() {
        return new WayangPlatformContract(SCHEMA, VERSION, PROFILE_DETAIL);
    }

    public static WayangPlatformContract sdkBoundaryCatalog() {
        return new WayangPlatformContract(SCHEMA, VERSION, SDK_BOUNDARY_CATALOG);
    }

    public static WayangPlatformContract sdkBoundaryDetail() {
        return new WayangPlatformContract(SCHEMA, VERSION, SDK_BOUNDARY_DETAIL);
    }

    public static WayangPlatformContract readinessProfileList() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_LIST);
    }

    public static WayangPlatformContract readinessProfileDetail() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_DETAIL);
    }

    public static WayangPlatformContract readinessProfileValidation() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_VALIDATION);
    }

    public static WayangPlatformContract readinessProfileValidationPolicyList() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_VALIDATION_POLICY_LIST);
    }

    public static WayangPlatformContract readinessProfileRegistryResolution() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_REGISTRY_RESOLUTION);
    }

    public static WayangPlatformContract readinessProfileRegistryConfigDiagnostics() {
        return new WayangPlatformContract(SCHEMA, VERSION, READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS);
    }
}
