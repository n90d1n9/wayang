package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Agent Task - Unit of work for an agent.
 */
public record AgentTask(
        String taskId,
        String instruction,
        Map<String, Object> context,
        List<String> requiredCapabilities) {
}
