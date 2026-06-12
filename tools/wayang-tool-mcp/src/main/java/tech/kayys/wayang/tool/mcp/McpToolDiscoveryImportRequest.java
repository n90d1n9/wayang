package tech.kayys.wayang.tool.mcp;

import java.util.Map;

public record McpToolDiscoveryImportRequest(
        String serverName,
        String endpoint,
        String namespace,
        String createdBy,
        Map<String, Object> context) {

    public McpToolDiscoveryImportRequest {
        serverName = serverName == null ? null : serverName.trim();
        endpoint = endpoint == null ? null : endpoint.trim();
        namespace = namespace == null ? null : namespace.trim();
        createdBy = createdBy == null ? null : createdBy.trim();
        context = McpMaps.copy(context);
    }

    McpToolDiscoveryRequest discoveryRequest() {
        return new McpToolDiscoveryRequest(serverName, endpoint, context);
    }

    String effectiveNamespace() {
        if (namespace != null && !namespace.isBlank()) {
            return namespace;
        }
        if (serverName != null && !serverName.isBlank()) {
            return serverName;
        }
        return "mcp";
    }
}
