package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.body;
import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.jsonRpcMethod;
import static tech.kayys.wayang.tool.mcp.McpHttpTestSupport.jsonRpcResponse;
import static tech.kayys.wayang.tool.mcp.McpHttpTestServer.respond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMcpToolDiscoveryClientTest {

    private McpHttpTestServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void initializesSessionAndListsTools() throws IOException {
        List<String> requestBodies = new ArrayList<>();
        List<String> protocolHeaders = new ArrayList<>();
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            requestBodies.add(body);
            protocolHeaders.add(exchange.getRequestHeaders().getFirst("MCP-Protocol-Version"));
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{\"listChanged\":true}},\"serverInfo\":{\"name\":\"docs\"}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"tools\":[{\"name\":\"search\",\"title\":\"Search Docs\",\"description\":\"Search documentation\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"q\":{\"type\":\"string\"}}}}]}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of(HttpMcpToolDiscoveryClient.CONTEXT_CLIENT_NAME, "test-client")))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals("2025-11-25", result.protocolVersion());
        assertEquals(1, result.tools().size());
        assertEquals("docs:search", result.tools().getFirst().id());
        assertEquals("Search Docs", result.tools().getFirst().title());
        assertEquals(Map.of("type", "object", "properties", Map.of("q", Map.of("type", "string"))),
                result.tools().getFirst().inputSchema());
        assertTrue(requestBodies.get(0).contains(jsonRpcMethod(McpMethods.INITIALIZE)));
        assertTrue(requestBodies.stream().anyMatch(
                body -> body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))));
        assertTrue(requestBodies.getLast().contains(jsonRpcMethod(McpMethods.TOOLS_LIST)));
        assertTrue(protocolHeaders.stream().allMatch("2025-11-25"::equals));
    }

    @Test
    void followsToolsListPagination() throws IOException {
        AtomicInteger listRequests = new AtomicInteger();
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{}}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else if (listRequests.incrementAndGet() == 1) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"tools\":[{\"name\":\"one\"}],\"nextCursor\":\"next\"}"));
            } else {
                assertTrue(body.contains("\"cursor\":\"next\""));
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"tools\":[{\"name\":\"two\"}]}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(2, result.tools().size());
        assertEquals("docs:one", result.tools().get(0).id());
        assertEquals("docs:two", result.tools().get(1).id());
        assertEquals(2, listRequests.get());
    }

    @Test
    void mapsToolsListProtocolErrorToFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{}}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"error\":{\"code\":-32603,\"message\":\"blocked\"}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("blocked", result.error());
        assertEquals(McpFailureType.JSON_RPC.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(-32603, jsonRpcErrorMetadata(result).get("code"));
        assertEquals("blocked", jsonRpcErrorMetadata(result).get("message"));
    }

    @Test
    void acceptsEmptyToolsListResult() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{}}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else {
                respond(exchange, 200, jsonRpcResponse(body, "\"result\":{\"tools\":[]}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(0, result.tools().size());
        assertEquals(0, result.metadata().get(McpToolDiscoveryMetadata.TOOL_COUNT));
    }

    @Test
    void classifiesMalformedInitializeResultAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> respond(
                exchange,
                200,
                jsonRpcResponse(body(exchange), "\"result\":\"not-an-object\"")));

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP initialize result must be an object", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.INITIALIZE, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void classifiesMissingToolsArrayAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{}}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else {
                respond(exchange, 200, jsonRpcResponse(body, "\"result\":{}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP tools/list result must include a tools array", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_LIST, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void classifiesToolWithoutNameAsParseFailure() throws IOException {
        server = McpHttpTestServer.start(exchange -> {
            String body = body(exchange);
            if (body.contains(jsonRpcMethod(McpMethods.INITIALIZE))) {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{\"tools\":{}}}"));
            } else if (body.contains(jsonRpcMethod(McpMethods.INITIALIZED_NOTIFICATION))) {
                respond(exchange, 202, "");
            } else {
                respond(exchange, 200, jsonRpcResponse(
                        body,
                        "\"result\":{\"tools\":[{\"description\":\"missing name\"}]}"));
            }
        });

        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                server.endpoint(),
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP tools/list tool at index 0 must include a non-blank name", result.error());
        assertEquals(McpFailureType.PARSE.name(), result.metadata().get(McpFailureType.METADATA_KEY));
        assertEquals(McpMethods.TOOLS_LIST, result.metadata().get(McpHttpMetadata.METHOD));
    }

    @Test
    void failsWhenEndpointIsMissing() {
        McpToolDiscoveryResult result = new HttpMcpToolDiscoveryClient().discoverTools(new McpToolDiscoveryRequest(
                "docs",
                null,
                Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP HTTP endpoint is required in invocation context", result.error());
        assertEquals(McpFailureType.TRANSPORT.name(), result.metadata().get(McpFailureType.METADATA_KEY));
    }

    private Map<String, Object> jsonRpcErrorMetadata(McpToolDiscoveryResult result) {
        return McpMaps.fromObject(result.metadata().get(McpHttpMetadata.JSON_RPC_ERROR));
    }
}
