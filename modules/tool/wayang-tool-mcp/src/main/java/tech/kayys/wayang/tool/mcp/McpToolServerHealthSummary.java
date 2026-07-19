package tech.kayys.wayang.tool.mcp;

import java.util.Comparator;
import java.util.List;

final class McpToolServerHealthSummary {

    private McpToolServerHealthSummary() {
    }

    static McpToolServerHealth from(
            McpToolServerHealthSource.Snapshot snapshot,
            McpServerHealthFilters filters) {
        McpServerHealthFilters effectiveFilters = filters == null
                ? McpServerHealthFilters.byServerName(null)
                : filters;
        McpToolServerHealthSource.Snapshot effectiveSnapshot = snapshot == null
                ? new McpToolServerHealthSource.Snapshot(
                        List.of(),
                        McpToolServerHealthInputs.from(null, null, null, null, null))
                : snapshot;

        McpToolServerHealthTotals totals = new McpToolServerHealthTotals();
        List<McpToolServerHealth.ServerHealth> entries = effectiveSnapshot.servers().stream()
                .filter(effectiveFilters::matchesServer)
                .sorted(Comparator.comparing(server -> sortKey(server.getName())))
                .map(server -> McpToolServerHealthEntries.from(
                        server,
                        effectiveSnapshot.inputs().forServer(server.getName())))
                .filter(effectiveFilters::matchesHealth)
                .peek(totals::add)
                .toList();
        return totals.toHealth(entries, effectiveFilters);
    }

    private static String sortKey(String serverName) {
        return McpToolServerHealthToolCounts.serverKey(serverName);
    }
}
