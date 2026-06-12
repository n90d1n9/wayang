package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class UnsupportedMcpToolClient implements McpToolClient {

    static final UnsupportedMcpToolClient INSTANCE = new UnsupportedMcpToolClient();

    @Override
    public Uni<McpToolCallResult> callTool(McpToolInvocation invocation) {
        return Uni.createFrom().item(McpToolCallResult.failure(
                "No MCP tool client configured for tool: " + invocation.toolId(),
                0));
    }
}
