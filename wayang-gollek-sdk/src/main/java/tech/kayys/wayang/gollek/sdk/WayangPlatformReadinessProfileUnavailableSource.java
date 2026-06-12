package tech.kayys.wayang.gollek.sdk;

public final class WayangPlatformReadinessProfileUnavailableSource
        implements WayangPlatformReadinessProfileSource {

    private final String sourceId;
    private final String sourceType;
    private final String location;
    private final String message;

    private WayangPlatformReadinessProfileUnavailableSource(
            String sourceId,
            String sourceType,
            String location,
            String message) {
        this.sourceId = SdkText.trimToDefault(sourceId, "unavailable");
        this.sourceType = SdkText.trimToDefault(sourceType, "unavailable");
        this.location = SdkText.trimToEmpty(location);
        this.message = SdkText.trimToDefault(message, "Readiness profile source is unavailable.");
    }

    public static WayangPlatformReadinessProfileUnavailableSource of(
            String sourceId,
            String sourceType,
            String location,
            String message) {
        return new WayangPlatformReadinessProfileUnavailableSource(sourceId, sourceType, location, message);
    }

    @Override
    public WayangPlatformReadinessProfileSourceResult load() {
        return WayangPlatformReadinessProfileSourceResult.unavailable(
                sourceId,
                sourceType,
                location,
                message);
    }
}
