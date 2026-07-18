package tech.kayys.wayang.tool.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

final class McpToolCallHistoryTestFixtures {
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(3);

    private McpToolCallHistoryTestFixtures() {
    }

    static void append(McpToolCallHistoryStore store, McpToolCallHistoryEntry entry) {
        store.append(entry.runId(), entry)
                .await().atMost(WAIT_TIMEOUT);
    }

    static McpToolCallHistoryEntry entry(
            String runId,
            String toolId,
            Instant finishedAt) {
        return entry(runId, toolId, true, null, null, finishedAt, 25, 5);
    }

    static McpToolCallHistoryEntry entry(
            String runId,
            String toolId,
            boolean success,
            String failureType,
            Instant finishedAt) {
        return entry(runId, toolId, success, failureType, null, finishedAt, 10, 3);
    }

    static McpToolCallHistoryEntry entry(
            String runId,
            String toolId,
            boolean success,
            String failureType,
            Instant finishedAt,
            long durationMs,
            long mcpDurationMs) {
        return entry(runId, toolId, success, failureType, null, finishedAt, durationMs, mcpDurationMs);
    }

    static McpToolCallHistoryEntry entry(
            String runId,
            String toolId,
            boolean success,
            String failureType,
            String error,
            Instant finishedAt) {
        return entry(runId, toolId, success, failureType, error, finishedAt, 25, 5);
    }

    static McpToolCallHistoryEntry entry(
            String runId,
            String toolId,
            boolean success,
            String failureType,
            String error,
            Instant finishedAt,
            long durationMs,
            long mcpDurationMs) {
        Map<String, Object> metadata = failureType == null
                ? Map.of()
                : Map.of(McpFailureType.METADATA_KEY, failureType);
        return new McpToolCallHistoryEntry(
                runId,
                "node-1",
                1,
                toolId,
                McpToolOutputFields.status(success),
                success,
                failureType,
                error,
                finishedAt.minusMillis(durationMs),
                finishedAt,
                durationMs,
                mcpDurationMs,
                metadata);
    }
}
