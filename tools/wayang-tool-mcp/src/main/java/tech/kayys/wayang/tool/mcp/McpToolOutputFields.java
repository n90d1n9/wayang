package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpToolOutputFields {

    static final String PROTOCOL = "mcp";
    static final String KEY_DURATION_MS = "durationMs";
    static final String KEY_ERROR = "error";
    static final String KEY_METADATA = "metadata";
    static final String KEY_PROTOCOL = "protocol";
    static final String KEY_RESULT = "result";
    static final String KEY_STATUS = "status";
    static final String KEY_SUCCESS = "success";
    static final String KEY_TEXT = "text";
    static final String KEY_TOOL_ID = McpToolInvocationFields.KEY_TOOL_ID;
    static final String RAW_MCP_METADATA_KEY = "mcp";
    static final String DEFAULT_ERROR = "MCP tool call failed";
    static final String STATUS_FAILURE = "failure";
    static final String STATUS_SUCCESS = "success";

    private McpToolOutputFields() {
    }

    static void putExecutorBase(
            Map<String, Object> output,
            boolean success,
            String toolId,
            long durationMs) {
        output.put(KEY_SUCCESS, success);
        output.put(KEY_STATUS, status(success));
        output.put(KEY_PROTOCOL, PROTOCOL);
        output.put(KEY_TOOL_ID, toolId);
        output.put(KEY_DURATION_MS, durationMs);
    }

    static Map<String, Object> executorOutput(String toolId, McpToolCallResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        putExecutorBase(output, result.success(), toolId, result.durationMs());
        if (result.success()) {
            putSuccessOutput(output, result.result(), result.text());
        } else {
            putFailureOutput(output, result.error(), result.metadata());
        }
        putMetadata(output, result.metadata());
        return McpMaps.copy(output);
    }

    static Map<String, Object> toolResultMetadata(long durationMs, Map<String, Object> mcpMetadata) {
        Map<String, Object> rawMcpMetadata = McpMaps.copy(mcpMetadata);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(KEY_PROTOCOL, PROTOCOL);
        metadata.put(KEY_DURATION_MS, durationMs);
        McpFailureType.copyMetadataValue(metadata, rawMcpMetadata);
        if (!rawMcpMetadata.isEmpty()) {
            metadata.put(RAW_MCP_METADATA_KEY, rawMcpMetadata);
        }
        return McpMaps.copy(metadata);
    }

    static String status(boolean success) {
        return success ? STATUS_SUCCESS : STATUS_FAILURE;
    }

    private static void putSuccessOutput(Map<String, Object> output, Object result, String text) {
        output.put(KEY_RESULT, result);
        output.put(KEY_TEXT, text);
    }

    private static void putFailureOutput(Map<String, Object> output, String error, Map<String, Object> metadata) {
        output.put(KEY_ERROR, error == null ? DEFAULT_ERROR : error);
        McpFailureType.copyMetadataValue(output, metadata);
    }

    private static void putMetadata(Map<String, Object> output, Map<String, Object> metadata) {
        Map<String, Object> values = McpMaps.copy(metadata);
        if (!values.isEmpty()) {
            output.put(KEY_METADATA, values);
        }
    }
}
