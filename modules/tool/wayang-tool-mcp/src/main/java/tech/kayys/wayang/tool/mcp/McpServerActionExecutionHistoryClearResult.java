package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpServerActionExecutionHistoryClearResult(
        int cleared,
        Instant clearedAt) {
}
