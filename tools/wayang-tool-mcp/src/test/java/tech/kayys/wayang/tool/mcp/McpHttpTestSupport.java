package tech.kayys.wayang.tool.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class McpHttpTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private McpHttpTestSupport() {
    }

    static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static String jsonRpcResponse(HttpExchange exchange, String fields) throws IOException {
        return jsonRpcResponse(body(exchange), fields);
    }

    static String jsonRpcResponse(String requestBody, String fields) throws IOException {
        return jsonRpcResponse(requestBody, McpJsonRpcProtocol.VERSION, fields);
    }

    static String jsonRpcResponse(String requestBody, String version, String fields) throws IOException {
        return "{\"" + McpJsonRpcProtocol.FIELD_JSONRPC + "\":\"" + version + "\",\""
                + McpJsonRpcProtocol.FIELD_ID + "\":" + requestIdJson(requestBody) + "," + fields + "}";
    }

    static String jsonRpcMethod(String method) {
        return "\"" + McpJsonRpcProtocol.FIELD_METHOD + "\":\"" + method + "\"";
    }

    private static String requestIdJson(String requestBody) throws IOException {
        return MAPPER.writeValueAsString(MAPPER.readValue(requestBody, MAP_TYPE).get(McpJsonRpcProtocol.FIELD_ID));
    }
}
