package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangGollekSdkConfig;


/**
 * Service-provider interface for optional readiness profile reader modules.
 *
 * <p>Provider modules can expose database, object-storage, or hybrid readers
 * through {@link java.util.ServiceLoader} without adding driver or cloud
 * dependencies to the SDK core.</p>
 */
public interface WayangPlatformReadinessProfileExternalReaderProvider {

    default String providerId() {
        return getClass().getName();
    }

    default int priority() {
        return 100;
    }

    WayangPlatformReadinessProfileExternalReaders readers(WayangGollekSdkConfig config);

    default WayangPlatformReadinessProfileExternalReaderProviderDiagnostics diagnostics(
            WayangGollekSdkConfig config) {
        WayangPlatformReadinessProfileExternalReaders resolvedReaders = readers(config);
        List<String> readerTypes = readerTypes(resolvedReaders);
        return new WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
                providerId(),
                getClass().getName(),
                priority(),
                !readerTypes.isEmpty(),
                readerTypes,
                message(readerTypes),
                Map.of());
    }

    private static List<String> readerTypes(WayangPlatformReadinessProfileExternalReaders readers) {
        WayangPlatformReadinessProfileExternalReaders resolved = readers == null
                ? WayangPlatformReadinessProfileExternalReaders.none()
                : readers;
        List<String> readerTypes = new ArrayList<>();
        if (resolved.objectReader() != null) {
            readerTypes.add("object_storage");
        }
        if (resolved.databaseReader() != null) {
            readerTypes.add("database");
        }
        return List.copyOf(readerTypes);
    }

    private static String message(List<String> readerTypes) {
        if (readerTypes == null || readerTypes.isEmpty()) {
            return "Readiness profile external reader provider has no available readers for this config.";
        }
        return "Readiness profile external reader provider is available.";
    }
}
