package tech.kayys.wayang.tool.mcp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class McpToolCallProtocol {

    static final String METADATA_TOOL_ERROR = "toolError";
    static final String FIELD_NAME = "name";
    static final String FIELD_ARGUMENTS = McpToolInvocationFields.KEY_ARGUMENTS;
    static final String FIELD_CONTENT = "content";
    static final String FIELD_STRUCTURED_CONTENT = "structuredContent";
    static final String FIELD_IS_ERROR = "isError";
    static final String FIELD_TYPE = "type";
    static final String FIELD_TEXT = "text";
    static final String CONTENT_TYPE_TEXT = "text";

    private McpToolCallProtocol() {
    }

    static Map<String, Object> callParams(String toolName, Map<String, Object> arguments) {
        return Map.of(
                FIELD_NAME, toolName,
                FIELD_ARGUMENTS, McpMaps.copy(arguments));
    }

    static ToolCallPayload parse(Object result) {
        Map<String, Object> values = resultObject(result);
        validateIsError(values);
        validateResultShape(values);
        return new ToolCallPayload(values, Boolean.TRUE.equals(values.get(FIELD_IS_ERROR)), text(values));
    }

    static String text(Object result) {
        if (result == null) {
            return "";
        }
        if (result instanceof Map<?, ?> map) {
            Object content = map.get(FIELD_CONTENT);
            if (content instanceof List<?> blocks) {
                String text = blocks.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .filter(block -> CONTENT_TYPE_TEXT.equals(block.get(FIELD_TYPE)))
                        .map(block -> block.get(FIELD_TEXT))
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.joining("\n"));
                if (!text.isBlank()) {
                    return text;
                }
            }
            Object text = map.get(FIELD_TEXT);
            if (text != null) {
                return text.toString();
            }
        }
        return result.toString();
    }

    private static Map<String, Object> resultObject(Object result) {
        if (!(result instanceof Map<?, ?>)) {
            throw new McpToolCallProtocolException("MCP tools/call result must be an object");
        }
        return McpMaps.fromObject(result);
    }

    private static void validateIsError(Map<String, Object> values) {
        Object isError = values.get(FIELD_IS_ERROR);
        if (isError != null && !(isError instanceof Boolean)) {
            throw new McpToolCallProtocolException("MCP tools/call isError must be a boolean");
        }
    }

    private static void validateResultShape(Map<String, Object> values) {
        if (values.containsKey(FIELD_CONTENT)) {
            validateContent(values.get(FIELD_CONTENT));
            return;
        }
        if (values.containsKey(FIELD_TEXT)) {
            if (!(values.get(FIELD_TEXT) instanceof String)) {
                throw new McpToolCallProtocolException("MCP tools/call text must be a string");
            }
            return;
        }
        if (values.containsKey(FIELD_STRUCTURED_CONTENT)) {
            return;
        }
        throw new McpToolCallProtocolException(
                "MCP tools/call result must include content, text, or structuredContent");
    }

    private static void validateContent(Object content) {
        if (!(content instanceof List<?> blocks)) {
            throw new McpToolCallProtocolException("MCP tools/call content must be an array");
        }
        for (int index = 0; index < blocks.size(); index++) {
            validateContentBlock(blocks.get(index), index);
        }
    }

    private static void validateContentBlock(Object value, int index) {
        Map<String, Object> block = contentBlock(value, index);
        Object type = block.get(FIELD_TYPE);
        if (!(type instanceof String typeName) || typeName.isBlank()) {
            throw new McpToolCallProtocolException(
                    "MCP tools/call content block at index " + index + " must include a non-blank type");
        }
        if (CONTENT_TYPE_TEXT.equals(typeName) && !(block.get(FIELD_TEXT) instanceof String)) {
            throw new McpToolCallProtocolException(
                    "MCP tools/call text content block at index " + index + " must include text");
        }
    }

    private static Map<String, Object> contentBlock(Object value, int index) {
        if (!(value instanceof Map<?, ?>)) {
            throw new McpToolCallProtocolException(
                    "MCP tools/call content block at index " + index + " must be an object");
        }
        return McpMaps.fromObject(value);
    }

    record ToolCallPayload(Map<String, Object> result, boolean toolError, String text) {

        ToolCallPayload {
            result = McpMaps.copy(result);
            text = text == null ? "" : text;
        }
    }
}

final class McpToolCallProtocolException extends RuntimeException {

    McpToolCallProtocolException(String message) {
        super(message);
    }
}
