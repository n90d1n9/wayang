package tech.kayys.wayang.readiness;

/**
 * Optional external readers used by readiness profile registry sources.
 *
 * <p>The SDK core owns registry composition, while provider-specific modules
 * can supply database or object-storage readers without adding driver or cloud
 * dependencies to the core module.</p>
 */
public record WayangPlatformReadinessProfileExternalReaders(
        WayangPlatformReadinessProfileObjectReader objectReader,
        WayangPlatformReadinessProfileDatabaseReader databaseReader) {

    public static WayangPlatformReadinessProfileExternalReaders none() {
        return new WayangPlatformReadinessProfileExternalReaders(null, null);
    }

    public static WayangPlatformReadinessProfileExternalReaders objectStorage(
            WayangPlatformReadinessProfileObjectReader objectReader) {
        return new WayangPlatformReadinessProfileExternalReaders(objectReader, null);
    }

    public static WayangPlatformReadinessProfileExternalReaders database(
            WayangPlatformReadinessProfileDatabaseReader databaseReader) {
        return new WayangPlatformReadinessProfileExternalReaders(null, databaseReader);
    }

    public static WayangPlatformReadinessProfileExternalReaders of(
            WayangPlatformReadinessProfileObjectReader objectReader,
            WayangPlatformReadinessProfileDatabaseReader databaseReader) {
        return new WayangPlatformReadinessProfileExternalReaders(objectReader, databaseReader);
    }

    public static WayangPlatformReadinessProfileExternalReaders merge(
            WayangPlatformReadinessProfileExternalReaders primary,
            WayangPlatformReadinessProfileExternalReaders fallback) {
        return resolved(primary).withFallbacks(fallback);
    }

    public boolean empty() {
        return objectReader == null && databaseReader == null;
    }

    public WayangPlatformReadinessProfileExternalReaders withFallbacks(
            WayangPlatformReadinessProfileExternalReaders fallback) {
        WayangPlatformReadinessProfileExternalReaders resolvedFallback = resolved(fallback);
        return new WayangPlatformReadinessProfileExternalReaders(
                objectReader == null ? resolvedFallback.objectReader() : objectReader,
                databaseReader == null ? resolvedFallback.databaseReader() : databaseReader);
    }

    private static WayangPlatformReadinessProfileExternalReaders resolved(
            WayangPlatformReadinessProfileExternalReaders readers) {
        return readers == null ? none() : readers;
    }
}
