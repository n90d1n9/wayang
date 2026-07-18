package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;

public record McpToolDiscoverySyncHistorySummary(
        int total,
        int success,
        int error,
        int itemsAffected,
        long totalDurationMs,
        String latestStatus,
        String latestMessage,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        List<ServerSummary> servers) {

    public McpToolDiscoverySyncHistorySummary {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record ServerSummary(
            String serverName,
            int total,
            int success,
            int error,
            int itemsAffected,
            long totalDurationMs,
            String latestStatus,
            String latestMessage,
            Instant lastStartedAt,
            Instant lastFinishedAt) {
    }
}
