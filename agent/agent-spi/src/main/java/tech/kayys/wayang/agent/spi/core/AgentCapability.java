package tech.kayys.wayang.agent.spi.core;

import java.util.Map;

/**
 * Agent Capability - Specific instance of a capability with configuration.
 */
public record AgentCapability(
        String name,
        String description,
        AgentCapabilityType type,
        Map<String, Object> config) {
}
