package tech.kayys.wayang.runtime.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.BackendRegistry;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

/**
 * Quarkus CDI producers for backend interfaces.
 *
 * <p>
 * These producers expose {@link InferenceBackend} and {@link WorkflowBackend}
 * as CDI beans, enabling injection anywhere in the application:
 * </p>
 *
 * <pre>{@code
 * @Inject InferenceBackend inferenceBackend;
 * @Inject WorkflowBackend workflowBackend;
 * }</pre>
 *
 * <h3>Backend Resolution:</h3>
 * <p>
 * Backends are resolved from {@link BackendRegistry}, which auto-discovers
 * backend providers via ServiceLoader. If the registry is not initialized,
 * these producers will initialize it with default configuration.
 * </p>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
@ApplicationScoped
public class BackendProducer {

    private static final Logger LOG = Logger.getLogger(BackendProducer.class);

    /**
     * Produce default inference backend.
     *
     * @return inference backend from BackendRegistry
     */
    @Produces
    @ApplicationScoped
    public InferenceBackend produceInferenceBackend() {
        LOG.debug("Producing InferenceBackend from BackendRegistry");

        ensureRegistryInitialized();

        InferenceBackend backend = BackendRegistry.getDefaultInferenceBackend();
        LOG.infof("Produced InferenceBackend: %s (healthy=%s)",
            backend.name(), backend.isHealthy());

        return backend;
    }

    /**
     * Produce default workflow backend.
     *
     * @return workflow backend from BackendRegistry (may be null if not available)
     */
    @Produces
    @ApplicationScoped
    public WorkflowBackend produceWorkflowBackend() {
        LOG.debug("Producing Workflow Backend from BackendRegistry");

        ensureRegistryInitialized();

        try {
            WorkflowBackend backend = BackendRegistry.getDefaultWorkflowBackend();
            LOG.infof("Produced WorkflowBackend: %s (healthy=%s)",
                backend.name(), backend.isHealthy());
            return backend;
        } catch (IllegalStateException e) {
            LOG.debug("No workflow backend available from BackendRegistry");
            return null;
        }
    }

    /**
     * Ensure BackendRegistry is initialized.
     * This is idempotent — safe to call multiple times.
     */
    private void ensureRegistryInitialized() {
        try {
            // This will no-op if already initialized
            BackendRegistry.initialize();
        } catch (Exception e) {
            LOG.warnf(e, "Failed to initialize BackendRegistry, backends may not be available");
        }
    }
}
