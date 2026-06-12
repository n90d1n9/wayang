package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.List;

public record McpServerActionExecutionHistoryEntry(
        String actionId,
        String status,
        boolean executed,
        String reason,
        String serverName,
        String actionCode,
        String executionMode,
        String riskLevel,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        Integer scanned,
        Integer imported,
        Integer stale,
        Integer reactivated,
        Integer actionQueueTotalAfter,
        List<String> warnings) {

    public McpServerActionExecutionHistoryEntry {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    static McpServerActionExecutionHistoryEntry from(McpServerActionExecutionResult result) {
        McpServerActionPreview preview = result == null ? null : result.preview();
        McpToolDiscoverySyncResult syncResult = result == null ? null : result.syncResult();
        McpServerActionQueue actionQueueAfter = result == null ? null : result.actionQueueAfter();
        return new McpServerActionExecutionHistoryEntry(
                result == null ? null : result.actionId(),
                result == null ? null : result.status(),
                result != null && result.executed(),
                result == null ? null : result.reason(),
                preview == null ? null : preview.serverName(),
                preview == null ? null : preview.actionCode(),
                preview == null ? null : preview.executionMode(),
                preview == null ? null : preview.riskLevel(),
                result == null ? null : result.startedAt(),
                result == null ? null : result.finishedAt(),
                result == null ? 0 : result.durationMs(),
                syncResult == null ? null : syncResult.scanned(),
                syncResult == null ? null : syncResult.imported(),
                syncResult == null ? null : syncResult.stale(),
                syncResult == null ? null : syncResult.reactivated(),
                actionQueueAfter == null ? null : actionQueueAfter.total(),
                result == null ? List.of() : result.warnings());
    }
}
