package tech.kayys.wayang.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing discovery service instances.
 * Provides centralized access to all discovery services
 * and allows discovery operations across registered services.
 */
public final class DiscoveryRegistry {

    private static final DiscoveryRegistry INSTANCE = new DiscoveryRegistry();

    private final Map<String, AbstractDiscoveryService<?, ?, ?>> services = new HashMap<>();

    private DiscoveryRegistry() {
    }

    /**
     * Get the singleton instance.
     */
    public static DiscoveryRegistry create() {
        return INSTANCE;
    }

    /**
     * Register a discovery service.
     *
     * @param discoveryId unique identifier for the service
     * @param service the discovery service to register
     */
    public void register(String discoveryId, AbstractDiscoveryService<?, ?, ?> service) {
        if (discoveryId == null || discoveryId.isBlank()) {
            throw new IllegalArgumentException("Discovery ID cannot be null or blank");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        services.put(discoveryId, service);
    }

    /**
     * Get a registered discovery service.
     *
     * @param discoveryId the service identifier
     * @param serviceClass the expected service class
     * @return the service cast to the provided class
     * @throws IllegalArgumentException if service not found or type mismatch
     */
    @SuppressWarnings("unchecked")
    public <S extends AbstractDiscoveryService<?, ?, ?>> S getService(String discoveryId, Class<S> serviceClass) {
        AbstractDiscoveryService<?, ?, ?> service = services.get(discoveryId);
        if (service == null) {
            throw new IllegalArgumentException("Discovery service not found: " + discoveryId);
        }
        if (!serviceClass.isInstance(service)) {
            throw new IllegalArgumentException(
                    "Service " + discoveryId + " is not of type " + serviceClass.getSimpleName());
        }
        return (S) service;
    }

    /**
     * Discover items from a registered service.
     *
     * @param discoveryId the service identifier
     * @return list of discovered items
     */
    public List<?> discover(String discoveryId) {
        AbstractDiscoveryService<?, ?, ?> service = services.get(discoveryId);
        if (service == null) {
            throw new IllegalArgumentException("Discovery service not found: " + discoveryId);
        }
        return service.discover();
    }

    /**
     * Clear cache for a specific service.
     */
    public void clearCache(String discoveryId) {
        AbstractDiscoveryService<?, ?, ?> service = services.get(discoveryId);
        if (service != null) {
            service.clearCache();
        }
    }

    /**
     * Clear cache for all services.
     */
    public void clearAllCaches() {
        services.values().forEach(AbstractDiscoveryService::clearCache);
    }

    /**
     * Check if a service is registered.
     */
    public boolean isRegistered(String discoveryId) {
        return services.containsKey(discoveryId);
    }

    /**
     * Get count of registered services.
     */
    public int size() {
        return services.size();
    }
}
