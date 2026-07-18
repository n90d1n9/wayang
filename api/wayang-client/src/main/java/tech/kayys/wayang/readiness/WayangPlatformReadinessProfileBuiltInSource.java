package tech.kayys.wayang.readiness;

public final class WayangPlatformReadinessProfileBuiltInSource
        implements WayangPlatformReadinessProfileSource {

    public static final String SOURCE_ID = "builtin";
    public static final String SOURCE_TYPE = "builtin";

    private static final WayangPlatformReadinessProfileBuiltInSource INSTANCE =
            new WayangPlatformReadinessProfileBuiltInSource();

    private WayangPlatformReadinessProfileBuiltInSource() {
    }

    public static WayangPlatformReadinessProfileBuiltInSource create() {
        return INSTANCE;
    }

    @Override
    public WayangPlatformReadinessProfileSourceResult load() {
        return WayangPlatformReadinessProfileSourceResult.available(
                SOURCE_ID,
                SOURCE_TYPE,
                "wayang-gollek-sdk",
                WayangPlatformReadinessProfileCatalog.defaultProfiles(),
                "Built-in readiness profile catalog loaded.");
    }
}
