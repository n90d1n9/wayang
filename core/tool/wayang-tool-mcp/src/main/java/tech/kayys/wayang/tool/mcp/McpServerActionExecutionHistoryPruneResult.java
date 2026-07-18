package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

public record McpServerActionExecutionHistoryPruneResult(
        int pruned,
        Instant prunedAt) {
}
