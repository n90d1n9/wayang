package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpToolDiscoverySyncHistoryEntry(
        String serverName,
        String status,
        String message,
        int itemsAffected,
        long durationMs,
        Instant startedAt,
        Instant finishedAt) {
}
