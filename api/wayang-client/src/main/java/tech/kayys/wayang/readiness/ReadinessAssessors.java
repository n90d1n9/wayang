package tech.kayys.wayang.readiness;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.client.WayangReadinessReport;

/**
 * Factory and registry for component readiness assessors.
 * Provides lazy-loading support and centralized assessment orchestration.
 */
public final class ReadinessAssessors {

    private static final Map<String, ComponentReadinessAssessor> REGISTRY = new HashMap<>();
    private static volatile boolean initialized = false;

    private ReadinessAssessors() {
    }

    /**
     * Register an assessor instance with the given readiness ID.
     */
    public static void register(String readinessId, ComponentReadinessAssessor assessor) {
        if (readinessId == null || readinessId.isBlank()) {
            throw new IllegalArgumentException("readinessId cannot be null or blank");
        }
        if (assessor == null) {
            throw new IllegalArgumentException("assessor cannot be null");
        }
        REGISTRY.put(readinessId, assessor);
    }

    /**
     * Get a registered assessor by its readiness ID.
     * Returns null if not registered.
     */
    public static ComponentReadinessAssessor get(String readinessId) {
        ensureInitialized();
        return REGISTRY.get(readinessId);
    }

    /**
     * Assess readiness using the registered assessor for the given ID.
     * Returns null if no assessor is registered for the ID.
     */
    public static WayangReadinessReport assess(String readinessId, Object input) {
        ComponentReadinessAssessor assessor = get(readinessId);
        if (assessor == null) {
            return null;
        }
        return assessor.assess(input);
    }

    /**
     * Check if an assessor is registered for the given readiness ID.
     */
    public static boolean isRegistered(String readinessId) {
        ensureInitialized();
        return REGISTRY.containsKey(readinessId);
    }

    /**
     * Get the number of registered assessors.
     */
    public static int size() {
        ensureInitialized();
        return REGISTRY.size();
    }

    /**
     * Initialize the registry with default assessors (lazy-loaded).
     * This is called automatically on first access.
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (ReadinessAssessors.class) {
                if (!initialized) {
                    initializeDefaultAssessors();
                    initialized = true;
                }
            }
        }
    }

    /**
     * Register default built-in assessors.
     * Extend this with additional assessor registrations as needed.
     */
    private static void initializeDefaultAssessors() {
        // Default assessors will be registered here
        // This can be extended with @Autowired assessor instances in Spring context
    }

    /**
     * Clear all registered assessors (useful for testing).
     */
    protected static void clear() {
        synchronized (ReadinessAssessors.class) {
            REGISTRY.clear();
            initialized = false;
        }
    }
}
