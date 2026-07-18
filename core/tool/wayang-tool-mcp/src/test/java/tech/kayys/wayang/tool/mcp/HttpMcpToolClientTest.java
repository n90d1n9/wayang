package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.body;
import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.jsonRpcMethod;
import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.jsonRpcResponse;
import static tech.kayys.wayang.tool.mcp.McpHttpTestServer.respond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMcpToolClientTest {

    private McpHttpTestServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void postsToolsCallJsonRpcRequestAndReturnsResult() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> protocolHeader = new AtomicReference<>();
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            requestBody.set(body);
            protocolHeader.set(exchange.getRequestHeaders().getFirst("MCP-Protocol-Version"));
            respond(exchange, 200, jsonRpcResponse(body, "\"result\":{\"text\":\"hello\"}"));
        });

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "filesystem:read_file",
                Map.of("path", "/tmp/a.txt"),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals("hello", result.text());
        assertTrue(requestBody.get().contains(jsonRpcMethod(McpMethods.TOOLS_CALL)));
        assertTrue(requestBody.get().contains("\"" + McpToolCallProtocol.FIELD_NAME + "\":\"read_file\""));
        assertTrue(requestBody.get().contains("\"path\":\"/tmp/a.txt\""));
        assertEquals(McpHttpJsonRpcClient.DEFAULT_PROTOCOL_VERSION, protocolHeader.get());
        assertEquals(200, result.metadata().get(McpHttpMetadata.HTTP_STATUS));
        assertEquals(McpHttpJsonRpcClient.DEFAULT_PROTOCOL_VERSION,
                result.metadata().get(McpHttpMetadata.PROTOCOL_VERSION));
        assertEquals("application/json", result.metadata().get(McpHttpMetadata.RESPONSE_CONTENT_TYPE));
        assertTrue(result.metadata().get(McpHttpMetadata.REQUEST_ID) instanceof String);
        assertEquals(result.metadata().get(McpHttpMetadata.REQUEST_ID),
                result.metadata().get(McpHttpMetadata.RESPONSE_ID));
    }

    @Test
    void acceptsServerSentEventDataResponses() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                "event: message\n"
                        + "data: " + jsonRpcResponse(
                                body(exchange),
                                "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"streamed\"}]}")
                        + "\n\n",
                "text/event-stream"));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "read_file",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals("streamed", result.text());
        assertEquals("text/event-stream", result.metadata().get(McpHttpMetadata.RESPONSE_CONTENT_TYPE));
    }

    @Test
    void acceptsMultilineServerSentEventDataResponses() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            String response = jsonRpcResponse(
                    exchange,
                    "\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"multiline\"}]}");
            int splitAt = response.indexOf("\"result\"");
            respond(
                    exchange,
                    200,
                    "event: message\n"
                            + "data: " + response.substring(0, splitAt) + "\n"
                            + "data: " + response.substring(splitAt) + "\n\n"
                            + "data: [DONE]\n\n");
        });

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "read_file",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals("multiline", result.text());
    }

    @Test
    void acceptsStructuredContentOnlyResults() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(exchange, "\"result\":{\"structuredContent\":{\"answer\":42}}")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(Map.of("answer", 42), McpMaps.fromObject(
                McpMaps.fromObject(result.result()).get(McpToolCallProtocol.FIELD_STRUCTURED_CONTENT)));
    }

    @Test
    void mapsMcpToolErrorResultToFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(
                        exchange,
                        "\"result\":{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"permission denied\"}]}")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("permission denied", result.error());
        assertEquals(McpFailureType.TOOL.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(true, result.metadata().get(McpToolCallProtocol.METADATA_TOOL_ERROR));
    }

    @Test
    void classifiesMalformedToolCallResultAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(exchange, "\"result\":{\"content\":\"not-an-array\"}")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP tools/call content must be an array", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_CALL, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void classifiesMalformedJsonRpcEnvelopeAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(body(exchange), "1.0", "\"result\":{\"text\":\"hello\"}")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("Failed to parse MCP response: MCP JSON-RPC response jsonrpc must be 2.0", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_CALL, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void classifiesMismatchedJsonRpcResponseIdAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            body(exchange);
            respond(exchange, 200, "{\"jsonrpc\":\"2.0\",\"id\":\"different\",\"result\":{\"text\":\"hello\"}}");
        });

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("Failed to parse MCP response: MCP JSON-RPC response id does not match request id",
                result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_CALL, result.metadata().get(McpHttpMetadata.METHOD));
        assertTrue(result.metadata().get(McpHttpMetadata.REQUEST_ID) instanceof String);
        assertEquals("different", result.metadata().get(McpHttpMetadata.RESPONSE_ID));
    }

    @Test
    void mapsJsonRpcErrorToFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(
                        exchange,
                        "\"error\":{\"code\":-32602,\"message\":\"bad args\",\"data\":{\"field\":\"q\"}}")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("bad args", result.error());
        assertEquals(McpFailureType.JSON_RPC.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(-32602, jsonRpcErrorMetadata(result).get("code"));
        assertEquals("bad args", jsonRpcErrorMetadata(result).get("message"));
        assertEquals(Map.of("field", "q"), jsonRpcErrorMetadata(result).get("data"));
    }

    @Test
    void classifiesMalformedJsonRpcErrorAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(exchange, "\"error\":\"blocked\"")));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("Failed to parse MCP response: MCP JSON-RPC error must be an object", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_CALL, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void classifiesHttpFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(exchange, 503, "{\"error\":\"unavailable\"}"));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP HTTP 503", result.error());
        assertEquals(McpFailureType.HTTP.name(), result.metadata().get(McpFailureType.METADATA_KEY));
    }

    @Test
    void classifiesParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(exchange, 200, "{not-json"));

        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of(HttpMcpToolClient.CONTEXT_MCP_ENDPOINT, server.endpoint())))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertTrue(result.error().startsWith("Failed to parse MCP response:"));
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_CALL, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void failsWhenEndpointIsMissing() {
        McpToolCallResult result = new HttpMcpToolClient().callTool(new McpToolInvocation(
                "search",
                Map.of(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP HTTP endpoint is required in invocation context", result.error());
        assertEquals(McpFailureType.TRANSPORT.name(), result.metadata().get(McpFailureType.METADATA_KEY));
    }

    private Map<String, Object> jsonRpcErrorMetadata(McpToolCallResult result) {
        return McpMaps.fromObject(result.metadata().get(McpHttpMetadata.JSON_RPC_ERROR));
    }
}
