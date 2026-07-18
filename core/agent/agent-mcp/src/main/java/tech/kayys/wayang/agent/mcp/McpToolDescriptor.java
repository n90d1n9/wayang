package tech.kayys.wayang.agent.mcp;

import java.util.Map;

public record McpToolDescriptor(
        String serverId,
        String name,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> metadata) {

    public McpToolDescriptor {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("MCP server id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP tool name is required");
        }
        description = description == null || description.isBlank() ? name : description;
        inputSchema = McpMaps.copy(inputSchema);
        metadata = McpMaps.copy(metadata);
    }

    public String id() {
        return serverId + ":" + name;
    }
}
