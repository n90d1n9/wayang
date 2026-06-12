package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.util.List;

final class McpToolDiscoverySyncHistoryViews {

    private McpToolDiscoverySyncHistoryViews() {
    }

    static List<McpToolDiscoverySyncHistoryEntry> entries(
            List<RegistrySyncHistory> histories,
            String serverName,
            String status,
            int limit) {
        return entries(histories, McpToolDiscoverySyncHistoryFilters.of(serverName, status, limit));
    }

    static List<McpToolDiscoverySyncHistoryEntry> entries(
            List<RegistrySyncHistory> histories,
            McpToolDiscoverySyncHistoryFilters filters) {
        return histories.stream()
                .filter(filters::matches)
                .limit(filters.limit())
                .map(McpToolDiscoverySyncHistoryEntries::toEntry)
                .toList();
    }

    static List<McpToolDiscoverySyncHistoryEntry> latestEntries(
            List<RegistrySyncHistory> histories,
            String serverName,
            String status,
            int limit) {
        return latestEntries(histories, McpToolDiscoverySyncHistoryFilters.latest(serverName, status, limit));
    }

    static List<McpToolDiscoverySyncHistoryEntry> latestEntries(
            List<RegistrySyncHistory> histories,
            McpToolDiscoverySyncHistoryFilters filters) {
        return McpToolDiscoverySyncHistoryLatest.entries(histories, filters);
    }

    static McpToolDiscoverySyncHistorySummary summary(
            List<McpToolDiscoverySyncHistoryEntry> entries) {
        return McpToolDiscoverySyncHistorySummaries.summary(entries);
    }
}
