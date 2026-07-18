package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Instant;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.mcpHistory;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.successEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncHistoryEntriesTest {

    @Test
    void mapsRegistryHistoryAndCalculatesDuration() {
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        RegistrySyncHistory history = mcpHistory("docs", "SUCCESS", startedAt, startedAt.plusMillis(25));

        McpToolDiscoverySyncHistoryEntry entry = McpToolDiscoverySyncHistoryEntries.toEntry(history);

        assertEquals("docs", entry.serverName());
        assertEquals("SUCCESS", entry.status());
        assertEquals(25, entry.durationMs());
        assertEquals(startedAt, entry.startedAt());
        assertEquals(startedAt.plusMillis(25), entry.finishedAt());
    }

    @Test
    void clampsMissingOrNegativeDurationsToZero() {
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");

        assertEquals(0, McpToolDiscoverySyncHistoryEntries.durationMs(
                mcpHistory("docs", "SUCCESS", startedAt, null)));
        assertEquals(0, McpToolDiscoverySyncHistoryEntries.durationMs(
                mcpHistory("docs", "SUCCESS", startedAt, startedAt.minusMillis(10))));
    }

    @Test
    void ordersLatestEntriesByNewestHistoryTimeThenServerName() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        McpToolDiscoverySyncHistoryEntry docs = successEntry("docs", base, base.plusMillis(10));
        McpToolDiscoverySyncHistoryEntry crm = successEntry("crm", base, base.plusMillis(20));
        McpToolDiscoverySyncHistoryEntry billing = successEntry("billing", base, base.plusMillis(20));

        assertTrue(McpToolDiscoverySyncHistoryEntries.compareLatestEntries(crm, docs) < 0);
        assertTrue(McpToolDiscoverySyncHistoryEntries.compareLatestEntries(billing, crm) < 0);
    }
}
