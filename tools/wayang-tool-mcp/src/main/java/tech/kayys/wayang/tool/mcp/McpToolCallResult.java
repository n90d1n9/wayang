package tech.kayys.wayang.tool.mcp;

import java.util.Map;

public record McpToolCallResult(
        boolean success,
        Object result,
        String error,
        long durationMs,
        Map<String, Object> metadata) {

    public McpToolCallResult {
        metadata = McpMaps.copy(metadata);
    }

    public static McpToolCallResult success(Object result, long durationMs) {
        return new McpToolCallResult(true, result, null, durationMs, Map.of());
    }

    public static McpToolCallResult success(Object result, long durationMs, Map<String, Object> metadata) {
        return new McpToolCallResult(true, result, null, durationMs, metadata);
    }

    public static McpToolCallResult failure(String error, long durationMs) {
        return new McpToolCallResult(false, null, error, durationMs, Map.of());
    }

    public static McpToolCallResult failure(String error, long durationMs, Map<String, Object> metadata) {
        return new McpToolCallResult(false, null, error, durationMs, metadata);
    }

    public String text() {
        return McpToolCallProtocol.text(result);
    }

    Map<String, Object> toOutput(String toolId) {
        return McpToolOutputFields.executorOutput(toolId, this);
    }
}
