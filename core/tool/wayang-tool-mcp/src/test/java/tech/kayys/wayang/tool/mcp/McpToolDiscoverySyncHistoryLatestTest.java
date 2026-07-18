package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Instant;
import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.mcpHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoverySyncHistoryLatestTest {

    @Test
    void returnsNewestEntryPerServerNewestFirst() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<RegistrySyncHistory> histories = List.of(
                mcpHistory("docs", "SUCCESS", "docs old", 1, base, base.plusMillis(10)),
                mcpHistory("crm", "ERROR", "crm latest", 1, base, base.plusMillis(40)),
                mcpHistory("docs", "ERROR", "docs latest", 1, base, base.plusMillis(30)),
                mcpHistory("billing", "SUCCESS", "billing synced", 1, base, base.plusMillis(20)));

        List<McpToolDiscoverySyncHistoryEntry> entries = McpToolDiscoverySyncHistoryLatest.entries(
                histories,
                McpToolDiscoverySyncHistoryFilters.latest(null, null, 10));

        assertEquals(3, entries.size());
        assertEquals("crm", entries.get(0).serverName());
        assertEquals("crm latest", entries.get(0).message());
        assertEquals("docs", entries.get(1).serverName());
        assertEquals("docs latest", entries.get(1).message());
        assertEquals("billing", entries.get(2).serverName());
    }

    @Test
    void appliesFiltersAndLimitAfterPerServerSelection() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<RegistrySyncHistory> histories = List.of(
                mcpHistory("docs", "SUCCESS", "docs old", 1, base, base.plusMillis(10)),
                mcpHistory("docs", "ERROR", "docs blocked", 1, base, base.plusMillis(20)),
                mcpHistory("crm", "SUCCESS", "crm synced", 1, base, base.plusMillis(30)),
                mcpHistory("archive", "SUCCESS", "archive synced", 1, base, base.plusMillis(40)));

        List<McpToolDiscoverySyncHistoryEntry> entries = McpToolDiscoverySyncHistoryLatest.entries(
                histories,
                McpToolDiscoverySyncHistoryFilters.latest(null, "success", 1));

        assertEquals(1, entries.size());
        assertEquals("archive", entries.getFirst().serverName());
    }
}
