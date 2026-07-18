package tech.kayys.wayang.tool.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@DefaultBean
public class HttpMcpToolClient implements McpToolClient {

    static final String CONTEXT_MCP_ENDPOINT = McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT;
    static final String CONTEXT_MCP_URL = McpHttpJsonRpcClient.CONTEXT_MCP_URL;
    static final String CONTEXT_SERVER_URL = McpHttpJsonRpcClient.CONTEXT_SERVER_URL;
    static final String CONTEXT_URL = McpHttpJsonRpcClient.CONTEXT_URL;
    static final String CONTEXT_HEADERS = McpHttpJsonRpcClient.CONTEXT_HEADERS;
    static final String CONTEXT_MCP_HEADERS = McpHttpJsonRpcClient.CONTEXT_MCP_HEADERS;
    static final String CONTEXT_TOOL_NAME = "mcpToolName";
    static final String CONTEXT_TIMEOUT_MS = McpHttpJsonRpcClient.CONTEXT_TIMEOUT_MS;

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
    public Uni<McpToolCallResult> callTool(McpToolInvocation invocation) {
        return jsonRpc.request(
                        invocation.context(),
                        McpMethods.TOOLS_CALL,
                        McpToolCallProtocol.callParams(toolName(invocation), invocation.arguments()))
                .map(this::toResult)
                .onFailure().recoverWithItem(error -> McpToolCallResult.failure(
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                        0,
                        McpFailureType.metadata(McpFailureType.TRANSPORT)));
    }

    private McpToolCallResult toResult(McpHttpJsonRpcResponse response) {
        if (!response.httpOk()) {
            return McpToolCallResult.failure(
                    "MCP HTTP " + response.httpStatus(),
                    response.durationMs(),
                    response.metadata(McpFailureType.HTTP));
        }

        McpJsonRpcError error = response.jsonRpcError();
        if (error != null) {
            return McpToolCallResult.failure(
                    error.message(),
                    response.durationMs(),
                    response.metadata(response.failureTypeOr(McpFailureType.JSON_RPC)));
        }
        McpToolCallProtocol.ToolCallPayload result;
        try {
            result = McpToolCallProtocol.parse(response.result());
        } catch (McpToolCallProtocolException protocolError) {
            return protocolParseFailure(response, protocolError);
        }
        if (result.toolError()) {
            return McpToolCallResult.failure(
                    toolErrorMessage(result),
                    response.durationMs(),
                    toolErrorMetadata(response));
        }
        return McpToolCallResult.success(result.result(), response.durationMs(), response.metadata());
    }

    private McpToolCallResult protocolParseFailure(
            McpHttpJsonRpcResponse response,
            McpToolCallProtocolException error) {
        return McpToolCallResult.failure(
                error.getMessage(),
                response.durationMs(),
                response.metadata(McpFailureType.PARSE));
    }

    private Map<String, Object> toolErrorMetadata(McpHttpJsonRpcResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>(response.metadata(McpFailureType.TOOL));
        metadata.put(McpToolCallProtocol.METADATA_TOOL_ERROR, true);
        return McpMaps.copy(metadata);
    }

    private String toolErrorMessage(McpToolCallProtocol.ToolCallPayload result) {
        return result.text().isBlank() ? "MCP tool returned an error" : result.text();
    }

    private String toolName(McpToolInvocation invocation) {
        return firstString(invocation.context(), CONTEXT_TOOL_NAME)
                .or(() -> customString(invocation, CONTEXT_TOOL_NAME))
                .orElseGet(() -> {
                    int separator = invocation.toolId().lastIndexOf(':');
                    return separator >= 0 ? invocation.toolId().substring(separator + 1) : invocation.toolId();
                });
    }

    private Optional<String> firstString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return Optional.of(string);
        }
        return Optional.empty();
    }

    private Optional<String> customString(McpToolInvocation invocation, String key) {
        return firstString(McpToolInvocationFields.customData(invocation.context()), key);
    }

}
