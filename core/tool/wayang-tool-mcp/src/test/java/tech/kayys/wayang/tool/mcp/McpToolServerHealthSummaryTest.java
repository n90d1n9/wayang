package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpServerHealthFiltersTestBuilder.filters;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.server;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthSummaryTest {

    @Test
    void buildsSortedHealthSummaryFromSnapshot() {
        Instant startedAt = Instant.now();
        McpServerRegistry docs = server("docs", "http://docs.local/mcp", true);
        McpServerRegistry crm = server("crm", "http://crm.local/mcp", true);
        McpToolServerHealthSource.Snapshot snapshot = new McpToolServerHealthSource.Snapshot(
                List.of(docs, crm),
                McpToolServerHealthInputs.from(
                        List.of(tool("docs.search", true, Set.of("mcp", "tool", "mcp:docs"), Set.of("docs"))),
                        List.of(history("docs", McpToolDiscoverySyncStatuses.SUCCESS, "docs synced", startedAt)),
                        List.of(history("docs", McpToolDiscoverySyncStatuses.SUCCESS, "docs synced", startedAt)),
                        List.of(),
                        List.of(history("docs", McpToolDiscoverySyncStatuses.SUCCESS, "docs synced", startedAt))));

        McpToolServerHealth health = McpToolServerHealthSummary.from(snapshot, McpServerHealthFilters.byServerName(null));

        assertEquals(2, health.totalServers());
        assertEquals(List.of("crm", "docs"), health.servers().stream()
                .map(McpToolServerHealth.ServerHealth::serverName)
                .toList());
        assertEquals(1, health.unsyncedServers());
        assertEquals(1, health.healthyServers());
        assertEquals(McpServerHealthStatus.UNSYNCED, health.highestHealthStatus());
        assertEquals(1, health.totalTools());
        assertEquals(1, health.actionCounts().get(McpServerActionCatalog.ACTION_RUN_SYNC));
    }

    @Test
    void appliesServerAndHealthFiltersBeforeTotals() {
        Instant startedAt = Instant.now();
        McpServerRegistry docs = server("docs", "http://docs.local/mcp", true);
        McpServerRegistry crm = server("crm", "http://crm.local/mcp", true);
        McpToolDiscoverySyncHistoryEntry crmError =
                history("crm", McpToolDiscoverySyncStatuses.ERROR, "blocked", startedAt);
        McpToolServerHealthSource.Snapshot snapshot = new McpToolServerHealthSource.Snapshot(
                List.of(docs, crm),
                McpToolServerHealthInputs.from(
                        List.of(),
                        List.of(crmError),
                        List.of(),
                        List.of(crmError),
                        List.of(crmError)));

        McpToolServerHealth unhealthy = McpToolServerHealthSummary.from(
                snapshot,
                filters()
                        .withHealthStatus(McpServerHealthStatus.UNHEALTHY)
                        .build());
        McpToolServerHealth docsOnly = McpToolServerHealthSummary.from(
                snapshot,
                McpServerHealthFilters.byServerName("docs"));

        assertEquals(1, unhealthy.totalServers());
        assertEquals("crm", unhealthy.servers().getFirst().serverName());
        assertEquals(1, unhealthy.unhealthyServers());
        assertEquals(1, unhealthy.actionCounts().get(McpServerActionCatalog.ACTION_CHECK_ENDPOINT));
        assertEquals(1, docsOnly.totalServers());
        assertEquals("docs", docsOnly.servers().getFirst().serverName());
        assertEquals(1, docsOnly.unsyncedServers());
        assertEquals(1, docsOnly.actionCounts().get(McpServerActionCatalog.ACTION_RUN_SYNC));
    }

    @Test
    void nullSnapshotReturnsEmptyHealth() {
        McpToolServerHealth health = McpToolServerHealthSummary.from(null, null);

        assertEquals(0, health.totalServers());
        assertEquals(List.of(), health.servers());
        assertEquals(List.of(), health.warnings());
    }
}
