package tech.kayys.wayang.agent.mcp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record McpToolCallResult(
        boolean success,
        Object result,
        String error,
        long durationMs,
        Map<String, Object> metadata) {

    public McpToolCallResult {
        metadata = McpMaps.copy(metadata);
    }

    public McpToolCallResult(boolean success, Object result, String error, long durationMs) {
        this(success, result, error, durationMs, Map.of());
    }

    public static McpToolCallResult success(Object result, long durationMs) {
        return success(result, durationMs, Map.of());
    }

    public static McpToolCallResult success(Object result, long durationMs, Map<String, Object> metadata) {
        return new McpToolCallResult(true, result, null, durationMs, metadata);
    }

    public static McpToolCallResult failure(String error, long durationMs) {
        return failure(error, durationMs, Map.of());
    }

    public static McpToolCallResult failure(String error, long durationMs, Map<String, Object> metadata) {
        return new McpToolCallResult(false, null, error, durationMs, metadata);
    }

    public String text() {
        if (result == null) {
            return "";
        }
        if (result instanceof Map<?, ?> map) {
            Object content = map.get("content");
            if (content instanceof List<?> blocks) {
                String text = blocks.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .filter(block -> "text".equals(block.get("type")))
                        .map(block -> block.get("text"))
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.joining("\n"));
                if (!text.isBlank()) {
                    return text;
                }
            }
            Object text = map.get("text");
            if (text != null) {
                return text.toString();
            }
        }
        return result.toString();
    }
}
