package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;

public record McpToolRegistrySummary(
        int total,
        int enabled,
        int disabled,
        int stale,
        int active,
        int serverDisabled,
        int retired,
        int readOnly,
        int requiresApproval,
        Map<String, Integer> lifecycleStates,
        List<ServerSummary> servers) {

    public McpToolRegistrySummary {
        lifecycleStates = McpToolLifecycleCounts.copyLifecycleStates(lifecycleStates);
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record ServerSummary(
            String serverName,
            String endpoint,
            int total,
            int enabled,
            int disabled,
            int stale,
            int active,
            int serverDisabled,
            int retired,
            int readOnly,
            int requiresApproval,
            Map<String, Integer> lifecycleStates) {

        public ServerSummary {
            lifecycleStates = McpToolLifecycleCounts.copyLifecycleStates(lifecycleStates);
        }
    }
}
