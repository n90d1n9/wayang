package tech.kayys.wayang.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry for all Wayang catalog implementations.
 *
 * <p>Provides centralized access to catalog instances and supports lazy
 * initialization, registration, and discovery of catalogs by domain ID.</p>
 */
public final class CatalogRegistry {

    private static final Map<String, AbstractCatalog<?, ?>> REGISTRY = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    private CatalogRegistry() {
    }

    /**
     * Registers a catalog instance with a unique domain ID.
     *
     * @param domainId unique identifier for the catalog's domain
     * @param catalog the catalog instance to register
     * @throws IllegalArgumentException if domainId is already registered
     */
    public static void register(String domainId, AbstractCatalog<?, ?> catalog) {
        if (domainId == null || domainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID cannot be null or empty");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }
        if (REGISTRY.containsKey(domainId)) {
            throw new IllegalArgumentException("Domain ID '" + domainId + "' is already registered");
        }
        REGISTRY.put(domainId, catalog);
    }

    /**
     * Registers a catalog, replacing any existing registration with the same domain ID.
     *
     * @param domainId unique identifier for the catalog's domain
     * @param catalog the catalog instance to register
     */
    public static void registerOrReplace(String domainId, AbstractCatalog<?, ?> catalog) {
        if (domainId == null || domainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID cannot be null or empty");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }
        REGISTRY.put(domainId, catalog);
    }

    /**
     * Retrieves a registered catalog by domain ID with type safety.
     *
     * @param domainId the domain ID to look up
     * @param catalogClass the expected catalog type
     * @return the catalog instance cast to the specified type
     * @throws IllegalArgumentException if domainId not found or type mismatch
     */
    @SuppressWarnings("unchecked")
    public static <C extends AbstractCatalog<?, ?>> C getCatalog(String domainId, Class<C> catalogClass) {
        if (domainId == null || domainId.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID cannot be null or empty");
        }
        ensureInitialized();
        AbstractCatalog<?, ?> catalog = REGISTRY.get(domainId);
        if (catalog == null) {
            throw new IllegalArgumentException("No catalog registered for domain: " + domainId);
        }
        if (!catalogClass.isInstance(catalog)) {
            throw new IllegalArgumentException(
                    "Catalog for domain '" + domainId + "' is not of type " + catalogClass.getSimpleName());
        }
        return (C) catalog;
    }

    /**
     * Retrieves a registered catalog by domain ID without type checking.
     *
     * @param domainId the domain ID to look up
     * @return the catalog instance, or null if not found
     */
    public static AbstractCatalog<?, ?> getCatalog(String domainId) {
        if (domainId == null || domainId.trim().isEmpty()) {
            return null;
        }
        ensureInitialized();
        return REGISTRY.get(domainId);
    }

    /**
     * Checks if a catalog is registered for the given domain ID.
     *
     * @param domainId the domain ID to check
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(String domainId) {
        if (domainId == null || domainId.trim().isEmpty()) {
            return false;
        }
        ensureInitialized();
        return REGISTRY.containsKey(domainId);
    }

    /**
     * Gets all registered catalogs as an unmodifiable map.
     *
     * @return map of domain ID to catalog instance
     */
    public static Map<String, AbstractCatalog<?, ?>> getAllCatalogs() {
        ensureInitialized();
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(REGISTRY));
    }

    /**
     * Gets the collection of all registered domain IDs.
     *
     * @return collection of domain IDs
     */
    public static Collection<String> getRegisteredDomainIds() {
        ensureInitialized();
        return Collections.unmodifiableCollection(REGISTRY.keySet());
    }

    /**
     * Unregisters a catalog by domain ID.
     *
     * @param domainId the domain ID to unregister
     * @return true if a catalog was unregistered, false if not found
     */
    public static boolean unregister(String domainId) {
        if (domainId == null || domainId.trim().isEmpty()) {
            return false;
        }
        return REGISTRY.remove(domainId) != null;
    }

    /**
     * Clears all registered catalogs. Use with caution - mainly for testing.
     */
    public static void clear() {
        REGISTRY.clear();
        initialized = false;
    }

    /**
     * Initializes the registry with default catalogs.
     * This is called automatically on first access but can be called explicitly.
     */
    public static void initialize() {
        ensureInitialized();
    }

    /**
     * Ensures the registry is initialized with default catalogs.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (INIT_LOCK) {
                if (!initialized) {
                    // Default initialization - catalogs are registered lazily or through
                    // CatalogBuilder as needed. Applications can call initialize() or
                    // register() their own catalogs.
                    initialized = true;
                }
            }
        }
    }

    /**
     * Gets the current count of registered catalogs.
     *
     * @return number of catalogs in the registry
     */
    public static int count() {
        ensureInitialized();
        return REGISTRY.size();
    }
}
