package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service-provider interface for initialized readiness profile object-storage services.
 *
 * <p>Deployments can register provider, credential, or default services through
 * {@link java.util.ServiceLoader}. The neutral Gollek storage module adapts
 * those services into SDK readiness profile readers.</p>
 */
public interface WayangReadinessProfileObjectStorageServiceProvider {

    default String providerId() {
        return getClass().getName();
    }

    default int priority() {
        return 100;
    }

    List<WayangReadinessProfileObjectStorageServiceRegistration> services(
            WayangGollekSdkConfig config);

    default Map<String, Object> diagnostics(WayangGollekSdkConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("available", false);
        values.put("serviceIds", List.of());
        values.put("message", "Object-storage service provider does not expose detailed diagnostics.");
        return java.util.Collections.unmodifiableMap(values);
    }
}
