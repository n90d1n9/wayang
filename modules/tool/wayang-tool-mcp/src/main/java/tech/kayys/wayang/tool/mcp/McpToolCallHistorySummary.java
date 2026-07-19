package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Map;

public record McpToolCallHistorySummary(
        int total,
        int succeeded,
        int failed,
        long averageDurationMs,
        long averageMcpDurationMs,
        Instant oldestFinishedAt,
        Instant newestFinishedAt,
        Map<String, Integer> byStatus,
        Map<String, Integer> byFailureType,
        Map<String, Integer> byToolId) {

    public McpToolCallHistorySummary {
        byStatus = byStatus == null ? Map.of() : Map.copyOf(byStatus);
        byFailureType = byFailureType == null ? Map.of() : Map.copyOf(byFailureType);
        byToolId = byToolId == null ? Map.of() : Map.copyOf(byToolId);
    }
}
