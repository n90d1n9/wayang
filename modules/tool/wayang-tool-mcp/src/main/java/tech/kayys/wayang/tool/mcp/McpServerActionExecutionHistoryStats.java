package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpServerActionExecutionHistoryStats(
        int requests,
        int entries,
        int maxEntriesPerRequest,
        long retentionSeconds,
        Instant oldestEntryAt,
        Instant newestEntryAt,
        Instant inspectedAt) {

    static McpServerActionExecutionHistoryStats empty() {
        return new McpServerActionExecutionHistoryStats(
                0,
                0,
                0,
                0,
                null,
                null,
                Instant.now());
    }
}
