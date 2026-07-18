package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;

class McpServerActionCatalogTest {

    @Test
    void enableServerActionCarriesPostOperation() {
        McpToolServerHealth.RecommendedAction action = McpServerActionCatalog.enableServer("files");

        assertEquals(McpServerActionCatalog.ACTION_ENABLE_SERVER, action.code());
        assertEquals(McpIssueSeverity.INFO, action.severity());
        assertEquals(false, action.safeToAutomate());
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED, action.executionMode());
        assertEquals("POST /mcp/servers/files/enable", action.actionHint());
        assertEquals(operation("POST", "/mcp/servers/files/enable"), action.operation());
    }

    @Test
    void syncActionsShareCodeButKeepSeverityAndMessage() {
        McpToolServerHealth.RecommendedAction initial = McpServerActionCatalog.initializeServerSync("docs");
        McpToolServerHealth.RecommendedAction scheduled = McpServerActionCatalog.runScheduledSync("docs");

        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, initial.code());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, scheduled.code());
        assertEquals(McpIssueSeverity.WARNING, initial.severity());
        assertEquals(McpIssueSeverity.INFO, scheduled.severity());
        assertEquals("Run MCP discovery sync to initialize this server.", initial.message());
        assertEquals("Run scheduled MCP discovery sync for this server.", scheduled.message());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, initial.executionMode());
        assertEquals(operation("POST", "/mcp/tools/discover/sync/docs"), scheduled.operation());
    }

    @Test
    void manualActionsDoNotCarryStructuredOperations() {
        McpToolServerHealth.RecommendedAction schedule = McpServerActionCatalog.fixSyncSchedule("invalid");
        McpToolServerHealth.RecommendedAction endpoint = McpServerActionCatalog.checkEndpoint("http://crm.local/mcp");

        assertEquals(McpServerActionCatalog.ACTION_FIX_SYNC_SCHEDULE, schedule.code());
        assertEquals("Update registry syncSchedule for invalid", schedule.actionHint());
        assertEquals(McpServerActionExecutionMode.MANUAL, schedule.executionMode());
        assertNull(schedule.operation());
        assertEquals(McpServerActionCatalog.ACTION_CHECK_ENDPOINT, endpoint.code());
        assertEquals("Inspect http://crm.local/mcp", endpoint.actionHint());
        assertNull(endpoint.operation());
    }

    @Test
    void reviewActionsPointToRegistryWithFilters() {
        McpToolServerHealth.RecommendedAction stale = McpServerActionCatalog.reviewStaleTools("docs", 2);
        McpToolServerHealth.RecommendedAction serverDisabled =
                McpServerActionCatalog.reviewServerDisabledTools("docs", 3);

        assertEquals(McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS, stale.code());
        assertEquals("Review 2 stale MCP tool(s).", stale.message());
        assertEquals("GET /mcp/tools/registry?serverName=docs&stale=true", stale.actionHint());
        assertEquals(operation(
                        "GET",
                        "/mcp/tools/registry",
                        Map.of("serverName", "docs", "stale", "true")),
                stale.operation());
        assertEquals(McpServerActionCatalog.ACTION_REVIEW_SERVER_DISABLED_TOOLS, serverDisabled.code());
        assertEquals("Review 3 server-disabled MCP tool(s).", serverDisabled.message());
        assertEquals(operation(
                        "GET",
                        "/mcp/tools/registry",
                        Map.of("serverName", "docs", "serverDisabled", "true")),
                serverDisabled.operation());
    }
}
