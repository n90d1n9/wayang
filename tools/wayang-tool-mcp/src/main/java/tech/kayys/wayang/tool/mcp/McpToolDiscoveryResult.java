package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

public record McpToolDiscoveryResult(
        boolean success,
        String serverName,
        String protocolVersion,
        List<McpDiscoveredTool> tools,
        String error,
        long durationMs,
        Map<String, Object> metadata) {

    public McpToolDiscoveryResult {
        tools = tools == null ? List.of() : List.copyOf(tools);
        metadata = McpMaps.copy(metadata);
    }

    static McpToolDiscoveryResult success(
            String serverName,
            String protocolVersion,
            List<McpDiscoveredTool> tools,
            long durationMs,
            Map<String, Object> metadata) {
        return new McpToolDiscoveryResult(true, serverName, protocolVersion, tools, null, durationMs, metadata);
    }

    static McpToolDiscoveryResult failure(
            String serverName,
            String error,
            long durationMs,
            Map<String, Object> metadata) {
        return new McpToolDiscoveryResult(false, serverName, null, List.of(), error, durationMs, metadata);
    }
}
