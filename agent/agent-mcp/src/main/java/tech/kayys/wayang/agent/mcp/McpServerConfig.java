package tech.kayys.wayang.agent.mcp;

import java.util.List;
import java.util.Map;

public record McpServerConfig(
        String id,
        String url,
        String command,
        List<String> args,
        McpTransportType transportType,
        boolean enabled,
        Map<String, String> headers) {

    public McpServerConfig {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("MCP server id is required");
        }
        args = args == null ? List.of() : List.copyOf(args);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        transportType = transportType == null ? McpTransportType.HTTP : transportType;
    }

    public static McpServerConfig http(String id, String url) {
        return new McpServerConfig(id, url, null, List.of(), McpTransportType.HTTP, true, Map.of());
    }

    public static McpServerConfig stdio(String id, String command, List<String> args) {
        return new McpServerConfig(id, null, command, args, McpTransportType.STDIO, true, Map.of());
    }
}
