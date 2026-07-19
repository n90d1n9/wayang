package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.Map;

public record McpToolCallHistoryFailureSummary(
        String failureType,
        int total,
        String latestToolId,
        String latestError,
        Instant oldestFinishedAt,
        Instant newestFinishedAt,
        Map<String, Integer> byToolId) {

    public McpToolCallHistoryFailureSummary {
        byToolId = byToolId == null ? Map.of() : Map.copyOf(byToolId);
    }
}
