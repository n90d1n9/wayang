package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.health;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.preview;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.executionService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionTestHarness.executor;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionExecutionServiceTest {

    @Test
    void delegatesAutomatableRunSyncActionToExecutor() {
        McpToolServerHealth.ActionQueueItem action = action(
                "due",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                true);
        AtomicBoolean executed = new AtomicBoolean(false);
        McpServerActionExecutionService service = executionService(executor(McpServerActionCatalog.ACTION_RUN_SYNC, (
                requestId,
                preview) -> {
            executed.set(true);
            assertEquals("tenant-1", requestId);
            assertEquals("due", preview.serverName());
            return Uni.createFrom().item(McpServerActionExecutionResult.executed(
                    preview,
                    new McpToolDiscoverySyncResult(1, 2, 1, 0, List.of("sync warning")),
                    McpServerActionQueue.from(health(action)),
                    Instant.EPOCH,
                    Instant.EPOCH));
        }));

        McpServerActionExecutionResult result = service.execute("tenant-1", preview(action))
                .await().indefinitely();

        assertEquals(true, executed.get());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, result.status());
        assertEquals(true, result.executed());
        assertEquals(1, result.syncResult().scanned());
        assertEquals(2, result.syncResult().imported());
        assertEquals(1, result.actionQueueAfter().total());
        assertEquals(List.of("preview warning", "sync warning"), result.warnings());
    }

    @Test
    void rejectsAllowedShapeWhenNoExecutorSupportsActionCode() {
        McpToolServerHealth.ActionQueueItem action = action(
                "due",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                true);
        McpServerActionExecutionService service = executionService(List.of());

        McpServerActionExecutionResult result = service.execute("tenant-1", preview(action))
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, result.status());
        assertEquals(false, result.executed());
        assertEquals(McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE, result.reason());
    }

    @Test
    void rejectsUnsupportedAutomatableActionCode() {
        McpToolServerHealth.ActionQueueItem action = action(
                "docs",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                true);
        McpServerActionExecutionService service = executionService(executor(McpServerActionCatalog.ACTION_RUN_SYNC, (
                requestId,
                preview) -> {
            throw new AssertionError("unsupported action must not sync");
        }));

        McpServerActionExecutionResult result = service.execute("tenant-1", preview(action))
                .await().indefinitely();

        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, result.status());
        assertEquals(false, result.executed());
        assertEquals(McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE, result.reason());
    }

}
