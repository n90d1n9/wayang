package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

final class McpServerHealthFallbacks {

    private static final String SERVER_HEALTH_NOT_CONFIGURED =
            "MCP server health service is not configured";

    private McpServerHealthFallbacks() {
    }

    static McpToolServerHealthService serverHealthService(
            McpToolServerHealthService configured) {
        return configured == null ? new McpToolServerHealthService() {
            @Override
            public Uni<McpToolServerHealth> summarize(String requestId, String serverName) {
                return summarize(requestId, McpServerHealthFilters.byServerName(serverName));
            }

            @Override
            public Uni<McpToolServerHealth> summarize(
                    String requestId,
                    McpServerHealthFilters filters) {
                return Uni.createFrom().item(McpToolServerHealthResponses.unavailable(SERVER_HEALTH_NOT_CONFIGURED));
            }
        } : configured;
    }
}
