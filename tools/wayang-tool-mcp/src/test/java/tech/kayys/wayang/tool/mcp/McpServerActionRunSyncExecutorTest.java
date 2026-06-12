package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.healthServiceWithOptionalActionFilter;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.preview;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.failingSyncService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.runSyncExecutor;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.successfulSyncService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class McpServerActionRunSyncExecutorTest {

    @Test
    void executesRunSyncAndRefreshesActionQueue() {
        McpToolServerHealth.ActionQueueItem action = action(
                "due",
                McpServerActionCatalog.ACTION_RUN_SYNC);
        McpServerActionRunSyncExecutor executor = runSyncExecutor(
                healthServiceWithOptionalActionFilter(action, "tenant-1", "due"),
                successfulSyncService("tenant-1", "due", new McpToolDiscoverySyncResult(
                        1,
                        2,
                        1,
                        0,
                        List.of("sync warning"))));

        McpServerActionExecutionResult result = executor.execute("tenant-1", preview(action))
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, result.status());
        assertEquals(true, result.executed());
        assertEquals(1, result.syncResult().scanned());
        assertEquals(2, result.syncResult().imported());
        assertNotNull(result.actionQueueAfter());
        assertEquals(1, result.actionQueueAfter().total());
        assertEquals(List.of("preview warning", "sync warning"), result.warnings());
    }

    @Test
    void returnsFailedResultWhenSyncFails() {
        McpToolServerHealth.ActionQueueItem action = action(
                "due",
                McpServerActionCatalog.ACTION_RUN_SYNC);
        McpServerActionRunSyncExecutor executor = runSyncExecutor(
                healthServiceWithOptionalActionFilter(action, "tenant-1", "due"),
                failingSyncService("tenant-1", "due", new IllegalStateException("sync unavailable")));

        McpServerActionExecutionResult result = executor.execute("tenant-1", preview(action))
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_FAILED, result.status());
        assertEquals(false, result.executed());
        assertEquals("Action execution failed: sync unavailable", result.reason());
        assertEquals(null, result.syncResult());
        assertEquals(null, result.actionQueueAfter());
        assertEquals(List.of("preview warning"), result.warnings());
    }

}
