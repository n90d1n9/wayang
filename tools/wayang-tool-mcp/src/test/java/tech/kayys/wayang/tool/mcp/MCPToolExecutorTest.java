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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryServiceTestHarness.newHistory;

class MCPToolExecutorTest {

    @Test
    void executeFailsWhenToolIdMissing() {
        MCPToolExecutor executor = new MCPToolExecutor();
        NodeExecutionTask task = createTask(Map.of(
                McpToolInvocationFields.KEY_ARGUMENTS, Map.of("x", 1)));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertFalse((Boolean) result.output().get(McpToolOutputFields.KEY_SUCCESS));
        assertEquals("ToolId is required for MCP execution", result.output().get(McpToolOutputFields.KEY_ERROR));
    }

    @Test
    void executeCallsConfiguredMcpClientForValidToolId() {
        McpToolClientTestDouble client = McpToolClientTestDouble.succeeding(McpToolCallResult.success(
                Map.of("text", "found wayang"),
                25,
                Map.of("serverId", "docs")));
        McpToolCallHistoryService historyService = historyService();
        MCPToolExecutor executor = new MCPToolExecutor(client, historyService);
        NodeExecutionTask task = createTask(Map.of(
                McpToolInvocationFields.KEY_TOOL_ID, "search-docs",
                McpToolInvocationFields.KEY_ARGUMENTS, Map.of("query", "wayang")));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertEquals(McpToolOutputFields.STATUS_SUCCESS, result.output().get(McpToolOutputFields.KEY_STATUS));
        assertEquals(McpToolOutputFields.PROTOCOL, result.output().get(McpToolOutputFields.KEY_PROTOCOL));
        assertEquals("search-docs", result.output().get(McpToolOutputFields.KEY_TOOL_ID));
        assertEquals("found wayang", result.output().get(McpToolOutputFields.KEY_TEXT));
        assertEquals(Map.of("query", "wayang"), client.lastInvocation().arguments());
        assertEquals("docs",
                ((Map<?, ?>) result.output().get(McpToolOutputFields.KEY_METADATA)).get("serverId"));

        List<McpToolCallHistoryEntry> history = historyService.list(String.valueOf(task.runId()))
                .await().atMost(Duration.ofSeconds(3));
        assertEquals(1, history.size());
        McpToolCallHistoryEntry entry = history.get(0);
        assertTrue(entry.success());
        assertEquals(McpToolOutputFields.STATUS_SUCCESS, entry.status());
        assertEquals("search-docs", entry.toolId());
        assertEquals(25, entry.mcpDurationMs());
        assertEquals("docs", entry.metadata().get("serverId"));
    }

    @Test
    void executeSurfacesMcpClientFailures() {
        McpToolCallHistoryService historyService = historyService();
        MCPToolExecutor executor = new MCPToolExecutor(McpToolClientTestDouble.succeeding(
                McpToolCallResult.failure(
                        "server unavailable",
                        7,
                        McpFailureType.metadata(McpFailureType.HTTP))),
                historyService);
        NodeExecutionTask task = createTask(Map.of(McpToolInvocationFields.KEY_TOOL_ID, "search-docs"));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertFalse((Boolean) result.output().get(McpToolOutputFields.KEY_SUCCESS));
        assertEquals(McpToolOutputFields.STATUS_FAILURE, result.output().get(McpToolOutputFields.KEY_STATUS));
        assertEquals("server unavailable", result.output().get(McpToolOutputFields.KEY_ERROR));
        assertEquals(McpFailureType.HTTP.name(), result.output().get(McpFailureType.METADATA_KEY));
        assertEquals(McpFailureType.HTTP.name(),
                ((Map<?, ?>) result.output().get(McpToolOutputFields.KEY_METADATA))
                        .get(McpFailureType.METADATA_KEY));

        McpToolCallHistoryEntry entry = historyService.list(String.valueOf(task.runId()))
                .await().atMost(Duration.ofSeconds(3))
                .get(0);
        assertFalse(entry.success());
        assertEquals(McpToolOutputFields.STATUS_FAILURE, entry.status());
        assertEquals("server unavailable", entry.error());
        assertEquals(McpFailureType.HTTP.name(), entry.failureType());
    }

    @Test
    void executeClassifiesUnexpectedClientFailureAsTransport() {
        McpToolCallHistoryService historyService = historyService();
        MCPToolExecutor executor = new MCPToolExecutor(
                McpToolClientTestDouble.failing(new RuntimeException("connection reset")),
                historyService);
        NodeExecutionTask task = createTask(Map.of(McpToolInvocationFields.KEY_TOOL_ID, "search-docs"));

        NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(NodeExecutionStatus.COMPLETED, result.status());
        assertFalse((Boolean) result.output().get(McpToolOutputFields.KEY_SUCCESS));
        assertEquals("connection reset", result.output().get(McpToolOutputFields.KEY_ERROR));
        assertEquals(McpFailureType.TRANSPORT.name(), result.output().get(McpFailureType.METADATA_KEY));

        McpToolCallHistoryEntry entry = historyService.list(String.valueOf(task.runId()))
                .await().atMost(Duration.ofSeconds(3))
                .get(0);
        assertFalse(entry.success());
        assertEquals("connection reset", entry.error());
        assertEquals(McpFailureType.TRANSPORT.name(), entry.failureType());
    }

    private McpToolCallHistoryService historyService() {
        return newHistory().service();
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
