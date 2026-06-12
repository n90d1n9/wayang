package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.Locale;
import java.util.Objects;

/**
 * Named object-storage service contribution for readiness profile reads.
 */
public record WayangReadinessProfileObjectStorageServiceRegistration(
        String serviceId,
        ObjectStorageService service) {

    public WayangReadinessProfileObjectStorageServiceRegistration {
        serviceId = normalize(serviceId);
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("Object-storage service id is required.");
        }
        service = Objects.requireNonNull(service, "service");
    }

    public static WayangReadinessProfileObjectStorageServiceRegistration of(
            String serviceId,
            ObjectStorageService service) {
        return new WayangReadinessProfileObjectStorageServiceRegistration(serviceId, service);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
