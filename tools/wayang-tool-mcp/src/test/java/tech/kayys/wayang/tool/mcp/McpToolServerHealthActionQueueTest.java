package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.recommendedAction;

class McpToolServerHealthActionQueueTest {

    @Test
    void buildsQueueItemWithStableIdPriorityAndExecutionMode() {
        McpToolServerHealth.ActionOperation operation =
                operation("POST", "/mcp/tools/discover/sync/docs");
        McpToolServerHealth.RecommendedAction action = recommendedAction(
                "RUN_SYNC",
                McpIssueSeverity.WARNING,
                true,
                "Run MCP discovery sync.",
                "POST /mcp/tools/discover/sync/docs",
                operation);

        McpToolServerHealth.ActionQueueItem item =
                McpToolServerHealthActionQueue.item(server("docs"), action);

        assertEquals("docs:RUN_SYNC", item.id());
        assertEquals("docs", item.serverName());
        assertEquals(McpServerHealthStatus.DEGRADED, item.healthStatus());
        assertEquals("http", item.transport());
        assertEquals("http://docs.local/mcp", item.endpoint());
        assertEquals("RUN_SYNC", item.code());
        assertEquals(210, item.priority());
        assertEquals(operation, item.operation());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, item.executionMode());
    }

    @Test
    void sortsByPriorityThenServerName() {
        List<McpToolServerHealth.ActionQueueItem> sorted = McpToolServerHealthActionQueue.sorted(List.of(
                item("docs", "REVIEW_STALE_TOOLS", McpIssueSeverity.WARNING, false),
                item("crm", "CHECK_ENDPOINT", McpIssueSeverity.CRITICAL, false),
                item("alpha", "RUN_SYNC", McpIssueSeverity.WARNING, true)));

        assertEquals(List.of(
                        "crm:CHECK_ENDPOINT",
                        "alpha:RUN_SYNC",
                        "docs:REVIEW_STALE_TOOLS"),
                sorted.stream().map(McpToolServerHealth.ActionQueueItem::id).toList());
    }

    @Test
    void windowsQueueWithNormalizedBounds() {
        List<McpToolServerHealth.ActionQueueItem> queue = List.of(
                item("a", "ONE", McpIssueSeverity.INFO, false),
                item("b", "TWO", McpIssueSeverity.INFO, false),
                item("c", "THREE", McpIssueSeverity.INFO, false));

        McpToolServerHealthActionQueue.Window paged = McpToolServerHealthActionQueue.window(queue, 1, 1);
        McpToolServerHealthActionQueue.Window negativeBounds = McpToolServerHealthActionQueue.window(queue, -5, -1);
        McpToolServerHealthActionQueue.Window beyondTail = McpToolServerHealthActionQueue.window(queue, 99, 5);

        assertEquals(3, paged.total());
        assertEquals(1, paged.offset());
        assertEquals(1, paged.limit());
        assertEquals(1, paged.returned());
        assertTrue(paged.truncated());
        assertEquals(List.of("b:TWO"), paged.items().stream()
                .map(McpToolServerHealth.ActionQueueItem::id)
                .toList());
        assertEquals(0, negativeBounds.offset());
        assertEquals(0, negativeBounds.limit());
        assertEquals(0, negativeBounds.returned());
        assertTrue(negativeBounds.truncated());
        assertEquals(3, beyondTail.offset());
        assertEquals(0, beyondTail.returned());
        assertFalse(beyondTail.truncated());
    }

    @Test
    void trimsIdsAndHandlesNullParts() {
        assertEquals("docs:RUN_SYNC", McpToolServerHealthActionQueue.id(" docs ", " RUN_SYNC "));
        assertEquals(":", McpToolServerHealthActionQueue.id(null, null));
    }

    private static McpToolServerHealth.ActionQueueItem item(
            String serverName,
            String code,
            String severity,
            boolean safeToAutomate) {
        McpToolServerHealth.RecommendedAction action = recommendedAction(
                code,
                severity,
                safeToAutomate,
                "message",
                "hint");
        return McpToolServerHealthActionQueue.item(server(serverName), action);
    }

    private static McpToolServerHealth.ServerHealth server(String serverName) {
        return new McpToolServerHealth.ServerHealth(
                serverName,
                "http",
                "http://" + serverName + ".local/mcp",
                true,
                null,
                null,
                null,
                false,
                null,
                McpServerHealthStatus.DEGRADED,
                0,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                McpIssueSeverity.WARNING,
                null,
                null,
                0,
                0,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                McpToolLifecycleCounts.emptyLifecycleStates());
    }
}
