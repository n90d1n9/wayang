package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProvider;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaders;

import java.util.List;
import java.util.Map;

/**
 * Exposes discovered object-storage services as SDK readiness profile readers.
 */
public final class WayangReadinessProfileObjectStorageExternalReaderProvider
        implements WayangPlatformReadinessProfileExternalReaderProvider {

    @Override
    public String providerId() {
        return "wayang-readiness-profile-object-storage";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public WayangPlatformReadinessProfileExternalReaders readers(WayangGollekSdkConfig config) {
        WayangReadinessProfileObjectStorageServiceRegistry registry =
                WayangReadinessProfileObjectStorageServiceProviders.registry(config);
        if (registry.size() == 0) {
            return WayangPlatformReadinessProfileExternalReaders.none();
        }
        return WayangPlatformReadinessProfileExternalReaders.objectStorage(
                WayangReadinessProfileObjectStorageServiceReader.resolving(registry));
    }

    @Override
    public WayangPlatformReadinessProfileExternalReaderProviderDiagnostics diagnostics(
            WayangGollekSdkConfig config) {
        WayangReadinessProfileObjectStorageServiceDiscoveryReport report =
                WayangReadinessProfileObjectStorageServiceProviders.discoveryReport(config);
        return new WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
                providerId(),
                getClass().getName(),
                priority(),
                report.available(),
                report.available() ? List.of("object_storage") : List.of(),
                report.message(),
                Map.of("objectStorage", report.toMap()));
    }
}
