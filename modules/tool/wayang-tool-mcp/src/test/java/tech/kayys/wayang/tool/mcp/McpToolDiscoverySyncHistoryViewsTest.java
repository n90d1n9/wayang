package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Instant;
import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.mcpHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoverySyncHistoryViewsTest {

    @Test
    void filtersMcpHistoryAndMapsDurations() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<RegistrySyncHistory> histories = List.of(
                mcpHistory("docs", "SUCCESS", "docs synced", 2, base, base.plusMillis(25)),
                mcpHistory("crm", "ERROR", "crm blocked", 0, base, base.plusMillis(50)),
                history(
                        "tenant-1",
                        "OPENAPI",
                        "docs",
                        "SUCCESS",
                        "openapi synced",
                        1,
                        base,
                        base.plusMillis(75)));

        List<McpToolDiscoverySyncHistoryEntry> entries = McpToolDiscoverySyncHistoryViews.entries(
                histories,
                "DOCS",
                "success",
                10);

        assertEquals(1, entries.size());
        assertEquals("docs", entries.getFirst().serverName());
        assertEquals("SUCCESS", entries.getFirst().status());
        assertEquals("docs synced", entries.getFirst().message());
        assertEquals(2, entries.getFirst().itemsAffected());
        assertEquals(25, entries.getFirst().durationMs());
    }

    @Test
    void returnsLatestEntryPerServerNewestFirst() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<RegistrySyncHistory> histories = List.of(
                mcpHistory("docs", "SUCCESS", "docs old", 1, base, base.plusMillis(10)),
                mcpHistory("crm", "ERROR", "crm latest", 0, base, base.plusMillis(40)),
                mcpHistory("docs", "ERROR", "docs latest", 0, base, base.plusMillis(30)));

        List<McpToolDiscoverySyncHistoryEntry> entries = McpToolDiscoverySyncHistoryViews.latestEntries(
                histories,
                null,
                null,
                10);

        assertEquals(2, entries.size());
        assertEquals("crm", entries.get(0).serverName());
        assertEquals("crm latest", entries.get(0).message());
        assertEquals("docs", entries.get(1).serverName());
        assertEquals("docs latest", entries.get(1).message());
    }

    @Test
    void summarizesEntriesForTotalAndEachServer() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<McpToolDiscoverySyncHistoryEntry> entries = McpToolDiscoverySyncHistoryViews.entries(
                List.of(
                        mcpHistory("docs", "SUCCESS", "docs synced", 2, base, base.plusMillis(10)),
                        mcpHistory("docs", "ERROR", "docs blocked", 0, base, base.plusMillis(20)),
                        mcpHistory("crm", "SUCCESS", "crm synced", 4, base, base.plusMillis(30))),
                null,
                null,
                10);

        McpToolDiscoverySyncHistorySummary summary = McpToolDiscoverySyncHistoryViews.summary(entries);

        assertEquals(3, summary.total());
        assertEquals(2, summary.success());
        assertEquals(1, summary.error());
        assertEquals(6, summary.itemsAffected());
        assertEquals(60, summary.totalDurationMs());
        assertEquals("SUCCESS", summary.latestStatus());
        assertEquals("crm synced", summary.latestMessage());
        assertEquals(2, summary.servers().size());
        assertEquals("crm", summary.servers().get(0).serverName());
        assertEquals("docs", summary.servers().get(1).serverName());
        assertEquals(2, summary.servers().get(1).total());
        assertEquals(1, summary.servers().get(1).success());
        assertEquals(1, summary.servers().get(1).error());
    }

    @Test
    void calculatesRepositoryScanLimits() {
        assertEquals(50, McpToolDiscoverySyncHistoryFilters.boundedLimit(0));
        assertEquals(200, McpToolDiscoverySyncHistoryFilters.boundedLimit(1000));
        assertEquals(10, McpToolDiscoverySyncHistoryFilters.listScanLimit(10, null, null));
        assertEquals(40, McpToolDiscoverySyncHistoryFilters.listScanLimit(10, "docs", null));
        assertEquals(500, McpToolDiscoverySyncHistoryFilters.latestScanLimit(200));
    }
}
