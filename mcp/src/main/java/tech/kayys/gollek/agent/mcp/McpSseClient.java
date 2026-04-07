package tech.kayys.gollek.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.logging.Logger;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real MCP client over HTTP + Server-Sent Events (SSE) transport.
 *
 * <h2>Transport protocol</h2>
 * The MCP SSE transport uses:
 * <ul>
 *   <li>A persistent GET connection to {@code /sse} for server-to-client messages.</li>
 *   <li>POST requests to {@code /message} for client-to-server JSON-RPC calls.</li>
 * </ul>
 *
 * <h2>Connection lifecycle</h2>
 * <pre>
 * 1. GET /sse                       ← open SSE channel, receive "endpoint" event
 * 2. POST {endpoint} initialize     ← send initialize, receive response via SSE
 * 3. POST {endpoint} initialized    ← send notification (no response)
 * 4. Normal operation: POST tool calls, receive results via SSE
 * </pre>
 *
 * <h2>Thread safety</h2>
 * Pending requests are stored in a {@code ConcurrentHashMap} keyed by request id.
 * The SSE event handler completes the matching {@code CompletableFuture} when
 * the response arrives. All outgoing calls are serialised via the reactive chain.
 */
public class McpSseClient implements McpClient {

    private static final Logger LOG = Logger.getLogger(McpSseClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String serverId;
    private final String baseUrl;
    private final Map<String, String> headers;
    private final WebClient httpClient;
    private final AtomicLong idCounter    = new AtomicLong(1);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** Pending JSON-RPC calls: id → CompletableFuture<JsonRpcResponse> */
    private final ConcurrentHashMap<String, CompletableFuture<McpProtocol.JsonRpcResponse>> pending
            = new ConcurrentHashMap<>();

    /** SSE endpoint URL received from the server during the connection handshake. */
    private volatile String postEndpoint;
    private volatile McpProtocol.ServerCapabilities serverCapabilities;
    private volatile McpProtocol.ServerInfo         serverInfo;

    public McpSseClient(Vertx vertx, String serverId, String baseUrl, Map<String, String> headers) {
        this.serverId   = serverId;
        this.baseUrl    = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.headers    = headers != null ? Map.copyOf(headers) : Map.of();

        URI uri = URI.create(baseUrl);
        WebClientOptions opts = new WebClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort() > 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80))
                .setSsl("https".equals(uri.getScheme()))
                .setTrustAll(true)            // use proper truststore in production
                .setConnectTimeout(10_000)
                .setIdleTimeout(300_000);     // 5 min: SSE connections are long-lived
        this.httpClient = WebClient.create(vertx, opts);
    }

    @Override public String serverId()   { return serverId; }
    @Override public String serverName() { return serverInfo != null ? serverInfo.name() : serverId; }
    @Override public boolean isConnected() { return connected.get(); }
    @Override public McpProtocol.ServerCapabilities capabilities() { return serverCapabilities; }

    // ── Connection lifecycle ───────────────────────────────────────────────────

    @Override
    public Uni<Void> connect() {
        if (connected.get()) return Uni.createFrom().voidItem();

        return openSseChannel()
                .chain(() -> sendInitialize())
                .chain(() -> sendInitialized())
                .invoke(() -> connected.set(true))
                .invoke(() -> LOG.infof("MCP connected: server=%s url=%s caps=%s",
                        serverName(), baseUrl, serverCapabilities));
    }

    /**
     * Open the SSE channel. Parses the "endpoint" event to get the POST URL,
     * then continues to process all incoming events in a background subscription.
     */
    private Uni<Void> openSseChannel() {
        CompletableFuture<Void> endpointReceived = new CompletableFuture<>();

        HttpRequest<Buffer> req = buildRequest(HttpMethod.GET, "/sse");
        req.putHeader("Accept", "text/event-stream");
        req.putHeader("Cache-Control", "no-cache");

        // Start SSE subscription (fire-and-forget; events arrive asynchronously)
        req.send()
                .subscribe().with(
                resp -> {
                    if (resp.statusCode() != 200) {
                        endpointReceived.completeExceptionally(
                                new McpException("SSE connection failed: HTTP " + resp.statusCode()));
                        return;
                    }
                    // Process SSE events from the response body
                    if (resp.body() != null) {
                        processSseChunk(resp.bodyAsString(), endpointReceived);
                    }
                },
                err -> endpointReceived.completeExceptionally(err));

        return Uni.createFrom().completionStage(endpointReceived)
                .ifNoItem().after(Duration.ofSeconds(15))
                .failWith(() -> new McpException("Timed out waiting for SSE endpoint event from " + baseUrl));
    }

    /**
     * Process a raw SSE chunk which may contain multiple events.
     * Each event has the format:
     * <pre>
     * event: &lt;type&gt;\n
     * data: &lt;json&gt;\n
     * \n
     * </pre>
     */
    private void processSseChunk(String chunk, CompletableFuture<Void> endpointFuture) {
        String[] lines = chunk.split("\n");
        String eventType = null;
        StringBuilder dataBuf = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("event:")) {
                eventType = line.substring(6).strip();
            } else if (line.startsWith("data:")) {
                String data = line.substring(5).strip();
                dataBuf.append(data);
            } else if (line.isBlank() && dataBuf.length() > 0) {
                // Dispatch completed event
                dispatchSseEvent(eventType, dataBuf.toString(), endpointFuture);
                eventType = null;
                dataBuf.setLength(0);
            }
        }
    }

    private void dispatchSseEvent(String type, String data, CompletableFuture<Void> endpointFuture) {
        if ("endpoint".equals(type)) {
            // The server tells us where to POST messages
            postEndpoint = data.startsWith("http") ? data : baseUrl + data;
            LOG.debugf("MCP SSE endpoint received: %s", postEndpoint);
            if (!endpointFuture.isDone()) endpointFuture.complete(null);
            return;
        }
        if ("message".equals(type)) {
            try {
                McpProtocol.JsonRpcResponse response = MAPPER.readValue(data, McpProtocol.JsonRpcResponse.class);
                if (response.id() != null) {
                    CompletableFuture<McpProtocol.JsonRpcResponse> fut = pending.remove(response.id());
                    if (fut != null) fut.complete(response);
                }
            } catch (Exception e) {
                LOG.warnf("MCP: failed to parse SSE message event: %s — %s", data, e.getMessage());
            }
        }
    }

    // ── Initialize handshake ──────────────────────────────────────────────────

    private Uni<Void> sendInitialize() {
        McpProtocol.InitializeParams params = McpProtocol.InitializeParams.of("gollek-agent", "1.0.0");
        return sendRequest("initialize", params)
                .map(response -> {
                    try {
                        McpProtocol.InitializeResult result = MAPPER.treeToValue(
                                response.result(), McpProtocol.InitializeResult.class);
                        serverCapabilities = result.capabilities();
                        serverInfo         = result.serverInfo();
                        return null;
                    } catch (Exception e) {
                        throw new McpException("Failed to parse initialize response: " + e.getMessage());
                    }
                }).replaceWithVoid();
    }

    private Uni<Void> sendInitialized() {
        // Notification: no response expected
        McpProtocol.JsonRpcNotification notif = new McpProtocol.JsonRpcNotification(
                "notifications/initialized", Map.of());
        try {
            String body = MAPPER.writeValueAsString(notif);
            return postMessage(body).replaceWithVoid();
        } catch (Exception e) {
            return Uni.createFrom().failure(new McpException("Failed to send initialized: " + e.getMessage()));
        }
    }

    @Override
    public Uni<Void> disconnect() {
        connected.set(false);
        pending.values().forEach(f -> f.completeExceptionally(new McpException("Client disconnecting")));
        pending.clear();
        httpClient.close();
        return Uni.createFrom().voidItem();
    }

    // ── Tool operations ────────────────────────────────────────────────────────

    @Override
    public Uni<List<McpProtocol.McpTool>> listTools() {
        return sendRequest("tools/list", new McpProtocol.ListToolsParams())
                .map(resp -> {
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.ListToolsResult.class).tools();
                    } catch (Exception e) {
                        throw new McpException("tools/list parse error: " + e.getMessage());
                    }
                });
    }

    @Override
    public Uni<McpProtocol.CallToolResult> callTool(String toolName, Map<String, Object> arguments) {
        McpProtocol.CallToolParams params = new McpProtocol.CallToolParams(
                toolName, arguments != null ? arguments : Map.of());
        return sendRequest("tools/call", params)
                .map(resp -> {
                    if (resp.error() != null) {
                        throw new McpException("Tool call error [" + resp.error().code() + "]: "
                                + resp.error().message());
                    }
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.CallToolResult.class);
                    } catch (Exception e) {
                        throw new McpException("tools/call parse error: " + e.getMessage());
                    }
                });
    }

    // ── Resource operations ────────────────────────────────────────────────────

    @Override
    public Uni<List<McpProtocol.McpResource>> listResources() {
        if (serverCapabilities == null || !serverCapabilities.hasResources())
            return Uni.createFrom().item(List.of());
        return sendRequest("resources/list", new McpProtocol.ListResourcesParams())
                .map(resp -> {
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.ListResourcesResult.class).resources();
                    } catch (Exception e) { throw new McpException(e.getMessage()); }
                });
    }

    @Override
    public Uni<McpProtocol.ReadResourceResult> readResource(String uri) {
        return sendRequest("resources/read", new McpProtocol.ReadResourceParams(uri))
                .map(resp -> {
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.ReadResourceResult.class);
                    } catch (Exception e) { throw new McpException(e.getMessage()); }
                });
    }

    // ── Prompt operations ──────────────────────────────────────────────────────

    @Override
    public Uni<List<McpProtocol.McpPrompt>> listPrompts() {
        if (serverCapabilities == null || !serverCapabilities.hasPrompts())
            return Uni.createFrom().item(List.of());
        return sendRequest("prompts/list", new McpProtocol.ListPromptsParams())
                .map(resp -> {
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.ListPromptsResult.class).prompts();
                    } catch (Exception e) { throw new McpException(e.getMessage()); }
                });
    }

    @Override
    public Uni<McpProtocol.GetPromptResult> getPrompt(String name, Map<String, String> arguments) {
        return sendRequest("prompts/get", new McpProtocol.GetPromptParams(name, arguments))
                .map(resp -> {
                    try {
                        return MAPPER.treeToValue(resp.result(), McpProtocol.GetPromptResult.class);
                    } catch (Exception e) { throw new McpException(e.getMessage()); }
                });
    }

    // ── JSON-RPC transport ────────────────────────────────────────────────────

    private Uni<McpProtocol.JsonRpcResponse> sendRequest(String method, Object params) {
        String id = String.valueOf(idCounter.getAndIncrement());
        McpProtocol.JsonRpcRequest request = new McpProtocol.JsonRpcRequest(id, method, params);

        try {
            String body = MAPPER.writeValueAsString(request);
            CompletableFuture<McpProtocol.JsonRpcResponse> future = new CompletableFuture<>();
            pending.put(id, future);

            return postMessage(body)
                    .chain(() -> Uni.createFrom().completionStage(future)
                            .ifNoItem().after(DEFAULT_TIMEOUT)
                            .failWith(() -> {
                                pending.remove(id);
                                return new McpException("Timeout waiting for response to method=" + method);
                            }));
        } catch (Exception e) {
            return Uni.createFrom().failure(new McpException("Failed to serialise request: " + e.getMessage()));
        }
    }

    private Uni<HttpResponse<Buffer>> postMessage(String body) {
        String endpoint = postEndpoint != null ? postEndpoint : baseUrl + "/message";
        URI uri = URI.create(endpoint);
        String path = uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

        HttpRequest<Buffer> req = buildRequest(HttpMethod.POST, path);
        req.putHeader("Content-Type", "application/json");

        return req.sendBuffer(Buffer.buffer(body))
                .invoke(resp -> {
                    if (resp.statusCode() >= 400)
                        LOG.warnf("MCP POST returned %d: %s", resp.statusCode(), resp.bodyAsString());
                });
    }

    private HttpRequest<Buffer> buildRequest(HttpMethod method, String path) {
        HttpRequest<Buffer> req = httpClient.request(method, path);
        headers.forEach(req::putHeader);
        req.putHeader("Accept", "application/json, text/event-stream");
        return req;
    }

    /** Typed exception for MCP-specific errors. */
    public static class McpException extends RuntimeException {
        public McpException(String message) { super(message); }
        public McpException(String message, Throwable cause) { super(message, cause); }
    }
}
