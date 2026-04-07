package tech.kayys.wayang.agent.spi;

import java.time.Instant;

/**
 * Active Agent Runtime - Represents an agent that is currently running.
 */
public record ActiveAgent(
                String agentId,
                AgentType type,
                AgentStatus status,
                Instant activatedAt) {
}
