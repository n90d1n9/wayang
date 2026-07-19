package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.issueDetail;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.lifecycleCounts;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;

class McpToolServerHealthIssuesTest {

    @Test
    void detectsDisabledServerOnly() {
        McpToolServerHealthIssues.Result issues = McpToolServerHealthIssues.forServer(
                server(false),
                null,
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                0);

        assertEquals(List.of("Server is disabled."), issues.messages());
        assertEquals(List.of(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED), issues.codes());
        assertEquals(List.of(issueDetail(
                        McpToolServerHealthIssues.ISSUE_SERVER_DISABLED,
                        McpIssueSeverity.INFO,
                        "Server is disabled.")),
                issues.details());
    }

    @Test
    void detectsUnsyncedEnabledServer() {
        McpToolServerHealthIssues.Result issues = McpToolServerHealthIssues.forServer(
                server(true),
                null,
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                0);

        assertEquals(List.of(McpToolServerHealthIssues.ISSUE_UNSYNCED), issues.codes());
        assertEquals(McpIssueSeverity.WARNING, issues.details().get(0).severity());
    }

    @Test
    void detectsSyncErrorAndConsecutiveFailures() {
        McpToolServerHealthIssues.Result issues = McpToolServerHealthIssues.forServer(
                server(true),
                history("docs", McpToolDiscoverySyncStatuses.ERROR, "blocked"),
                new McpToolLifecycleCounts(),
                new McpToolServerHealthSyncPolicyStatus(null, false, null),
                3);

        assertEquals(List.of(
                        McpToolServerHealthIssues.ISSUE_SYNC_ERROR,
                        McpToolServerHealthIssues.ISSUE_CONSECUTIVE_SYNC_FAILURES),
                issues.codes());
        assertEquals(List.of(
                        "Latest sync failed: blocked",
                        "Server has 3 consecutive sync failures."),
                issues.messages());
        assertEquals(Map.of(McpIssueSeverity.CRITICAL, 2), McpToolServerHealthIssues.severityCounts(issues.details()));
    }

    @Test
    void detectsInvalidScheduleAndLifecycleIssues() {
        McpToolServerHealthIssues.Result issues = McpToolServerHealthIssues.forServer(
                server(true),
                history("docs", McpToolDiscoverySyncStatuses.SUCCESS, "ok"),
                lifecycleCounts("docs"),
                new McpToolServerHealthSyncPolicyStatus(null, false, "invalid interval"),
                0);

        assertEquals(List.of(
                        McpToolServerHealthIssues.ISSUE_INVALID_SYNC_SCHEDULE,
                        McpToolServerHealthIssues.ISSUE_STALE_TOOLS,
                        McpToolServerHealthIssues.ISSUE_SERVER_DISABLED_TOOLS),
                issues.codes());
        assertEquals(Map.of(McpIssueSeverity.WARNING, 3), McpToolServerHealthIssues.severityCounts(issues.details()));
    }

    @Test
    void mapsIssueSeverities() {
        assertEquals(McpIssueSeverity.CRITICAL,
                McpToolServerHealthIssues.severity(McpToolServerHealthIssues.ISSUE_SYNC_ERROR));
        assertEquals(McpIssueSeverity.CRITICAL,
                McpToolServerHealthIssues.severity(McpToolServerHealthIssues.ISSUE_CONSECUTIVE_SYNC_FAILURES));
        assertEquals(McpIssueSeverity.INFO,
                McpToolServerHealthIssues.severity(McpToolServerHealthIssues.ISSUE_SERVER_DISABLED));
        assertEquals(McpIssueSeverity.WARNING,
                McpToolServerHealthIssues.severity(McpToolServerHealthIssues.ISSUE_STALE_TOOLS));
    }
}
