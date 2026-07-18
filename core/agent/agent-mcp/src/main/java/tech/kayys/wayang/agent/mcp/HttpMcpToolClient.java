package tech.kayys.wayang.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP JSON-RPC implementation of the agent-side MCP tool client boundary.
 */
public final class HttpMcpToolClient implements McpToolClient {

    private static final Map<String, Object> DEFAULT_INPUT_SCHEMA = Map.of("type", "object");

    private final McpHttpJsonRpcClient jsonRpc;

    public HttpMcpToolClient() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
    }

    HttpMcpToolClient(HttpClient httpClient, ObjectMapper mapper) {
        this(new McpHttpJsonRpcClient(httpClient, mapper));
    }

    HttpMcpToolClient(McpHttpJsonRpcClient jsonRpc) {
        this.jsonRpc = jsonRpc;
    }

    @Override
    public Uni<List<McpToolDescriptor>> listTools(McpServerConfig server) {
        Map<String, Object> context = McpTransportContext.fromServer(server);
        return jsonRpc.request(context, "tools/list", Map.of())
                .map(response -> toToolDescriptors(server, response))
                .onFailure().recoverWithItem(error -> List.of());
    }

    @Override
    public Uni<McpToolCallResult> callTool(McpToolInvocation invocation) {
        return jsonRpc.request(invocation.context(), "tools/call", Map.of(
                        "name", invocation.toolName(),
                        "arguments", invocation.arguments()))
                .map(this::toCallResult)
                .onFailure().recoverWithItem(error -> McpToolCallResult.failure(
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                        0));
    }

    private List<McpToolDescriptor> toToolDescriptors(McpServerConfig server, McpHttpJsonRpcResponse response) {
        if (!response.httpOk() || response.error() != null) {
            return List.of();
        }
        Object tools = response.result();
        if (tools instanceof Map<?, ?> map) {
            tools = map.get("tools");
        }
        if (!(tools instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(McpMaps::fromObject)
                .filter(raw -> !raw.isEmpty())
                .map(raw -> toToolDescriptor(server.id(), raw))
                .toList();
    }

    private McpToolDescriptor toToolDescriptor(String serverId, Map<String, Object> raw) {
        String name = stringValue(raw.get("name"), "unknown");
        String description = stringValue(raw.get("description"), name);
        Map<String, Object> metadata = new LinkedHashMap<>(raw);
        metadata.remove("inputSchema");
        metadata.remove("outputSchema");
        return new McpToolDescriptor(
                serverId,
                name,
                description,
                inputSchema(raw.get("inputSchema")),
                metadata);
    }

    private McpToolCallResult toCallResult(McpHttpJsonRpcResponse response) {
        if (!response.httpOk()) {
            return McpToolCallResult.failure("MCP HTTP " + response.httpStatus(), response.durationMs(), response.metadata());
        }
        if (response.error() != null) {
            return McpToolCallResult.failure(errorMessage(response.error()), response.durationMs(), response.metadata());
        }
        return McpToolCallResult.success(response.result(), response.durationMs(), response.metadata());
    }

    private Map<String, Object> inputSchema(Object value) {
        Map<String, Object> schema = McpMaps.fromObject(value);
        return schema.isEmpty() ? DEFAULT_INPUT_SCHEMA : schema;
    }

    private String stringValue(Object value, String fallback) {
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return fallback;
    }

    private String errorMessage(Object error) {
        if (error instanceof Map<?, ?> map) {
            Object message = map.get("message");
            if (message != null) {
                return String.valueOf(message);
            }
        }
        return String.valueOf(error);
    }
}
