package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class McpToolDiscoverySyncHistorySummaries {

    private McpToolDiscoverySyncHistorySummaries() {
    }

    static McpToolDiscoverySyncHistorySummary summary(
            List<McpToolDiscoverySyncHistoryEntry> entries) {
        HistorySummaryAccumulator total = new HistorySummaryAccumulator(null);
        Map<String, HistorySummaryAccumulator> servers = new LinkedHashMap<>();
        for (McpToolDiscoverySyncHistoryEntry entry : entries) {
            total.add(entry);
            servers.computeIfAbsent(McpToolDiscoverySyncHistoryEntries.serverKey(entry.serverName()),
                    ignored -> new HistorySummaryAccumulator(entry.serverName()))
                    .add(entry);
        }
        return total.toSummary(servers.values().stream()
                .sorted(Comparator.comparing(HistorySummaryAccumulator::sortKey))
                .map(HistorySummaryAccumulator::toServerSummary)
                .toList());
    }

    private static final class HistorySummaryAccumulator {
        private final String serverName;
        private int total;
        private int success;
        private int error;
        private int itemsAffected;
        private long totalDurationMs;
        private String latestStatus;
        private String latestMessage;
        private Instant lastStartedAt;
        private Instant lastFinishedAt;

        private HistorySummaryAccumulator(String serverName) {
            this.serverName = serverName;
        }

        private void add(McpToolDiscoverySyncHistoryEntry entry) {
            total++;
            if (McpToolDiscoverySyncStatuses.isSuccess(entry.status())) {
                success++;
            } else if (McpToolDiscoverySyncStatuses.isError(entry.status())) {
                error++;
            }
            itemsAffected += entry.itemsAffected();
            totalDurationMs += Math.max(0L, entry.durationMs());
            if (McpToolDiscoverySyncHistoryEntries.isNewer(entry, lastStartedAt, lastFinishedAt)) {
                latestStatus = entry.status();
                latestMessage = entry.message();
                lastStartedAt = entry.startedAt();
                lastFinishedAt = entry.finishedAt();
            }
        }

        private String sortKey() {
            return McpToolDiscoverySyncHistoryEntries.serverKey(serverName);
        }

        private McpToolDiscoverySyncHistorySummary toSummary(
                List<McpToolDiscoverySyncHistorySummary.ServerSummary> servers) {
            return new McpToolDiscoverySyncHistorySummary(
                    total,
                    success,
                    error,
                    itemsAffected,
                    totalDurationMs,
                    latestStatus,
                    latestMessage,
                    lastStartedAt,
                    lastFinishedAt,
                    servers);
        }

        private McpToolDiscoverySyncHistorySummary.ServerSummary toServerSummary() {
            return new McpToolDiscoverySyncHistorySummary.ServerSummary(
                    serverName,
                    total,
                    success,
                    error,
                    itemsAffected,
                    totalDurationMs,
                    latestStatus,
                    latestMessage,
                    lastStartedAt,
                    lastFinishedAt);
        }
    }
}
