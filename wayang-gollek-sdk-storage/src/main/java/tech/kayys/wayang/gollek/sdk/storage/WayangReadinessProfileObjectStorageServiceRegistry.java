package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangObjectStorageConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable service registry for config-driven readiness profile object storage reads.
 */
public final class WayangReadinessProfileObjectStorageServiceRegistry
        implements WayangReadinessProfileObjectStorageServiceResolver {

    public static final String DEFAULT_SERVICE_ID = "default";

    private final Map<String, ObjectStorageService> services;

    private WayangReadinessProfileObjectStorageServiceRegistry(
            Map<String, ObjectStorageService> services) {
        this.services = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(services));
    }

    public static WayangReadinessProfileObjectStorageServiceRegistry empty() {
        return new WayangReadinessProfileObjectStorageServiceRegistry(Map.of());
    }

    public static WayangReadinessProfileObjectStorageServiceRegistry ofDefault(
            ObjectStorageService service) {
        return builder().register(DEFAULT_SERVICE_ID, service).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<ObjectStorageService> resolve(WayangObjectStorageConfig config) {
        WayangReadinessProfileObjectStorageServiceResolution resolution = resolveReport(config);
        return resolution.available() ? find(resolution.selectedServiceId()) : Optional.empty();
    }

    public WayangReadinessProfileObjectStorageServiceResolution resolveReport(
            WayangObjectStorageConfig config) {
        String credentialsRef = config == null ? "" : normalize(config.credentialsRef());
        String provider = config == null ? "" : normalize(config.provider());
        if (config != null) {
            Optional<ObjectStorageService> credentialService = find(config.credentialsRef());
            if (credentialService.isPresent()) {
                return available(credentialsRef, provider, credentialsRef, "credentialsRef");
            }
            Optional<ObjectStorageService> providerService = find(config.provider());
            if (providerService.isPresent()) {
                return available(credentialsRef, provider, provider, "provider");
            }
        }
        if (find(DEFAULT_SERVICE_ID).isPresent()) {
            return available(credentialsRef, provider, DEFAULT_SERVICE_ID, "default");
        }
        return new WayangReadinessProfileObjectStorageServiceResolution(
                credentialsRef,
                provider,
                "",
                "none",
                false,
                serviceIds(),
                "No readiness profile object-storage service matched the configured credentialsRef, provider, or default.");
    }

    public Optional<ObjectStorageService> find(String serviceId) {
        return Optional.ofNullable(services.get(normalize(serviceId)));
    }

    public List<String> serviceIds() {
        return List.copyOf(services.keySet());
    }

    public int size() {
        return services.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private WayangReadinessProfileObjectStorageServiceResolution available(
            String credentialsRef,
            String provider,
            String serviceId,
            String selectedBy) {
        return new WayangReadinessProfileObjectStorageServiceResolution(
                credentialsRef,
                provider,
                serviceId,
                selectedBy,
                true,
                serviceIds(),
                "Readiness profile object-storage service selected by " + selectedBy + ".");
    }

    /**
     * Builder for immutable readiness profile object-storage service registries.
     */
    public static final class Builder {

        private final Map<String, ObjectStorageService> services = new LinkedHashMap<>();

        public Builder register(String serviceId, ObjectStorageService service) {
            String id = normalize(serviceId);
            if (id.isBlank()) {
                throw new IllegalArgumentException("Object-storage service id is required.");
            }
            services.put(id, Objects.requireNonNull(service, "service"));
            return this;
        }

        public WayangReadinessProfileObjectStorageServiceRegistry build() {
            return new WayangReadinessProfileObjectStorageServiceRegistry(services);
        }
    }
}
