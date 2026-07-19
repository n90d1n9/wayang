package tech.kayys.wayang.tool.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record McpServerActionExecutionResult(
        String actionId,
        String status,
        boolean executed,
        String reason,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        McpServerActionPreview preview,
        McpToolDiscoverySyncResult syncResult,
        McpServerActionQueue actionQueueAfter,
        List<String> warnings) {

    static final String STATUS_EXECUTED = "EXECUTED";
    static final String STATUS_FAILED = "FAILED";
    static final String STATUS_INVALID = "INVALID";
    static final String STATUS_NOT_FOUND = "NOT_FOUND";
    static final String STATUS_REJECTED = "REJECTED";

    public McpServerActionExecutionResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    static McpServerActionExecutionResult invalid(String actionId, McpServerActionPreview preview) {
        return new McpServerActionExecutionResult(
                actionId,
                STATUS_INVALID,
                false,
                "Action id is invalid.",
                Instant.now(),
                Instant.now(),
                0,
                preview,
                null,
                null,
                preview == null ? List.of() : preview.warnings());
    }

    static McpServerActionExecutionResult notFound(McpServerActionPreview preview) {
        Instant now = Instant.now();
        return new McpServerActionExecutionResult(
                preview.actionId(),
                STATUS_NOT_FOUND,
                false,
                preview.reason(),
                now,
                now,
                0,
                preview,
                null,
                null,
                preview.warnings());
    }

    static McpServerActionExecutionResult rejected(McpServerActionPreview preview, String reason) {
        Instant now = Instant.now();
        return new McpServerActionExecutionResult(
                preview.actionId(),
                STATUS_REJECTED,
                false,
                reason,
                now,
                now,
                0,
                preview,
                null,
                null,
                preview.warnings());
    }

    static McpServerActionExecutionResult executed(
            McpServerActionPreview preview,
            McpToolDiscoverySyncResult syncResult,
            McpServerActionQueue actionQueueAfter,
            Instant startedAt,
            Instant finishedAt) {
        return new McpServerActionExecutionResult(
                preview.actionId(),
                STATUS_EXECUTED,
                true,
                "Action executed.",
                startedAt,
                finishedAt,
                durationMs(startedAt, finishedAt),
                preview,
                syncResult,
                actionQueueAfter,
                mergeWarnings(preview, syncResult));
    }

    static McpServerActionExecutionResult failed(
            McpServerActionPreview preview,
            Throwable error,
            Instant startedAt,
            Instant finishedAt) {
        return new McpServerActionExecutionResult(
                preview.actionId(),
                STATUS_FAILED,
                false,
                "Action execution failed: " + error.getMessage(),
                startedAt,
                finishedAt,
                durationMs(startedAt, finishedAt),
                preview,
                null,
                null,
                preview.warnings());
    }

    private static long durationMs(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return 0;
        }
        return Math.max(0, Duration.between(startedAt, finishedAt).toMillis());
    }

    private static List<String> mergeWarnings(
            McpServerActionPreview preview,
            McpToolDiscoverySyncResult syncResult) {
        if ((preview == null || preview.warnings().isEmpty())
                && (syncResult == null || syncResult.warnings().isEmpty())) {
            return List.of();
        }
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();
        if (preview != null) {
            warnings.addAll(preview.warnings());
        }
        if (syncResult != null) {
            warnings.addAll(syncResult.warnings());
        }
        return List.copyOf(warnings);
    }
}
