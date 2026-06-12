package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;

final class McpServerRegistryResourceTestFixtures {
    private McpServerRegistryResourceTestFixtures() {
    }

    static McpServerRegistryEntry serverEntry(String serverName, boolean enabled, Instant now) {
        return new McpServerRegistryEntry(
                serverName,
                McpServerTransports.HTTP,
                "http://" + serverName + ".local/mcp",
                enabled,
                "registry.yaml",
                "PT5M",
                now,
                now.minusSeconds(30),
                now);
    }

    static McpServerLifecycleResult lifecycleResult(
            McpServerRegistryEntry server,
            List<String> disabledToolIds,
            List<String> reactivatedToolIds) {
        return new McpServerLifecycleResult(server, disabledToolIds, reactivatedToolIds);
    }

    static McpServerLifecycleImpact lifecycleImpact(McpServerRegistryEntry server) {
        return new McpServerLifecycleImpact(
                server,
                List.of("docs:search", "docs:lookup"),
                List.of("docs:search"),
                List.of(),
                List.of("docs:lookup"),
                List.of(),
                List.of("docs:search"),
                List.of("docs:lookup"),
                List.of("docs:search", "docs:lookup"));
    }

    static McpServerRetirementResult retirementResult(
            McpServerRegistryEntry server,
            List<String> retiredToolIds) {
        return new McpServerRetirementResult(server, retiredToolIds);
    }
}
