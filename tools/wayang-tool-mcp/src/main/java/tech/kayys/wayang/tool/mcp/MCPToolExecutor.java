package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.wayang.tool.executor.AbstractToolExecutor;
import tech.kayys.wayang.tool.node.ToolNodeTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Executor for MCP tools.
 */
@ApplicationScoped
public class MCPToolExecutor extends AbstractToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(MCPToolExecutor.class);
    private final McpToolCallHistoryService fallbackHistoryService = new McpToolCallHistoryService();

    @Inject
    McpToolClient client;

    @Inject
    McpToolCallHistoryService historyService;

    public MCPToolExecutor() {
    }

    MCPToolExecutor(McpToolClient client) {
        this.client = client;
    }

    MCPToolExecutor(McpToolClient client, McpToolCallHistoryService historyService) {
        this.client = client;
        this.historyService = historyService;
    }

    @Override
    public String getExecutorType() {
        return ToolNodeTypes.TOOL_MCP;
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = McpMaps.copy(task.context());
        String toolId = McpToolInvocationFields.toolId(context);
        Map<String, Object> arguments = McpToolInvocationFields.arguments(context);

        if (toolId == null || toolId.isBlank()) {
            return Uni.createFrom().item(failure(task, "ToolId is required for MCP execution", startedAt));
        }

        LOG.info("Invoking MCP tool: {}", toolId);

        McpToolInvocation invocation = new McpToolInvocation(toolId, arguments, context);
        return mcpClient().callTool(invocation)
                .onFailure().recoverWithItem(error -> McpToolCallResult.failure(
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(),
                        0,
                        McpFailureType.metadata(McpFailureType.TRANSPORT)))
                .onItem().transformToUni(result -> historyService()
                        .record(task, toolId, result, startedAt)
                        .onFailure().recoverWithItem(result))
                .map(result -> success(task, result.toOutput(toolId), startedAt));
    }

    private McpToolClient mcpClient() {
        return client == null ? UnsupportedMcpToolClient.INSTANCE : client;
    }

    private McpToolCallHistoryService historyService() {
        return historyService == null ? fallbackHistoryService : historyService;
    }

}
