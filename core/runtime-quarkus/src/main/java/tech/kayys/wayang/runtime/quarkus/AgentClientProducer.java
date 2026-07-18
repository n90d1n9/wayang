package tech.kayys.wayang.runtime.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.core.AgentClient;
import tech.kayys.wayang.agent.core.core.AgentConfig;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

/**
 * Quarkus CDI producer for {@link AgentClient}.
 *
 * <p>
 * This is a thin adapter layer that creates {@code AgentClient} using the
 * builder pattern, with dependencies injected by CDI. This enables zero-config
 * auto-wiring in Quarkus applications:
 * </p>
 *
 * <pre>{@code
 * @Inject AgentClient agentClient;  // Works automatically!
 * }</pre>
 *
 * <h3>How it works:</h3>
 * <ol>
 *   <li>CDI injects {@code InferenceBackend} and {@code WorkflowBackend}</li>
 *   <li>Producer creates {@code AgentClient} via builder pattern</li>
 *   <li>Client is {@code @ApplicationScoped} — single instance shared</li>
 * </ol>
 *
 * <h3>Backend Resolution:</h3>
 * <p>
 * Backends are resolved via BackendRegistry auto-discovery (ServiceLoader).
 * No manual configuration needed if backend adapter JARs are on classpath.
 * </p>
 *
 * @author Wayang Team
 * @version 0.1.0
 * @since 2026-04-06
 */
@ApplicationScoped
public class AgentClientProducer {

    private static final Logger LOG = Logger.getLogger(AgentClientProducer.class);

    @Inject
    InferenceBackend inferenceBackend;

    @Inject
    WorkflowBackend workflowBackend;

    /**
     * Produce application-scoped AgentClient.
     *
     * @return configured agent client for CDI injection
     */
    @Produces
    @ApplicationScoped
    public AgentClient produceAgentClient() {
        LOG.info("Producing AgentClient via CDI producer");

        try {
            AgentClient client = AgentClient.builder()
                .inferenceBackend(inferenceBackend)
                .workflowBackend(workflowBackend)
                .config(AgentConfig.defaults())
                .build();

            LOG.infof("AgentClient produced successfully: inference=%s, workflow=%s",
                client.inferenceBackend().name(),
                client.workflowBackend() != null ? client.workflowBackend().name() : "none");

            return client;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to produce AgentClient");
            throw e;
        }
    }
}
