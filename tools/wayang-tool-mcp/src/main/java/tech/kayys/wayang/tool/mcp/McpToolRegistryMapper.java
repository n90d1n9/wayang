package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpToolRegistryMapper {

    private McpToolRegistryMapper() {
    }

    static McpTool toMcpTool(
            tech.kayys.wayang.tool.entity.McpTool tool,
            McpToolClient toolClient) {
        return new McpTool(
                tool.getToolId(),
                tool.getName(),
                tool.getDescription(),
                tool.getInputSchema(),
                defaultMcpContext(tool),
                toolClient == null ? UnsupportedMcpToolClient.INSTANCE : toolClient);
    }

    static McpToolRegistryEntry toRegistryEntry(tech.kayys.wayang.tool.entity.McpTool tool) {
        return new McpToolRegistryEntry(
                tool.getToolId(),
                tool.getName(),
                tool.getDescription(),
                tool.getNamespace(),
                McpToolRegistryMetadata.serverName(tool),
                McpToolRegistryMetadata.registryEndpoint(tool),
                tool.isEnabled(),
                McpToolLifecycle.isStale(tool),
                McpToolLifecycle.isServerDisabled(tool),
                McpToolLifecycle.isRetired(tool),
                McpToolLifecycle.lifecycleState(tool),
                tool.isReadOnly(),
                tool.isRequiresApproval(),
                tool.getCapabilities(),
                tool.getTags(),
                tool.getInputSchema(),
                tool.getOperationId(),
                tool.getCreatedAt(),
                tool.getUpdatedAt());
    }

    private static Map<String, Object> defaultMcpContext(tech.kayys.wayang.tool.entity.McpTool tool) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (tool.getExecutionConfig() != null) {
            String endpoint = McpToolRegistryMetadata.endpoint(tool.getExecutionConfig());
            if (endpoint != null && !endpoint.isBlank()) {
                context.put(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT, endpoint);
            }
            if (tool.getExecutionConfig().getHeaders() != null
                    && !tool.getExecutionConfig().getHeaders().isEmpty()) {
                context.put(McpHttpJsonRpcClient.CONTEXT_MCP_HEADERS,
                        tool.getExecutionConfig().getHeaders());
            }
            if (tool.getExecutionConfig().getTimeoutMs() != null) {
                context.put(McpHttpJsonRpcClient.CONTEXT_TIMEOUT_MS,
                        tool.getExecutionConfig().getTimeoutMs());
            }
        }
        if (tool.getOperationId() != null && !tool.getOperationId().isBlank()) {
            context.put(HttpMcpToolClient.CONTEXT_TOOL_NAME, tool.getOperationId());
        }
        return McpMaps.copy(context);
    }
}
