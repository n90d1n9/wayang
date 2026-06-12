package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.lifecycleCounts;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;

class McpToolServerHealthStatusAssessmentTest {

    @Test
    void disabledServerStatusWins() {
        assertEquals(
                McpServerHealthStatus.DISABLED,
                McpToolServerHealthStatusAssessment.status(
                        server(false),
                        history("docs", McpToolDiscoverySyncStatuses.ERROR),
                        lifecycleCounts("docs", true, true),
                        new McpToolServerHealthSyncPolicyStatus(null, true, "invalid")));
    }

    @Test
    void missingHistoryIsUnsyncedForEnabledServer() {
        assertEquals(
                McpServerHealthStatus.UNSYNCED,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        null,
                        new McpToolLifecycleCounts(),
                        new McpToolServerHealthSyncPolicyStatus(null, false, null)));
    }

    @Test
    void latestSyncErrorIsUnhealthy() {
        assertEquals(
                McpServerHealthStatus.UNHEALTHY,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        history("docs", McpToolDiscoverySyncStatuses.ERROR),
                        new McpToolLifecycleCounts(),
                        new McpToolServerHealthSyncPolicyStatus(null, false, null)));
    }

    @Test
    void syncPolicyErrorOrToolIssuesAreDegraded() {
        assertEquals(
                McpServerHealthStatus.DEGRADED,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        history("docs", McpToolDiscoverySyncStatuses.SUCCESS),
                        new McpToolLifecycleCounts(),
                        new McpToolServerHealthSyncPolicyStatus(null, false, "invalid")));
        assertEquals(
                McpServerHealthStatus.DEGRADED,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        history("docs", McpToolDiscoverySyncStatuses.SUCCESS),
                        lifecycleCounts("docs", true, false),
                        new McpToolServerHealthSyncPolicyStatus(null, false, null)));
        assertEquals(
                McpServerHealthStatus.DEGRADED,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        history("docs", McpToolDiscoverySyncStatuses.SUCCESS),
                        lifecycleCounts("docs", false, true),
                        new McpToolServerHealthSyncPolicyStatus(null, false, null)));
    }

    @Test
    void successfulSyncWithoutIssuesIsHealthy() {
        assertEquals(
                McpServerHealthStatus.HEALTHY,
                McpToolServerHealthStatusAssessment.status(
                        server(true),
                        history("docs", McpToolDiscoverySyncStatuses.SUCCESS),
                        new McpToolLifecycleCounts(),
                        new McpToolServerHealthSyncPolicyStatus(null, false, null)));
    }

    @Test
    void attentionRequiredComesFromHealthStatusOrIssueSeverity() {
        assertEquals(false, McpToolServerHealthStatusAssessment.attentionRequired(
                McpServerHealthStatus.DISABLED,
                McpIssueSeverity.INFO));
        assertEquals(false, McpToolServerHealthStatusAssessment.attentionRequired(
                McpServerHealthStatus.HEALTHY,
                McpIssueSeverity.INFO));
        assertEquals(true, McpToolServerHealthStatusAssessment.attentionRequired(
                McpServerHealthStatus.UNSYNCED,
                null));
        assertEquals(true, McpToolServerHealthStatusAssessment.attentionRequired(
                McpServerHealthStatus.HEALTHY,
                McpIssueSeverity.WARNING));
    }
}
