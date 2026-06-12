package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolDiscoverySyncHistoryLatest {

    private McpToolDiscoverySyncHistoryLatest() {
    }

    static List<McpToolDiscoverySyncHistoryEntry> entries(
            List<RegistrySyncHistory> histories,
            McpToolDiscoverySyncHistoryFilters filters) {
        Map<String, McpToolDiscoverySyncHistoryEntry> latestByServer = new LinkedHashMap<>();
        for (RegistrySyncHistory history : histories) {
            if (!filters.matches(history)) {
                continue;
            }
            McpToolDiscoverySyncHistoryEntry entry = McpToolDiscoverySyncHistoryEntries.toEntry(history);
            String key = McpToolDiscoverySyncHistoryEntries.serverKey(entry.serverName());
            McpToolDiscoverySyncHistoryEntry current = latestByServer.get(key);
            if (current == null || McpToolDiscoverySyncHistoryEntries.compareHistoryTime(entry, current) > 0) {
                latestByServer.put(key, entry);
            }
        }
        return latestByServer.values().stream()
                .sorted(McpToolDiscoverySyncHistoryEntries::compareLatestEntries)
                .limit(filters.limit())
                .toList();
    }
}
