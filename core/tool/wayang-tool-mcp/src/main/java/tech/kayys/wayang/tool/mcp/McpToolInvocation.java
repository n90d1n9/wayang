package tech.kayys.wayang.tool.mcp;

import java.util.Map;

public record McpToolInvocation(
        String toolId,
        Map<String, Object> arguments,
        Map<String, Object> context) {

    public McpToolInvocation {
        if (toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException("MCP tool id is required");
        }
        toolId = toolId.trim();
        arguments = McpMaps.copy(arguments);
        context = McpMaps.copy(context);
    }
}
