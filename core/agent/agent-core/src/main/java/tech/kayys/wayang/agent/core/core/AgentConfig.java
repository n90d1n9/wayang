package tech.kayys.wayang.agent.core.core;

import java.time.Duration;
import java.util.Map;

/**
 * Lightweight core client configuration.
 */
public record AgentConfig(
        Duration defaultTimeout,
        int defaultMaxSteps,
        Map<String, Object> metadata) {

    public AgentConfig {
        defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofMinutes(2);
        defaultMaxSteps = defaultMaxSteps > 0 ? defaultMaxSteps : 15;
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static AgentConfig defaults() {
        return new AgentConfig(Duration.ofMinutes(2), 15, Map.of());
    }
}
