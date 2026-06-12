package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;

public record McpToolCallHistoryToolSummaries(
        int totalTools,
        int totalCalls,
        List<McpToolCallHistoryToolSummary> tools,
        Instant summarizedAt) {

    public McpToolCallHistoryToolSummaries {
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
