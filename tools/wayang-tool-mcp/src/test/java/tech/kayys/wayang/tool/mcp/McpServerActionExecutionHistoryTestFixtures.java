package tech.kayys.wayang.tool.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class McpServerActionExecutionHistoryTestFixtures {
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);

    private McpServerActionExecutionHistoryTestFixtures() {
    }

    static void record(
            McpServerActionExecutionHistoryService service,
            String requestId,
            McpServerActionExecutionResult result) {
        service.record(requestId, result).await().atMost(WAIT_TIMEOUT);
    }

    static McpServerActionExecutionResult executedResult(String serverName, Instant finishedAt) {
        return result(
                serverName + ":" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                serverName,
                McpServerActionCatalog.ACTION_RUN_SYNC,
                finishedAt);
    }

    static McpServerActionExecutionResult result(
            String actionId,
            String status,
            boolean executed,
            String serverName,
            String actionCode,
            Instant finishedAt) {
        return result(actionId, status, executed, serverName, actionCode, finishedAt, List.of());
    }

    static McpServerActionExecutionResult result(
            String actionId,
            String status,
            boolean executed,
            String serverName,
            String actionCode,
            Instant finishedAt,
            List<String> warnings) {
        McpServerActionPreview preview = new McpServerActionPreview(
                actionId,
                executed
                        ? McpServerActionPreviewStatus.AUTOMATABLE
                        : McpServerActionPreviewStatus.REVIEW_REQUIRED,
                true,
                true,
                executed,
                executed
                        ? McpServerActionExecutionMode.AUTOMATABLE
                        : McpServerActionExecutionMode.REVIEW_REQUIRED,
                executed ? McpServerActionRiskLevel.LOW : McpServerActionRiskLevel.MEDIUM,
                status + " action",
                serverName,
                actionCode,
                McpIssueSeverity.INFO,
                100,
                null,
                null,
                warnings);
        return new McpServerActionExecutionResult(
                actionId,
                status,
                executed,
                status + " action",
                finishedAt.minusMillis(10),
                finishedAt,
                10,
                preview,
                null,
                null,
                warnings);
    }
}
