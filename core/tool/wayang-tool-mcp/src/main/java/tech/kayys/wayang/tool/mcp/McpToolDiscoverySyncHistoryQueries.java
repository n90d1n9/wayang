package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.util.List;

final class McpToolDiscoverySyncHistoryQueries {

    private McpToolDiscoverySyncHistoryQueries() {
    }

    static Uni<List<McpToolDiscoverySyncHistoryEntry>> listHistory(
            RegistrySyncHistoryRepository historyRepository,
            String requestId,
            String serverName,
            String status,
            int limit) {
        if (historyRepository == null) {
            return Uni.createFrom().item(List.of());
        }
        McpToolDiscoverySyncHistoryFilters filters = McpToolDiscoverySyncHistoryFilters.of(
                serverName,
                status,
                limit);
        return historyRepository.listByRequestId(requestId, filters.scanLimit())
                .map(items -> McpToolDiscoverySyncHistoryViews.entries(items, filters));
    }

    static Uni<List<McpToolDiscoverySyncHistoryEntry>> listLatestHistory(
            RegistrySyncHistoryRepository historyRepository,
            String requestId,
            String serverName,
            String status,
            int limit) {
        if (historyRepository == null) {
            return Uni.createFrom().item(List.of());
        }
        McpToolDiscoverySyncHistoryFilters filters = McpToolDiscoverySyncHistoryFilters.latest(
                serverName,
                status,
                limit);
        return historyRepository.listByRequestId(requestId, filters.scanLimit())
                .map(items -> McpToolDiscoverySyncHistoryViews.latestEntries(items, filters));
    }

    static Uni<McpToolDiscoverySyncHistorySummary> summarizeHistory(
            RegistrySyncHistoryRepository historyRepository,
            String requestId,
            String serverName,
            String status,
            int limit) {
        return listHistory(historyRepository, requestId, serverName, status, limit)
                .map(McpToolDiscoverySyncHistoryViews::summary);
    }
}
