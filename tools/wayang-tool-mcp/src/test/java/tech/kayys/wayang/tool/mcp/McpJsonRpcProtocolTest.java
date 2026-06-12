package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpJsonRpcProtocolTest {

    @Test
    void buildsRequestsAndNotifications() {
        Map<String, Object> request = McpJsonRpcProtocol.request(
                McpMethods.TOOLS_CALL,
                "request-1",
                Map.of("name", "search"));

        assertEquals(Map.of(
                McpJsonRpcProtocol.FIELD_JSONRPC, McpJsonRpcProtocol.VERSION,
                McpJsonRpcProtocol.FIELD_ID, "request-1",
                McpJsonRpcProtocol.FIELD_METHOD, McpMethods.TOOLS_CALL,
                McpJsonRpcProtocol.FIELD_PARAMS, Map.of("name", "search")), request);
        assertEquals("request-1", McpJsonRpcProtocol.id(request));
        assertEquals(McpMethods.TOOLS_CALL, McpJsonRpcProtocol.method(request));

        assertEquals(Map.of(
                McpJsonRpcProtocol.FIELD_JSONRPC, McpJsonRpcProtocol.VERSION,
                McpJsonRpcProtocol.FIELD_METHOD, McpMethods.INITIALIZED_NOTIFICATION),
                McpJsonRpcProtocol.notification(McpMethods.INITIALIZED_NOTIFICATION, Map.of()));
    }

    @Test
    void acceptsResultAndErrorResponses() {
        assertEquals(Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "result", Map.of("ok", true)), McpJsonRpcProtocol.validateResponse(Map.of(
                        "jsonrpc", "2.0",
                        "id", "1",
                        "result", Map.of("ok", true))));

        assertEquals(Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "error", Map.of("code", -32603, "message", "blocked")), McpJsonRpcProtocol.validateResponse(Map.of(
                        "jsonrpc", "2.0",
                        "id", "1",
                        "error", Map.of("code", -32603, "message", "blocked"))));
    }

    @Test
    void rejectsEmptyOrInvalidVersionResponses() {
        assertEnvelopeError(
                "MCP JSON-RPC response body is required",
                Map.of());
        assertEnvelopeError(
                "MCP JSON-RPC response jsonrpc must be 2.0",
                Map.of("jsonrpc", "1.0", "id", "1", "result", Map.of()));
        assertEnvelopeError(
                "MCP JSON-RPC response jsonrpc must be 2.0",
                Map.of("id", "1", "result", Map.of()));
    }

    @Test
    void rejectsMissingIdOrAmbiguousPayloadResponses() {
        assertEnvelopeError(
                "MCP JSON-RPC response id is required",
                Map.of("jsonrpc", "2.0", "result", Map.of()));
        assertEnvelopeError(
                "MCP JSON-RPC response must not include both result and error",
                Map.of("jsonrpc", "2.0", "id", "1", "result", Map.of(), "error", Map.of()));
        assertEnvelopeError(
                "MCP JSON-RPC response must include result or error",
                Map.of("jsonrpc", "2.0", "id", "1"));
    }

    @Test
    void rejectsMalformedErrorPayloads() {
        assertEnvelopeError(
                "MCP JSON-RPC error must be an object",
                Map.of("jsonrpc", "2.0", "id", "1", "error", "blocked"));
        assertEnvelopeError(
                "MCP JSON-RPC error code must be a number",
                Map.of("jsonrpc", "2.0", "id", "1", "error", Map.of("message", "blocked")));
        assertEnvelopeError(
                "MCP JSON-RPC error message must be a string",
                Map.of("jsonrpc", "2.0", "id", "1", "error", Map.of("code", -32603)));
    }

    @Test
    void validatesExpectedResponseIdWhenProvided() {
        assertEquals(Map.of(
                "jsonrpc", "2.0",
                "id", "request-1",
                "result", Map.of("ok", true)), McpJsonRpcProtocol.validateResponse(Map.of(
                        "jsonrpc", "2.0",
                        "id", "request-1",
                        "result", Map.of("ok", true)),
                        "request-1"));

        McpJsonRpcProtocolException error = assertThrows(
                McpJsonRpcProtocolException.class,
                () -> McpJsonRpcProtocol.validateResponse(Map.of(
                        "jsonrpc", "2.0",
                        "id", "other",
                        "result", Map.of("ok", true)),
                        "request-1"));

        assertEquals("MCP JSON-RPC response id does not match request id", error.getMessage());
    }

    private void assertEnvelopeError(String expectedMessage, Map<String, Object> body) {
        McpJsonRpcProtocolException error = assertThrows(
                McpJsonRpcProtocolException.class,
                () -> McpJsonRpcProtocol.validateResponse(body));

        assertEquals(expectedMessage, error.getMessage());
    }
}
