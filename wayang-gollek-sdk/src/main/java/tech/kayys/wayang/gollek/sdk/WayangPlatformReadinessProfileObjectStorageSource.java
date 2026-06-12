package tech.kayys.wayang.gollek.sdk;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Loads readiness profile catalogs from an object store through a pluggable reader.
 */
public final class WayangPlatformReadinessProfileObjectStorageSource
        implements WayangPlatformReadinessProfileSource {

    public static final String SOURCE_TYPE = "object_storage";

    private final WayangObjectStorageConfig config;
    private final WayangPlatformReadinessProfileObjectReader reader;

    private WayangPlatformReadinessProfileObjectStorageSource(
            WayangObjectStorageConfig config,
            WayangPlatformReadinessProfileObjectReader reader) {
        this.config = config == null ? WayangObjectStorageConfig.none() : config;
        this.reader = reader;
    }

    public static WayangPlatformReadinessProfileObjectStorageSource of(
            WayangObjectStorageConfig config,
            WayangPlatformReadinessProfileObjectReader reader) {
        return new WayangPlatformReadinessProfileObjectStorageSource(config, reader);
    }

    public static String locationOf(WayangObjectStorageConfig config) {
        WayangObjectStorageConfig resolved = config == null ? WayangObjectStorageConfig.none() : config;
        if (!resolved.configured()) {
            return "";
        }
        String bucket = resolved.bucket().isBlank() ? "unknown-bucket" : resolved.bucket();
        String prefix = resolved.keyPrefix().isBlank() ? "" : "/" + resolved.keyPrefix();
        return sourceIdOf(resolved) + "://" + bucket + prefix;
    }

    @Override
    public WayangPlatformReadinessProfileSourceResult load() {
        if (reader == null) {
            return unavailable("Object-storage readiness profile loader is not wired.");
        }
        try {
            String document = reader.read(config);
            Properties properties = new Properties();
            properties.load(new StringReader(SdkText.trimToEmpty(document)));
            return WayangPlatformReadinessProfileSourceResult.available(
                    sourceId(),
                    SOURCE_TYPE,
                    location(),
                    WayangPlatformReadinessProfileDocument.fromProperties(properties),
                    "Object-storage readiness profile loaded.");
        } catch (IOException | RuntimeException exception) {
            return unavailable("Object-storage readiness profile could not be loaded: "
                    + exception.getMessage());
        }
    }

    private WayangPlatformReadinessProfileSourceResult unavailable(String message) {
        return WayangPlatformReadinessProfileSourceResult.unavailable(
                sourceId(),
                SOURCE_TYPE,
                location(),
                message);
    }

    private String sourceId() {
        return sourceIdOf(config);
    }

    private String location() {
        return locationOf(config);
    }

    private static String sourceIdOf(WayangObjectStorageConfig config) {
        return SdkText.trimToDefault(config.provider(), "object-storage");
    }
}
