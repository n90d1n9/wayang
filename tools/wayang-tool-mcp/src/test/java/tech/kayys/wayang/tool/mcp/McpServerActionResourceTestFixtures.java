package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class McpServerActionResourceTestFixtures {
    private McpServerActionResourceTestFixtures() {
    }

    static McpServerHealthQuery filteredHealthQuery() {
        return McpServerHealthQuery.of(
                "docs",
                true,
                "HEALTHY",
                false,
                false,
                "stale-tools",
                "warning",
                "warning",
                false,
                false,
                false,
                "active",
                "degraded",
                true,
                "review-stale-tools",
                "warning",
                "warning",
                false,
                2,
                1,
                true,
                "get",
                "/mcp/tools/registry",
                "review-required");
    }

    static McpServerHealthQuery actionQueueQuery(String serverName) {
        return McpServerHealthQuery.of(
                serverName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                true,
                "post",
                null,
                "automatable");
    }

    static McpToolServerHealth serverHealth(String serverName, Instant startedAt) {
        return new McpToolServerHealth(
                1,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                McpServerHealthStatus.HEALTHY,
                McpServerHealthStatus.emptyCounts(),
                2,
                2,
                0,
                0,
                2,
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
                List.of(server(serverName, startedAt)),
                List.of());
    }

    private static McpToolServerHealth.ServerHealth server(String serverName, Instant startedAt) {
        return new McpToolServerHealth.ServerHealth(
                serverName,
                McpServerTransports.HTTP,
                "http://" + serverName + ".local/mcp",
                true,
                "PT5M",
                startedAt,
                startedAt.plusSeconds(300),
                false,
                null,
                McpServerHealthStatus.HEALTHY,
                0,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                null,
                "SUCCESS",
                serverName + " synced",
                2,
                10,
                startedAt,
                startedAt.plusMillis(10),
                startedAt.plusMillis(10),
                null,
                2,
                2,
                0,
                0,
                2,
                0,
                0,
                McpToolLifecycleCounts.emptyLifecycleStates());
    }
}
