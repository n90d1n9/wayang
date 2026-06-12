package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentType;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.List;

/**
 * Public descriptor used by routing layers to discover Hermes mode.
 */
public record HermesAgentModeDescriptor(
        String id,
        AgentType agentType,
        OrchestrationStrategy strategy,
        List<String> features,
        List<String> defaultToolsets,
        List<String> executionBackends) {

    public HermesAgentModeDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Hermes mode id is required");
        }
        agentType = agentType == null ? AgentType.HERMES : agentType;
        strategy = strategy == null ? OrchestrationStrategy.HERMES_AGENT : strategy;
        features = features == null ? List.of() : List.copyOf(features);
        defaultToolsets = defaultToolsets == null ? List.of() : List.copyOf(defaultToolsets);
        executionBackends = executionBackends == null ? List.of() : List.copyOf(executionBackends);
    }
}
