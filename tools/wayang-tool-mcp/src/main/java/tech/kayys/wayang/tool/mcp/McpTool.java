package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MCP Tool implementation.
 * Wraps an MCP tool execution.
 */
public class McpTool implements Tool {

    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> defaultContext;
    private final McpToolClient client;

    public McpTool(String id, String name, String description) {
        this(id, name, description, Map.of(), Map.of(), UnsupportedMcpToolClient.INSTANCE);
    }

    public McpTool(
            String id,
            String name,
            String description,
            Map<String, Object> inputSchema,
            McpToolClient client) {
        this(id, name, description, inputSchema, Map.of(), client);
    }

    public McpTool(
            String id,
            String name,
            String description,
            Map<String, Object> inputSchema,
            Map<String, Object> defaultContext,
            McpToolClient client) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.inputSchema = McpMaps.copy(inputSchema);
        this.defaultContext = McpMaps.copy(defaultContext);
        this.client = Objects.requireNonNullElse(client, UnsupportedMcpToolClient.INSTANCE);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<String, Object> inputSchema() {
        return inputSchema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        return executeAsync(arguments, context).await().indefinitely();
    }

    @Override
    public Uni<ToolResult> executeAsync(Map<String, Object> arguments, ToolContext context) {
        McpToolInvocation invocation = new McpToolInvocation(
                id,
                arguments,
                mergedContext(context));
        return client.callTool(invocation)
                .map(this::toToolResult)
                .onFailure().recoverWithItem(error -> ToolResult.error(
                        errorMessage(error),
                        McpToolOutputFields.toolResultMetadata(
                                0,
                                McpFailureType.metadata(McpFailureType.TRANSPORT))));
    }

    private ToolResult toToolResult(McpToolCallResult result) {
        Map<String, Object> metadata = McpToolOutputFields.toolResultMetadata(
                result.durationMs(),
                result.metadata());
        if (result.success()) {
            return ToolResult.success(result.result(), metadata);
        }
        return ToolResult.error(result.error(), metadata);
    }

    private static String errorMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private Map<String, Object> mergedContext(ToolContext context) {
        Map<String, Object> merged = new LinkedHashMap<>(defaultContext);
        if (context != null) {
            Map<String, Object> contextValues = context.asMap();
            merged.putAll(contextValues);
            merged.putAll(McpToolInvocationFields.customData(contextValues));
        }
        return McpMaps.copy(merged);
    }
}
