package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoverySyncHistorySummariesTest {

    @Test
    void aggregatesTotalsAndServerSummaries() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<McpToolDiscoverySyncHistoryEntry> entries = List.of(
                entry("docs", "SUCCESS", "docs synced", 2, 10, base, base.plusMillis(10)),
                entry("docs", "ERROR", "docs blocked", 0, 20, base, base.plusMillis(20)),
                entry("crm", "SUCCESS", "crm synced", 4, 30, base, base.plusMillis(30)));

        McpToolDiscoverySyncHistorySummary summary = McpToolDiscoverySyncHistorySummaries.summary(entries);

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
}
