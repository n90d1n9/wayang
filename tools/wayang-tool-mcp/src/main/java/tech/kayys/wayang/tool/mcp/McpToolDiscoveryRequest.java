package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

public record McpToolDiscoveryRequest(
        String serverName,
        String endpoint,
        Map<String, Object> context) {

    public McpToolDiscoveryRequest {
        serverName = serverName == null ? null : serverName.trim();
        endpoint = endpoint == null ? null : endpoint.trim();
        context = McpMaps.copy(context);
    }

    Map<String, Object> effectiveContext() {
        Map<String, Object> values = new LinkedHashMap<>(context);
        if (endpoint != null && !endpoint.isBlank()) {
            values.putIfAbsent(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT, endpoint);
        }
        return McpMaps.copy(values);
    }
}
