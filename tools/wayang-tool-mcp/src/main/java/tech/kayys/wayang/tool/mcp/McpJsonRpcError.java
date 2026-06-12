package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

record McpJsonRpcError(Integer code, String message, Object data) {

    private static final String DEFAULT_MESSAGE = "MCP JSON-RPC error";

    McpJsonRpcError {
        message = message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
        data = copyData(data);
    }

    static McpJsonRpcError from(Object error) {
        if (error == null) {
            return null;
        }
        if (error instanceof Map<?, ?> map) {
            return new McpJsonRpcError(
                    code(map.get(McpJsonRpcProtocol.FIELD_ERROR_CODE)),
                    message(map.get(McpJsonRpcProtocol.FIELD_ERROR_MESSAGE)),
                    map.get(McpJsonRpcProtocol.FIELD_ERROR_DATA));
        }
        return new McpJsonRpcError(null, String.valueOf(error), null);
    }

    Map<String, Object> metadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (code != null) {
            values.put(McpJsonRpcProtocol.FIELD_ERROR_CODE, code);
        }
        values.put(McpJsonRpcProtocol.FIELD_ERROR_MESSAGE, message);
        if (data != null) {
            values.put(McpJsonRpcProtocol.FIELD_ERROR_DATA, data);
        }
        return McpMaps.copy(values);
    }

    private static Integer code(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String message(Object value) {
        return value == null ? DEFAULT_MESSAGE : String.valueOf(value);
    }

    private static Object copyData(Object data) {
        Map<String, Object> values = McpMaps.fromObject(data);
        return values.isEmpty() ? data : values;
    }
}
