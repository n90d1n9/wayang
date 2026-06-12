package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Map;

public record McpToolCallHistoryToolSummary(
        String toolId,
        int total,
        int succeeded,
        int failed,
        long averageDurationMs,
        long averageMcpDurationMs,
        String latestStatus,
        boolean latestSuccess,
        String latestFailureType,
        String latestError,
        Instant oldestFinishedAt,
        Instant newestFinishedAt,
        Map<String, Integer> byStatus,
        Map<String, Integer> byFailureType) {

    public McpToolCallHistoryToolSummary {
        byStatus = byStatus == null ? Map.of() : Map.copyOf(byStatus);
        byFailureType = byFailureType == null ? Map.of() : Map.copyOf(byFailureType);
    }
}
