package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpToolCallHistoryStats(
        int runs,
        int entries,
        int maxEntriesPerRun,
        long retentionSeconds,
        Instant oldestEntryAt,
        Instant newestEntryAt,
        Instant inspectedAt) {

    static McpToolCallHistoryStats empty() {
        return new McpToolCallHistoryStats(
                0,
                0,
                0,
                0,
                null,
                null,
                Instant.now());
    }
}
