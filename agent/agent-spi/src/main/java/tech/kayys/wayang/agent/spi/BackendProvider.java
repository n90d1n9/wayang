package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Service provider interface for backend discovery.
 *
 * <p>
 * Implementations of this interface are discovered via Java's
 * {@link ServiceLoader}
 * mechanism and register backend adapters with the {@link BackendRegistry}.
 * </p>
 *
 * <h3>Implementation Example:</h3>
 * 
 * <pre>{@code
 * public class GollekBackendProvider implements BackendProvider {
 *     @Override
 *     public String name() {
 *         return "gollek";
 *     }
 *
 *     @Override
 *     public InferenceBackend createInferenceBackend(Map<String, Object> config) {
 *         return new GollekBackendAdapter(GollekSdk.builder().build());
 *     }
 *
 *     @Override
 *     public WorkflowBackend createWorkflowBackend(Map<String, Object> config) {
 *         return new GamelanBackendAdapter(GamelanClient.create(config));
 *     }
 * }
 * }</pre>
 *
 * <h3>Registration:</h3>
 * Create {@code META-INF/services/tech.kayys.wayang.agent.spi.BackendProvider}
 * with:
 * 
 * <pre>
 * tech.kayys.GollekBackendProvider
 * </pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public interface BackendProvider {

    /**
     * Unique provider name (e.g., "gollek", "ollama", "temporal").
     *
     * @return provider name
     */
    String name();

    /**
     * Provider priority (higher = checked first).
     *
     * @return priority value
     */
    default int priority() {
        return 0;
    }

    /**
     * Create an inference backend instance.
     *
     * @param config backend-specific configuration
     * @return inference backend instance
     */
    InferenceBackend createInferenceBackend(java.util.Map<String, Object> config);

    /**
     * Create a workflow backend instance.
     *
     * @param config backend-specific configuration
     * @return workflow backend instance
     */
    default WorkflowBackend createWorkflowBackend(java.util.Map<String, Object> config) {
        return null; // Optional
    }

    /**
     * Check if this provider is available given the current environment.
     *
     * @return true if provider can be used
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Get supported backend names.
     *
     * @return list of backend names this provider can create
     */
    List<String> supportedBackends();
}
