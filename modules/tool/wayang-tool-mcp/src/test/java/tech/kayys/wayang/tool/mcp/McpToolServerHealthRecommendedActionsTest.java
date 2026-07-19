package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.errorHistory;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.lifecycleCounts;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.successHistory;

class McpToolServerHealthRecommendedActionsTest {

    @Test
    void disabledServerReturnsEnableActionOnly() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("files", "stdio", "node files.js", false),
                errorHistory("files"),
                lifecycleCounts("files", true, true),
                new McpToolServerHealthSyncPolicyStatus(null, true, "bad schedule"),
                3);

        assertEquals(1, actions.size());
        McpToolServerHealth.RecommendedAction action = actions.get(0);
        assertEquals(McpServerActionCatalog.ACTION_ENABLE_SERVER, action.code());
        assertEquals(McpIssueSeverity.INFO, action.severity());
        assertEquals(false, action.safeToAutomate());
        assertEquals("POST /mcp/servers/files/enable", action.actionHint());
        assertEquals(operation("POST", "/mcp/servers/files/enable"), action.operation());
    }

    @Test
    void syncFailureRecommendsEndpointReview() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("crm", "http", "http://crm.local/mcp", true),
                errorHistory("crm"),
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                1);

        assertEquals(1, actions.size());
        McpToolServerHealth.RecommendedAction action = actions.get(0);
        assertEquals(McpServerActionCatalog.ACTION_CHECK_ENDPOINT, action.code());
        assertEquals(McpIssueSeverity.CRITICAL, action.severity());
        assertEquals("Inspect http://crm.local/mcp", action.actionHint());
        assertNull(action.operation());
    }

    @Test
    void unsyncedServerRecommendsInitialSync() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("docs", "http", "http://docs.local/mcp", true),
                null,
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                0);

        assertEquals(1, actions.size());
        McpToolServerHealth.RecommendedAction action = actions.get(0);
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, action.code());
        assertEquals(McpIssueSeverity.WARNING, action.severity());
        assertEquals(true, action.safeToAutomate());
        assertEquals(operation("POST", "/mcp/tools/discover/sync/docs"), action.operation());
    }

    @Test
    void syncDueRecommendsScheduledSync() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("docs", "http", "http://docs.local/mcp", true),
                successHistory("docs"),
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, true, null),
                0);

        assertEquals(1, actions.size());
        McpToolServerHealth.RecommendedAction action = actions.get(0);
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, action.code());
        assertEquals(McpIssueSeverity.INFO, action.severity());
        assertEquals("Run scheduled MCP discovery sync for this server.", action.message());
    }

    @Test
    void invalidScheduleRecommendsManualScheduleFix() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("invalid", "http", "http://invalid.local/mcp", true),
                successHistory("invalid"),
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, "Unsupported interval"),
                0);

        assertEquals(1, actions.size());
        McpToolServerHealth.RecommendedAction action = actions.get(0);
        assertEquals(McpServerActionCatalog.ACTION_FIX_SYNC_SCHEDULE, action.code());
        assertEquals(McpIssueSeverity.WARNING, action.severity());
        assertEquals("Update registry syncSchedule for invalid", action.actionHint());
        assertNull(action.operation());
    }

    @Test
    void toolLifecycleIssuesRecommendToolRegistryReviews() {
        List<McpToolServerHealth.RecommendedAction> actions = McpToolServerHealthRecommendedActions.forServer(
                server("docs", "http", "http://docs.local/mcp", true),
                successHistory("docs"),
                lifecycleCounts("docs", true, true),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                0);

        assertEquals(List.of(
                        McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                        McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS),
                actions.stream().map(McpToolServerHealth.RecommendedAction::code).toList());
        assertEquals(operation(
                        "GET",
                        "/mcp/tools/registry",
                        Map.of("serverName", "docs", "stale", "true")),
                actions.get(0).operation());
        assertEquals(operation(
                        "GET",
                        "/mcp/tools/registry",
                        Map.of("serverName", "docs", "serverDisabled", "true")),
                actions.get(1).operation());
    }
}
