package tech.kayys.wayang.agent.spi;

import java.util.List;
import java.util.Map;

/**
 * Agent Tool - Tools available to agent.
 */
public record AgentTool(
        String toolId,
        String name,
        String description,
        String type, // e.g. "API", "SCRIPT", "SEARCH"
        Map<String, Object> config,
        List<String> permissions) {
}
