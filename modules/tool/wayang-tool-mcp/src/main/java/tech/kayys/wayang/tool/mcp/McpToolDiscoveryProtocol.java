package tech.kayys.wayang.tool.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class McpToolDiscoveryProtocol {

    static final String FIELD_PROTOCOL_VERSION = "protocolVersion";
    static final String FIELD_CAPABILITIES = "capabilities";
    static final String FIELD_CLIENT_INFO = "clientInfo";
    static final String FIELD_NAME = "name";
    static final String FIELD_VERSION = "version";
    static final String FIELD_TOOLS = "tools";
    static final String FIELD_NEXT_CURSOR = "nextCursor";
    static final String FIELD_CURSOR = "cursor";
    static final String FIELD_TITLE = "title";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_INPUT_SCHEMA = "inputSchema";
    static final String FIELD_OUTPUT_SCHEMA = "outputSchema";
    static final String FIELD_ANNOTATIONS = "annotations";
    static final String FIELD_READ_ONLY_HINT = "readOnlyHint";
    static final String FIELD_SCHEMA_TYPE = "type";
    static final String SCHEMA_OBJECT = "object";

    private McpToolDiscoveryProtocol() {
    }

    static Map<String, Object> initializeParams(String protocolVersion, String clientName, String clientVersion) {
        return Map.of(
                FIELD_PROTOCOL_VERSION, protocolVersion,
                FIELD_CAPABILITIES, Map.of(),
                FIELD_CLIENT_INFO, Map.of(
                        FIELD_NAME, clientName,
                        FIELD_VERSION, clientVersion));
    }

    static Map<String, Object> toolsListParams(String cursor) {
        return cursor == null || cursor.isBlank()
                ? Map.of()
                : Map.of(FIELD_CURSOR, cursor);
    }

    static Map<String, Object> defaultInputSchema() {
        return Map.of(FIELD_SCHEMA_TYPE, SCHEMA_OBJECT);
    }

    static boolean readOnlyHint(McpDiscoveredTool tool) {
        return tool != null && readOnlyHint(tool.metadata());
    }

    static boolean readOnlyHint(Map<String, Object> metadata) {
        Map<String, Object> annotations = McpMaps.fromObject(metadata == null
                ? null
                : metadata.get(FIELD_ANNOTATIONS));
        Object readOnlyHint = annotations.get(FIELD_READ_ONLY_HINT);
        return readOnlyHint instanceof Boolean value && value;
    }

    static String initializeProtocolVersion(Object result, String fallback) {
        Map<String, Object> values = resultObject(result, McpMethods.INITIALIZE);
        return stringValue(values.get(FIELD_PROTOCOL_VERSION), fallback);
    }

    static ToolsListPayload toolsList(Object result) {
        Map<String, Object> values = resultObject(result, McpMethods.TOOLS_LIST);
        Object toolsValue = values.get(FIELD_TOOLS);
        if (!(toolsValue instanceof List<?> rawTools)) {
            throw new McpToolDiscoveryProtocolException("MCP tools/list result must include a tools array");
        }

        List<Object> tools = new ArrayList<>();
        for (int index = 0; index < rawTools.size(); index++) {
            tools.add(validTool(rawTools.get(index), index));
        }
        return new ToolsListPayload(List.copyOf(tools), stringValue(values.get(FIELD_NEXT_CURSOR), null));
    }

    private static Map<String, Object> resultObject(Object result, String method) {
        if (result == null) {
            return Map.of();
        }
        if (!(result instanceof Map<?, ?>)) {
            throw new McpToolDiscoveryProtocolException("MCP " + method + " result must be an object");
        }
        return McpMaps.fromObject(result);
    }

    private static Map<String, Object> validTool(Object value, int index) {
        if (!(value instanceof Map<?, ?>)) {
            throw new McpToolDiscoveryProtocolException(
                    "MCP tools/list tool at index " + index + " must be an object");
        }
        Map<String, Object> tool = McpMaps.fromObject(value);
        if (stringValue(tool.get(FIELD_NAME), null) == null) {
            throw new McpToolDiscoveryProtocolException(
                    "MCP tools/list tool at index " + index + " must include a non-blank name");
        }
        return tool;
    }

    private static String stringValue(Object value, String fallback) {
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        return fallback;
    }

    record ToolsListPayload(List<Object> tools, String nextCursor) {

        ToolsListPayload {
            tools = tools == null ? List.of() : List.copyOf(tools);
        }
    }
}

final class McpToolDiscoveryProtocolException extends RuntimeException {

    McpToolDiscoveryProtocolException(String message) {
        super(message);
    }
}
