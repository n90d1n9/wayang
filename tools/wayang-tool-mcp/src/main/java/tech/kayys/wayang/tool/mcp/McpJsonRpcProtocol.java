package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

final class McpJsonRpcProtocol {

    static final String VERSION = "2.0";
    static final String FIELD_JSONRPC = "jsonrpc";
    static final String FIELD_ID = "id";
    static final String FIELD_METHOD = "method";
    static final String FIELD_PARAMS = "params";
    static final String FIELD_RESULT = "result";
    static final String FIELD_ERROR = "error";
    static final String FIELD_ERROR_CODE = "code";
    static final String FIELD_ERROR_MESSAGE = "message";
    static final String FIELD_ERROR_DATA = "data";

    private McpJsonRpcProtocol() {
    }

    static Map<String, Object> request(String method, Object id, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(FIELD_JSONRPC, VERSION);
        payload.put(FIELD_ID, id);
        payload.put(FIELD_METHOD, method);
        putParams(payload, params);
        return McpMaps.copy(payload);
    }

    static Map<String, Object> notification(String method, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(FIELD_JSONRPC, VERSION);
        payload.put(FIELD_METHOD, method);
        putParams(payload, params);
        return McpMaps.copy(payload);
    }

    static Object id(Map<String, Object> values) {
        return values.get(FIELD_ID);
    }

    static String method(Map<String, Object> values) {
        Object value = values.get(FIELD_METHOD);
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    static Object result(Map<String, Object> values) {
        return values.containsKey(FIELD_RESULT) ? values.get(FIELD_RESULT) : values;
    }

    static Object error(Map<String, Object> values) {
        return values.get(FIELD_ERROR);
    }

    static Map<String, Object> errorBody(String message) {
        return Map.of(FIELD_ERROR, Map.of(FIELD_ERROR_MESSAGE, message));
    }

    static Map<String, Object> validateResponse(Map<String, Object> body) {
        return validateResponse(body, null);
    }

    static Map<String, Object> validateResponse(Map<String, Object> body, Object expectedId) {
        Map<String, Object> values = McpMaps.copy(body);
        if (values.isEmpty()) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC response body is required");
        }
        if (!VERSION.equals(values.get(FIELD_JSONRPC))) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC response jsonrpc must be 2.0");
        }
        if (!values.containsKey(FIELD_ID)) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC response id is required");
        }
        if (expectedId != null && !expectedId.equals(values.get(FIELD_ID))) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC response id does not match request id");
        }

        boolean hasResult = values.containsKey(FIELD_RESULT);
        boolean hasError = values.containsKey(FIELD_ERROR);
        if (hasResult && hasError) {
            throw new McpJsonRpcProtocolException(
                    "MCP JSON-RPC response must not include both result and error");
        }
        if (!hasResult && !hasError) {
            throw new McpJsonRpcProtocolException(
                    "MCP JSON-RPC response must include result or error");
        }
        if (hasError) {
            validateError(values.get(FIELD_ERROR));
        }
        return values;
    }

    private static void putParams(Map<String, Object> payload, Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            payload.put(FIELD_PARAMS, McpMaps.copy(params));
        }
    }

    private static void validateError(Object error) {
        if (!(error instanceof Map<?, ?>)) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC error must be an object");
        }
        Map<String, Object> values = McpMaps.fromObject(error);
        if (!(values.get(FIELD_ERROR_CODE) instanceof Number)) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC error code must be a number");
        }
        if (!(values.get(FIELD_ERROR_MESSAGE) instanceof String)) {
            throw new McpJsonRpcProtocolException("MCP JSON-RPC error message must be a string");
        }
    }
}

final class McpJsonRpcProtocolException extends RuntimeException {

    McpJsonRpcProtocolException(String message) {
        super(message);
    }
}
