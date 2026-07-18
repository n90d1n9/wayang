package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.operation;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.recommendedAction;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthEntriesTest {

    @Test
    void unsyncedServerEntryIncludesWarningAndInitialSyncAction() {
        McpToolServerHealth.ServerHealth health = McpToolServerHealthEntries.from(
                server("docs", "http", "http://docs.local/mcp", true),
                new McpToolServerHealthInputs.ServerInput(
                        new McpToolLifecycleCounts(),
                        null,
                        null,
                        null,
                        List.of()));

        assertEquals("docs", health.serverName());
        assertEquals("http://docs.local/mcp", health.endpoint());
        assertEquals(McpServerHealthStatus.UNSYNCED, health.healthStatus());
        assertEquals(true, health.attentionRequired());
        assertEquals(List.of("Server has no MCP sync history."), health.issues());
        assertEquals(List.of(McpToolServerHealthIssues.ISSUE_UNSYNCED), health.issueCodes());
        assertEquals(1, health.issueSeverityCounts().get(McpIssueSeverity.WARNING));
        assertEquals(McpIssueSeverity.WARNING, health.highestIssueSeverity());
        assertEquals(List.of(recommendedAction(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        McpIssueSeverity.WARNING,
                        true,
                        "Run MCP discovery sync to initialize this server.",
                        "POST /mcp/tools/discover/sync/docs",
                        operation("POST", "/mcp/tools/discover/sync/docs"))),
                health.recommendedActions());
        assertEquals(0, health.totalTools());
    }

    @Test
    void errorEntryCarriesLatestHistoryAndConsecutiveFailures() {
        Instant startedAt = Instant.now();
        McpToolDiscoverySyncHistoryEntry latest = history("crm", McpToolDiscoverySyncStatuses.ERROR, "blocked",
                startedAt);
        McpToolDiscoverySyncHistoryEntry previousError = history("crm", McpToolDiscoverySyncStatuses.ERROR,
                "still blocked", startedAt.minusSeconds(5));
        McpToolLifecycleCounts counts = new McpToolLifecycleCounts();
        counts.add(tool("crm.lookup", true, Set.of("mcp", "tool", "mcp:crm"), Set.of("crm")));

        McpToolServerHealth.ServerHealth health = McpToolServerHealthEntries.from(
                server("crm", "http", "http://crm.local/mcp", true),
                new McpToolServerHealthInputs.ServerInput(
                        counts,
                        latest,
                        null,
                        latest,
                        List.of(latest, previousError)));

        assertEquals(McpServerHealthStatus.UNHEALTHY, health.healthStatus());
        assertEquals(true, health.attentionRequired());
        assertEquals(2, health.consecutiveFailures());
        assertEquals(McpToolDiscoverySyncStatuses.ERROR, health.latestSyncStatus());
        assertEquals("blocked", health.latestSyncMessage());
        assertEquals(startedAt, health.latestStartedAt());
        assertEquals(startedAt.plusMillis(10), health.latestFinishedAt());
        assertEquals(startedAt.plusMillis(10), health.lastErrorAt());
        assertEquals(List.of("Latest sync failed: blocked", "Server has 2 consecutive sync failures."),
                health.issues());
        assertEquals(List.of(
                        McpToolServerHealthIssues.ISSUE_SYNC_ERROR,
                        McpToolServerHealthIssues.ISSUE_CONSECUTIVE_SYNC_FAILURES),
                health.issueCodes());
        assertEquals(McpIssueSeverity.CRITICAL, health.highestIssueSeverity());
        assertEquals(List.of(recommendedAction(
                        McpServerActionCatalog.ACTION_CHECK_ENDPOINT,
                        McpIssueSeverity.CRITICAL,
                        false,
                        "Check MCP endpoint, credentials, and transport logs.",
                        "Inspect http://crm.local/mcp")),
                health.recommendedActions());
        assertEquals(1, health.totalTools());
        assertEquals(1, health.activeTools());
    }
}
