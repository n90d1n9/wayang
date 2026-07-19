package tech.kayys.wayang.tool.mcp;

import tech.kayys.gamelan.engine.node.NodeExecutionTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record McpToolCallHistoryEntry(
        String runId,
        String nodeId,
        int attempt,
        String toolId,
        String status,
        boolean success,
        String failureType,
        String error,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        long mcpDurationMs,
        Map<String, Object> metadata) {

    public McpToolCallHistoryEntry {
        metadata = McpMaps.copy(metadata);
    }

    static McpToolCallHistoryEntry from(
            NodeExecutionTask task,
            String toolId,
            McpToolCallResult result,
            Instant startedAt,
            Instant finishedAt) {
        Instant effectiveFinishedAt = finishedAt == null ? Instant.now() : finishedAt;
        Instant effectiveStartedAt = startedAt == null ? effectiveFinishedAt : startedAt;
        McpToolCallResult effectiveResult = result == null
                ? McpToolCallResult.failure("MCP tool call failed", 0)
                : result;
        return new McpToolCallHistoryEntry(
                task == null ? null : String.valueOf(task.runId()),
                task == null ? null : String.valueOf(task.nodeId()),
                task == null ? 0 : task.attempt(),
                toolId,
                McpToolOutputFields.status(effectiveResult.success()),
                effectiveResult.success(),
                McpFailureType.value(effectiveResult.metadata()),
                effectiveResult.error(),
                effectiveStartedAt,
                effectiveFinishedAt,
                Duration.between(effectiveStartedAt, effectiveFinishedAt).toMillis(),
                effectiveResult.durationMs(),
                effectiveResult.metadata());
    }
}
