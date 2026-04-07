package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
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

    @Override
    public String getExecutorType() {
        return ToolNodeTypes.TOOL_MCP;
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context();
        String toolId = (String) context.get("toolId");
        Map<String, Object> arguments = (Map<String, Object>) context.getOrDefault("arguments", Map.of());

        if (toolId == null || toolId.isBlank()) {
            return Uni.createFrom().item(failure(task, "ToolId is required for MCP execution", startedAt));
        }

        LOG.info("Invoking MCP tool: {}", toolId);

        // TODO: Implement actual MCP client call
        return Uni.createFrom().item(success(task, Map.of(
                "status", "success",
                "protocol", "mcp",
                "toolId", toolId,
                "result", "MCP tool execution result placeholder"
        ), startedAt));
    }
}
