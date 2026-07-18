package tech.kayys.wayang.agent.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpMcpToolClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listsToolsOverHttpJsonRpc() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"jsonrpc":"2.0","id":"1","result":{"tools":[{"name":"read_file","description":"Read file","inputSchema":{"type":"object","properties":{"path":{"type":"string"}}},"title":"Read File"}]}}
                    """);
        });

        List<McpToolDescriptor> tools = new HttpMcpToolClient()
                .listTools(McpServerConfig.http("filesystem", endpoint()))
                .await().atMost(Duration.ofSeconds(3));

        assertThat(requestBody.get()).contains("\"method\":\"tools/list\"");
        assertThat(tools).hasSize(1);
        assertThat(tools.getFirst().id()).isEqualTo("filesystem:read_file");
        assertThat(tools.getFirst().description()).isEqualTo("Read file");
        assertThat(tools.getFirst().inputSchema()).containsEntry("type", "object");
        assertThat(tools.getFirst().metadata()).containsEntry("title", "Read File");
    }

    @Test
    void callsToolOverHttpJsonRpcAndReturnsTransportMetadata() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"text\":\"hello\"}}");
        });
        McpToolDescriptor tool = new McpToolDescriptor(
                "filesystem",
                "read_file",
                "Read file",
                Map.of("type", "object"),
                McpTransportContext.fromServer(McpServerConfig.http("filesystem", endpoint())));

        McpToolCallResult result = new HttpMcpToolClient()
                .callTool(McpToolInvocation.of(tool, Map.of("path", "/tmp/a.txt")))
                .await().atMost(Duration.ofSeconds(3));

        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("hello");
        assertThat(result.metadata()).containsEntry("httpStatus", 200);
        assertThat(result.metadata()).containsEntry("endpoint", endpoint());
        assertThat(requestBody.get()).contains("\"method\":\"tools/call\"");
        assertThat(requestBody.get()).contains("\"name\":\"read_file\"");
        assertThat(requestBody.get()).contains("\"path\":\"/tmp/a.txt\"");
    }

    @Test
    void acceptsServerSentEventDataResponses() throws IOException {
        server = server(exchange -> respond(
                exchange,
                200,
                "event: message\n"
                        + "data: {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"streamed\"}]}}\n\n"));
        McpToolDescriptor tool = new McpToolDescriptor(
                "filesystem",
                "read_file",
                "Read file",
                Map.of("type", "object"),
                McpTransportContext.fromServer(McpServerConfig.http("filesystem", endpoint())));

        McpToolCallResult result = new HttpMcpToolClient()
                .callTool(McpToolInvocation.of(tool, Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("streamed");
    }

    @Test
    void mapsJsonRpcAndMissingEndpointFailures() throws IOException {
        server = server(exchange -> respond(
                exchange,
                200,
                "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32602,\"message\":\"bad args\"}}"));
        McpToolDescriptor failingTool = new McpToolDescriptor(
                "filesystem",
                "search",
                "Search",
                Map.of("type", "object"),
                McpTransportContext.fromServer(McpServerConfig.http("filesystem", endpoint())));
        McpToolDescriptor missingEndpointTool = new McpToolDescriptor(
                "filesystem",
                "search",
                "Search",
                Map.of("type", "object"),
                Map.of());

        McpToolCallResult jsonRpcError = new HttpMcpToolClient()
                .callTool(McpToolInvocation.of(failingTool, Map.of()))
                .await().atMost(Duration.ofSeconds(3));
        McpToolCallResult missingEndpoint = new HttpMcpToolClient()
                .callTool(McpToolInvocation.of(missingEndpointTool, Map.of()))
                .await().atMost(Duration.ofSeconds(3));

        assertThat(jsonRpcError.success()).isFalse();
        assertThat(jsonRpcError.error()).isEqualTo("bad args");
        assertThat(missingEndpoint.success()).isFalse();
        assertThat(missingEndpoint.error()).isEqualTo("MCP HTTP endpoint is required in invocation context");
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/mcp", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp";
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
