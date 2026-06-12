package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.issueDetail;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.recommendedAction;

class McpToolServerHealthTotalsTest {

    @Test
    void aggregatesServerToolIssueAndActionTotals() {
        McpToolServerHealthTotals totals = new McpToolServerHealthTotals();
        McpToolServerHealth.ServerHealth docs = server(
                "docs",
                true,
                McpServerHealthStatus.DEGRADED,
                true,
                3,
                2,
                1,
                1,
                2,
                1,
                0,
                Map.of(McpToolLifecycle.LIFECYCLE_ACTIVE, 2, McpToolLifecycle.LIFECYCLE_STALE, 1),
                List.of(issue(McpToolServerHealthIssues.ISSUE_STALE_TOOLS, McpIssueSeverity.WARNING)),
                List.of(action(
                        "REVIEW_STALE_TOOLS",
                        McpIssueSeverity.WARNING,
                        false,
                        operation("GET", "/mcp/tools/registry"))));
        McpToolServerHealth.ServerHealth crm = server(
                "crm",
                true,
                McpServerHealthStatus.UNHEALTHY,
                true,
                1,
                1,
                0,
                0,
                1,
                0,
                0,
                Map.of(McpToolLifecycle.LIFECYCLE_ACTIVE, 1),
                List.of(issue(McpToolServerHealthIssues.ISSUE_SYNC_ERROR, McpIssueSeverity.CRITICAL)),
                List.of(action("CHECK_ENDPOINT", McpIssueSeverity.CRITICAL, false, null)));
        McpToolServerHealth.ServerHealth files = server(
                "files",
                false,
                McpServerHealthStatus.DISABLED,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Map.of(),
                List.of(issue(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED, McpIssueSeverity.INFO)),
                List.of(action(
                        "ENABLE_SERVER",
                        McpIssueSeverity.INFO,
                        true,
                        operation("POST", "/mcp/servers/files/enable"))));

        List<McpToolServerHealth.ServerHealth> entries = List.of(docs, crm, files);
        entries.forEach(totals::add);

        McpToolServerHealth health = totals.toHealth(entries, McpServerHealthFilters.byActionQueueWindow(1, 1));

        assertEquals(3, health.totalServers());
        assertEquals(2, health.enabledServers());
        assertEquals(1, health.disabledServers());
        assertEquals(1, health.degradedServers());
        assertEquals(1, health.unhealthyServers());
        assertEquals(2, health.attentionRequiredServers());
        assertEquals(McpServerHealthStatus.UNHEALTHY, health.highestHealthStatus());
        assertEquals(1, health.healthStatusCounts().get(McpServerHealthStatus.DEGRADED));
        assertEquals(1, health.healthStatusCounts().get(McpServerHealthStatus.UNHEALTHY));
        assertEquals(1, health.healthStatusCounts().get(McpServerHealthStatus.DISABLED));
        assertEquals(4, health.totalTools());
        assertEquals(3, health.enabledTools());
        assertEquals(1, health.disabledTools());
        assertEquals(1, health.staleTools());
        assertEquals(3, health.activeTools());
        assertEquals(1, health.serverDisabledTools());
        assertEquals(3, health.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(1, health.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        assertEquals(1, health.issueCounts().get(McpToolServerHealthIssues.ISSUE_STALE_TOOLS));
        assertEquals(1, health.issueCounts().get(McpToolServerHealthIssues.ISSUE_SYNC_ERROR));
        assertEquals(1, health.issueSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(1, health.issueSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.CRITICAL, health.highestIssueSeverity());
        assertEquals(3, health.recommendedActions());
        assertEquals(1, health.automatableActions());
        assertEquals(2, health.manualActions());
        assertEquals(2, health.callableActions());
        assertEquals(1, health.nonCallableActions());
        assertEquals(1, health.actionMethodCounts().get("GET"));
        assertEquals(1, health.actionMethodCounts().get("POST"));
        assertEquals(1, health.actionExecutionModeCounts().get(McpServerActionExecutionMode.AUTOMATABLE));
        assertEquals(1, health.actionExecutionModeCounts().get(McpServerActionExecutionMode.MANUAL));
        assertEquals(1, health.actionExecutionModeCounts().get(McpServerActionExecutionMode.REVIEW_REQUIRED));
        assertEquals(1, health.actionCounts().get("CHECK_ENDPOINT"));
        assertEquals(1, health.actionSeverityCounts().get(McpIssueSeverity.CRITICAL));
        assertEquals(McpIssueSeverity.CRITICAL, health.highestActionSeverity());
        assertEquals(3, health.actionQueueTotal());
        assertEquals(1, health.actionQueueOffset());
        assertEquals(1, health.actionQueueLimit());
        assertEquals(1, health.actionQueueReturned());
        assertEquals(true, health.actionQueueTruncated());
        assertEquals(List.of("docs:REVIEW_STALE_TOOLS"), health.actionQueue().stream()
                .map(McpToolServerHealth.ActionQueueItem::id)
                .toList());
        assertEquals(entries, health.servers());
    }

    @Test
    void emptyTotalsReturnEmptyHealth() {
        McpToolServerHealthTotals totals = new McpToolServerHealthTotals();

        McpToolServerHealth health = totals.toHealth(List.of(), McpServerHealthFilters.byServerName(null));

        assertEquals(0, health.totalServers());
        assertEquals(0, health.actionQueueTotal());
        assertEquals(0, health.actionQueueReturned());
        assertEquals(false, health.actionQueueTruncated());
        assertEquals(List.of(), health.servers());
        assertEquals(List.of(), health.warnings());
    }

    private static McpToolServerHealth.ServerHealth server(
            String serverName,
            boolean enabled,
            String healthStatus,
            boolean attentionRequired,
            int totalTools,
            int enabledTools,
            int disabledTools,
            int staleTools,
            int activeTools,
            int serverDisabledTools,
            int retiredTools,
            Map<String, Integer> lifecycleStates,
            List<McpToolServerHealth.IssueDetail> issueDetails,
            List<McpToolServerHealth.RecommendedAction> actions) {
        return new McpToolServerHealth.ServerHealth(
                serverName,
                "http",
                "http://" + serverName + ".local/mcp",
                enabled,
                null,
                null,
                null,
                false,
                null,
                healthStatus,
                0,
                attentionRequired,
                actions,
                issueDetails.stream().map(McpToolServerHealth.IssueDetail::message).toList(),
                issueDetails.stream().map(McpToolServerHealth.IssueDetail::code).toList(),
                issueDetails,
                McpToolServerHealthIssues.severityCounts(issueDetails),
                McpIssueSeverity.highest(issueDetails),
                null,
                null,
                0,
                0,
                null,
                null,
                null,
                null,
                totalTools,
                enabledTools,
                disabledTools,
                staleTools,
                activeTools,
                serverDisabledTools,
                retiredTools,
                lifecycleStates);
    }

    private static McpToolServerHealth.IssueDetail issue(String code, String severity) {
        return issueDetail(code, severity, code);
    }

    private static McpToolServerHealth.RecommendedAction action(
            String code,
            String severity,
            boolean safeToAutomate,
            McpToolServerHealth.ActionOperation operation) {
        return recommendedAction(
                code,
                severity,
                safeToAutomate,
                code,
                code,
                operation);
    }
}
