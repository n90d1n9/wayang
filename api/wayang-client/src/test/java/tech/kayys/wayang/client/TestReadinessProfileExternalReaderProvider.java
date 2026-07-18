package tech.kayys.wayang.client;

import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileExternalReaderProvider;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileExternalReaders;

/**
 * Test-only readiness profile provider used to verify ServiceLoader discovery wiring.
 */
public final class TestReadinessProfileExternalReaderProvider
        implements WayangPlatformReadinessProfileExternalReaderProvider {

    static final String DATABASE_URL = "test-service-loader://readiness-profiles";

    @Override
    public String providerId() {
        return "test-readiness-profile-provider";
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public WayangPlatformReadinessProfileExternalReaders readers(WayangGollekSdkConfig config) {
        String databaseUrl = config == null
                ? ""
                : config.readinessProfileRegistry().databaseUrl();
        if (!DATABASE_URL.equals(databaseUrl)) {
            return WayangPlatformReadinessProfileExternalReaders.none();
        }
        return WayangPlatformReadinessProfileExternalReaders.database(
                ignored -> discoveredProfileDocument());
    }

    private static String discoveredProfileDocument() {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=discovered-default,discovered-production
                defaultProfileId=discovered-default
                productionProfileId=discovered-production
                profile.discovered-default.description=Discovered default profile.
                profile.discovered-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.discovered-production.description=Discovered production profile.
                profile.discovered-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """;
    }
}
