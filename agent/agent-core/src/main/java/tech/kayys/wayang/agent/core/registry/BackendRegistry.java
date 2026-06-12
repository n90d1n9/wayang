package tech.kayys.wayang.agent.core.registry;

import tech.kayys.wayang.agent.spi.BackendProvider;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for backend instances discovered via {@link BackendProvider}.
 *
 * <p>
 * This class manages backend lifecycle and provides lookup mechanisms.
 * It can be used programmatically without any framework dependencies.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Initialize registry (typically at startup)
 * BackendRegistry.initialize();
 *
 * // Get default inference backend
 * InferenceBackend inference = BackendRegistry.getDefaultInferenceBackend();
 *
 * // Get specific backend by name
 * WorkflowBackend workflow = BackendRegistry.getWorkflowBackend("gamelan");
 *
 * // Shutdown (typically at application stop)
 * BackendRegistry.shutdown();
 * }</pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public final class BackendRegistry {

    private static final Map<String, InferenceBackend> INFERENCE_BACKENDS = new ConcurrentHashMap<>();
    private static final Map<String, WorkflowBackend> WORKFLOW_BACKENDS = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private BackendRegistry() {
        // Utility class
    }

    // ── Initialization ───────────────────────────────────────────────────

    /**
     * Initialize the registry by discovering backends via ServiceLoader.
     * Should be called once at application startup.
     */
    public static synchronized void initialize() {
        initialize(Map.of());
    }

    /**
     * Initialize the registry with configuration.
     *
     * @param config global backend configuration
     */
    public static synchronized void initialize(Map<String, Object> config) {
        if (initialized) {
            return;
        }

        // Discover providers via ServiceLoader
        ServiceLoader<BackendProvider> loader = ServiceLoader.load(BackendProvider.class);

        List<BackendProvider> providers = loader.stream()
                .map(ServiceLoader.Provider::get)
                .filter(BackendProvider::isAvailable)
                .sorted(Comparator.comparingInt(BackendProvider::priority).reversed())
                .collect(Collectors.toList());

        // Register backends from each provider
        for (BackendProvider provider : providers) {
            try {
                Map<String, Object> providerConfig = extractProviderConfig(config, provider.name());

                if (provider.supportedBackends().contains("inference")) {
                    InferenceBackend inference = provider.createInferenceBackend(providerConfig);
                    INFERENCE_BACKENDS.put(provider.name(), inference);
                    inference.initialize(providerConfig);
                }

                if (provider.supportedBackends().contains("workflow")) {
                    WorkflowBackend workflow = provider.createWorkflowBackend(providerConfig);
                    if (workflow != null) {
                        WORKFLOW_BACKENDS.put(provider.name(), workflow);
                        workflow.initialize(providerConfig);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to initialize backend provider: " + provider.name());
                e.printStackTrace();
            }
        }

        initialized = true;
    }

    // ── Inference Backend Lookup ─────────────────────────────────────────

    /**
     * Get the default inference backend (first registered).
     *
     * @return default inference backend
     * @throws IllegalStateException if no backends registered
     */
    public static InferenceBackend getDefaultInferenceBackend() {
        ensureInitialized();

        if (INFERENCE_BACKENDS.isEmpty()) {
            throw new IllegalStateException(
                "No inference backends registered. " +
                "Ensure a BackendProvider implementation is on the classpath " +
                "and registered in META-INF/services/tech.kayys.wayang.agent.spi.BackendProvider"
            );
        }

        return INFERENCE_BACKENDS.values().iterator().next();
    }

    /**
     * Get inference backend by name.
     *
     * @param name backend name
     * @return inference backend
     * @throws NoSuchElementException if backend not found
     */
    public static InferenceBackend getInferenceBackend(String name) {
        ensureInitialized();

        InferenceBackend backend = INFERENCE_BACKENDS.get(name);
        if (backend == null) {
            throw new NoSuchElementException(
                "Inference backend not found: " + name +
                ". Available: " + INFERENCE_BACKENDS.keySet()
            );
        }
        return backend;
    }

    /**
     * List all registered inference backends.
     *
     * @return map of backend name to instance
     */
    public static Map<String, InferenceBackend> listInferenceBackends() {
        return Map.copyOf(INFERENCE_BACKENDS);
    }

    // ── Workflow Backend Lookup ──────────────────────────────────────────

    /**
     * Get the default workflow backend (first registered).
     *
     * @return default workflow backend
     * @throws IllegalStateException if no backends registered
     */
    public static WorkflowBackend getDefaultWorkflowBackend() {
        ensureInitialized();

        if (WORKFLOW_BACKENDS.isEmpty()) {
            throw new IllegalStateException(
                "No workflow backends registered. " +
                "Ensure a BackendProvider implementation is on the classpath."
            );
        }

        return WORKFLOW_BACKENDS.values().iterator().next();
    }

    /**
     * Get workflow backend by name.
     *
     * @param name backend name
     * @return workflow backend
     * @throws NoSuchElementException if backend not found
     */
    public static WorkflowBackend getWorkflowBackend(String name) {
        ensureInitialized();

        WorkflowBackend backend = WORKFLOW_BACKENDS.get(name);
        if (backend == null) {
            throw new NoSuchElementException(
                "Workflow backend not found: " + name +
                ". Available: " + WORKFLOW_BACKENDS.keySet()
            );
        }
        return backend;
    }

    /**
     * List all registered workflow backends.
     *
     * @return map of backend name to instance
     */
    public static Map<String, WorkflowBackend> listWorkflowBackends() {
        return Map.copyOf(WORKFLOW_BACKENDS);
    }

    // ── Manual Registration ──────────────────────────────────────────────

    /**
     * Manually register an inference backend.
     *
     * @param name backend name
     * @param backend backend instance
     */
    public static void registerInferenceBackend(String name, InferenceBackend backend) {
        INFERENCE_BACKENDS.put(name, backend);
    }

    /**
     * Manually register a workflow backend.
     *
     * @param name backend name
     * @param backend backend instance
     */
    public static void registerWorkflowBackend(String name, WorkflowBackend backend) {
        WORKFLOW_BACKENDS.put(name, backend);
    }

    // ── Health Checks ────────────────────────────────────────────────────

    /**
     * Check health of all registered backends.
     *
     * @return map of backend name to health status
     */
    public static Map<String, Boolean> checkHealth() {
        Map<String, Boolean> health = new LinkedHashMap<>();

        INFERENCE_BACKENDS.forEach((name, backend) ->
            health.put("inference:" + name, backend.isHealthy())
        );

        WORKFLOW_BACKENDS.forEach((name, backend) ->
            health.put("workflow:" + name, backend.isHealthy())
        );

        return health;
    }

    // ── Shutdown ─────────────────────────────────────────────────────────

    /**
     * Shutdown all registered backends gracefully.
     * Should be called at application shutdown.
     */
    public static synchronized void shutdown() {
        INFERENCE_BACKENDS.values().forEach(backend -> {
            try {
                backend.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down inference backend: " + backend.name());
                e.printStackTrace();
            }
        });

        WORKFLOW_BACKENDS.values().forEach(backend -> {
            try {
                backend.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down workflow backend: " + backend.name());
                e.printStackTrace();
            }
        });

        INFERENCE_BACKENDS.clear();
        WORKFLOW_BACKENDS.clear();
        initialized = false;
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                "BackendRegistry not initialized. Call BackendRegistry.initialize() first."
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractProviderConfig(
            Map<String, Object> config,
            String providerName) {

        Object providerConfig = config.get("backends." + providerName);
        if (providerConfig instanceof Map) {
            return (Map<String, Object>) providerConfig;
        }
        return Map.of();
    }
}
