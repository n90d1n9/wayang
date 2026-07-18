package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;

public record McpToolCallHistoryFailureSummaries(
        int totalFailureTypes,
        int totalFailures,
        List<McpToolCallHistoryFailureSummary> failureTypes,
        Instant summarizedAt) {

    public McpToolCallHistoryFailureSummaries {
        failureTypes = failureTypes == null ? List.of() : List.copyOf(failureTypes);
    }
}
