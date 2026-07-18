package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.preview;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.executorRegistry;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.successfulExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionExecutorRegistryTest {

    @Test
    void findsExecutorUsingNormalizedActionCode() {
        McpServerActionExecutor runSyncExecutor = successfulExecutor(McpServerActionCatalog.ACTION_RUN_SYNC);

        assertEquals(runSyncExecutor, McpServerActionExecutorRegistry.find(
                List.of(runSyncExecutor),
                "run-sync"));
        assertEquals(runSyncExecutor, McpServerActionExecutorRegistry.find(
                List.of(runSyncExecutor),
                "RUN_SYNC"));
    }

    @Test
    void executesMatchingExecutor() {
        McpServerActionExecutorRegistry registry = executorRegistry(
                successfulExecutor(McpServerActionCatalog.ACTION_RUN_SYNC));
        McpServerActionPreview preview = preview(
                action("docs", McpServerActionCatalog.ACTION_RUN_SYNC),
                List.of());

        McpServerActionExecutionResult result = registry.execute("tenant-1", preview)
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, result.status());
        assertEquals(true, result.executed());
        assertEquals("docs", result.preview().serverName());
    }

    @Test
    void rejectsWhenNoExecutorSupportsActionCode() {
        McpServerActionExecutorRegistry registry = executorRegistry(
                successfulExecutor(McpServerActionCatalog.ACTION_RUN_SYNC));
        McpServerActionPreview preview = preview(
                action("docs", McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS),
                List.of());

        McpServerActionExecutionResult result = registry.execute("tenant-1", preview)
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, result.status());
        assertEquals(false, result.executed());
        assertEquals(McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE, result.reason());
    }

}
