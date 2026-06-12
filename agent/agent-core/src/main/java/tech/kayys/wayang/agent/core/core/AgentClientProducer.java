package tech.kayys.wayang.agent.core.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.registry.BackendRegistry;
import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

import java.util.Optional;

/**
 * CDI bridge for the backend-agnostic {@link AgentClient}.
 *
 * <p>Runtime modules can contribute {@link AgentOrchestrator} beans and they
 * will be registered by strategy ID. If no backend beans are present, the
 * producer falls back to the ServiceLoader-backed {@link BackendRegistry}.</p>
 */
@ApplicationScoped
public class AgentClientProducer {

    private static final Logger LOG = Logger.getLogger(AgentClientProducer.class);

    @Produces
    @Singleton
    public AgentClient agentClient(
            Instance<InferenceBackend> inferenceBackends,
            Instance<WorkflowBackend> workflowBackends,
            Instance<AgentOrchestrator> orchestrators) {
        AgentClient.Builder builder = AgentClient.builder();

        first(inferenceBackends).ifPresentOrElse(
                builder::inferenceBackend,
                this::initializeBackendRegistry);
        first(workflowBackends).ifPresent(builder::workflowBackend);
        orchestrators.stream().forEach(builder::orchestrator);

        return builder.build();
    }

    private void initializeBackendRegistry() {
        try {
            BackendRegistry.initialize();
        } catch (RuntimeException error) {
            LOG.debugf(error, "Backend registry initialization did not complete during AgentClient production");
        }
    }

    private static <T> Optional<T> first(Instance<T> values) {
        if (values == null) {
            return Optional.empty();
        }
        return values.stream().findFirst();
    }
}
