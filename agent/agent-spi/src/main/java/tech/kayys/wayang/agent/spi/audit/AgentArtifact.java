package tech.kayys.wayang.agent.spi.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an artifact generated during the agent's execution lifecycle.
 * Common artifact types include: plan, task, implementation, walkthrough.
 */
public record AgentArtifact(
        String id,
        String runId,
        String tenantId,
        String type,
        String content,
        String format, // e.g., "markdown", "json"
        Map<String, Object> metadata,
        Instant createdAt
) {
    public AgentArtifact {
        createdAt = createdAt != null ? createdAt : Instant.now();
        format = format != null ? format : "markdown";
    }

    public static AgentArtifact create(String runId, String tenantId, String type, String content) {
        return new AgentArtifact(
                java.util.UUID.randomUUID().toString(),
                runId,
                tenantId,
                type,
                content,
                "markdown",
                Map.of(),
                Instant.now()
        );
    }
}
