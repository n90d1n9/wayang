package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.agent.spi.AgentOrchestrator;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

/**
 * Minimal execution boundary used by A2A send-message handlers.
 */
@FunctionalInterface
public interface WayangA2aAgentExecutor {

    AgentResponse execute(AgentRequest request);

    static WayangA2aAgentExecutor fromOrchestrator(AgentOrchestrator orchestrator) {
        if (orchestrator == null) {
            throw new IllegalArgumentException("orchestrator must not be null");
        }
        return request -> orchestrator.execute(request).await().indefinitely();
    }
}
