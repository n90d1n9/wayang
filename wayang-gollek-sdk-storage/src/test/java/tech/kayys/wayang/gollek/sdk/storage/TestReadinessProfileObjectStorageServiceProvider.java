package tech.kayys.wayang.gollek.sdk.storage;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test-only object-storage service provider for ServiceLoader discovery coverage.
 */
public final class TestReadinessProfileObjectStorageServiceProvider
        implements WayangReadinessProfileObjectStorageServiceProvider {

    static final String OBJECT_KEY = "profiles/service-loader.properties";
    static final String PROVIDER_SERVICE_ID = "rustfs";

    @Override
    public String providerId() {
        return "test-object-storage-readiness-provider";
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public List<WayangReadinessProfileObjectStorageServiceRegistration> services(
            WayangGollekSdkConfig config) {
        WayangObjectStorageConfig objectStorage = config == null
                ? WayangObjectStorageConfig.none()
                : config.readinessProfileRegistry().objectStorage();
        if (!PROVIDER_SERVICE_ID.equals(objectStorage.provider())) {
            return List.of();
        }
        return List.of(WayangReadinessProfileObjectStorageServiceRegistration.of(
                PROVIDER_SERVICE_ID,
                new InMemoryObjectStorageService()
                        .withObject(objectStorage.keyPrefix(), readinessProfileDocument())));
    }

    @Override
    public Map<String, Object> diagnostics(WayangGollekSdkConfig config) {
        boolean available = !services(config).isEmpty();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("available", available);
        values.put("serviceId", available ? PROVIDER_SERVICE_ID : "");
        values.put("serviceIds", available ? List.of(PROVIDER_SERVICE_ID) : List.of());
        values.put("message", available
                ? "Test object-storage service provider is available."
                : "Test object-storage service provider does not match this config.");
        return java.util.Collections.unmodifiableMap(values);
    }

    private static String readinessProfileDocument() {
        return """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=storage-discovered-default,storage-discovered-production
                defaultProfileId=storage-discovered-default
                productionProfileId=storage-discovered-production
                profile.storage-discovered-default.description=Discovered object-storage default profile.
                profile.storage-discovered-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.storage-discovered-production.description=Discovered object-storage production profile.
                profile.storage-discovered-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """;
    }

    private static final class InMemoryObjectStorageService implements ObjectStorageService {

        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        InMemoryObjectStorageService withObject(String key, String value) {
            objects.put(key, value.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        @Override
        public Uni<Optional<byte[]>> getObject(String key) {
            return Uni.createFrom().item(Optional.ofNullable(objects.get(key)));
        }

        @Override
        public Uni<List<String>> listObjects(String prefix) {
            String normalizedPrefix = prefix == null ? "" : prefix;
            return Uni.createFrom().item(objects.keySet().stream()
                    .filter(key -> key.startsWith(normalizedPrefix))
                    .toList());
        }

        @Override
        public Uni<Void> putObject(String key, byte[] data) {
            objects.put(key, data);
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Boolean> deleteObject(String key) {
            return Uni.createFrom().item(objects.remove(key) != null);
        }
    }
}
