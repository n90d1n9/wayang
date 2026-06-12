package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolServerHealthSyncHistoryTest {

    @Test
    void indexesLatestAndRecentHistoryByNormalizedServerName() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        McpToolDiscoverySyncHistoryEntry docs = entry(
                "DOCS",
                "SUCCESS",
                "docs synced",
                1,
                10,
                base,
                base.plusMillis(10));
        McpToolDiscoverySyncHistoryEntry docsLater = entry(
                "docs",
                "ERROR",
                "docs blocked",
                0,
                10,
                base.plusSeconds(1),
                base.plusSeconds(1).plusMillis(10));
        McpToolDiscoverySyncHistoryEntry crm = entry(
                "crm",
                "SUCCESS",
                "crm synced",
                1,
                10,
                base.plusSeconds(2),
                base.plusSeconds(2).plusMillis(10));

        Map<String, McpToolDiscoverySyncHistoryEntry> byServer =
                McpToolServerHealthSyncHistory.byServer(List.of(docs, docsLater, crm));
        Map<String, List<McpToolDiscoverySyncHistoryEntry>> listByServer =
                McpToolServerHealthSyncHistory.listByServer(List.of(docs, docsLater, crm));

        assertEquals(docsLater, byServer.get("docs"));
        assertEquals(crm, byServer.get("crm"));
        assertEquals(List.of(docs, docsLater), listByServer.get("docs"));
    }

    @Test
    void countsConsecutiveFailuresFromNewestHistoryFirst() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        List<McpToolDiscoverySyncHistoryEntry> entries = List.of(
                entry("docs", "ERROR", "old failure", 0, 10, base, base.plusMillis(10)),
                entry(
                        "docs",
                        "ERROR",
                        "latest failure",
                        0,
                        10,
                        base.plusSeconds(3),
                        base.plusSeconds(3).plusMillis(10)),
                entry(
                        "docs",
                        "ERROR",
                        "second latest failure",
                        0,
                        10,
                        base.plusSeconds(2),
                        base.plusSeconds(2).plusMillis(10)),
                entry(
                        "docs",
                        "SUCCESS",
                        "previous success",
                        1,
                        10,
                        base.plusSeconds(1),
                        base.plusSeconds(1).plusMillis(10)));

        assertEquals(2, McpToolServerHealthSyncHistory.consecutiveFailures(entries));
    }

    @Test
    void usesFinishedAtAsHistoryTimeWhenPresent() {
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant finishedAt = startedAt.plusSeconds(1);

        assertEquals(finishedAt, McpToolServerHealthSyncHistory.historyTime(entry(
                "docs",
                "SUCCESS",
                "docs synced",
                1,
                10,
                startedAt,
                finishedAt)));
        assertEquals(startedAt, McpToolServerHealthSyncHistory.historyTime(entry(
                "docs",
                "SUCCESS",
                "docs synced",
                1,
                0,
                startedAt,
                null)));
    }
}
