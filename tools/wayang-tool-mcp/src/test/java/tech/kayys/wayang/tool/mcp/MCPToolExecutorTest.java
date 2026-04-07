package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.execution.ExecutionToken;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionStatus;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.node.NodeId;
import tech.kayys.gamelan.engine.run.RetryPolicy;
import tech.kayys.gamelan.engine.workflow.WorkflowRunId;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MCPToolExecutorTest {

    @Test
    void executeFailsWhenToolIdMissing() {
        MCPToolExecutor executor = new MCPToolExecutor();
        NodeExecutionTask task = createTask(Map.of(
                "arguments", Map.of("x", 1)));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertFalse((Boolean) result.output().get("success"));
        assertEquals("ToolId is required for MCP execution", result.output().get("error"));
    }

    @Test
    void executeReturnsPlaceholderSuccessForValidToolId() {
        MCPToolExecutor executor = new MCPToolExecutor();
        NodeExecutionTask task = createTask(Map.of(
                "toolId", "search-docs",
                "arguments", Map.of("query", "wayang")));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertEquals("success", result.output().get("status"));
        assertEquals("mcp", result.output().get("protocol"));
        assertEquals("search-docs", result.output().get("toolId"));
        assertTrue(String.valueOf(result.output().get("result")).contains("placeholder"));
    }

    private NodeExecutionTask createTask(Map<String, Object> context) {
        WorkflowRunId runId = new WorkflowRunId(UUID.randomUUID().toString());
        NodeId nodeId = new NodeId("mcp-tool-test-node");
        int attempt = 1;
        return new NodeExecutionTask(
                runId,
                nodeId,
                attempt,
                ExecutionToken.create(runId, nodeId, attempt, Duration.ofMinutes(5)),
                context,
                RetryPolicy.none());
    }
}
