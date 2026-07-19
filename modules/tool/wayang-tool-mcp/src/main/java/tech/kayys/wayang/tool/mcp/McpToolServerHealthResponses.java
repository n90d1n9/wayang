package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

final class McpToolServerHealthResponses {

    static final String WARNING_REGISTRY_DATABASE_MODE_DISABLED =
            "MCP server health is unavailable: MCP registry database mode is not enabled.";
    static final String WARNING_REGISTRY_NOT_CONFIGURED =
            "MCP server health is unavailable: MCP server registry is not configured.";

    private McpToolServerHealthResponses() {
    }

    static McpToolServerHealth registryDatabaseModeDisabled() {
        return unavailable(WARNING_REGISTRY_DATABASE_MODE_DISABLED);
    }

    static McpToolServerHealth registryNotConfigured() {
        return unavailable(WARNING_REGISTRY_NOT_CONFIGURED);
    }

    static McpToolServerHealth unavailable(String warning) {
        return empty(List.of(warning));
    }

    static McpToolServerHealth empty() {
        return empty(List.of());
    }

    private static McpToolServerHealth empty(List<String> warnings) {
        return new McpToolServerHealth(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                McpServerHealthStatus.emptyCounts(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                McpToolLifecycleCounts.emptyLifecycleStates(),
                Map.of(),
                Map.of(),
                null,
                0,
                0,
                0,
                0,
                0,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                0,
                0,
                null,
                0,
                false,
                List.of(),
                List.of(),
                warnings);
    }
}
