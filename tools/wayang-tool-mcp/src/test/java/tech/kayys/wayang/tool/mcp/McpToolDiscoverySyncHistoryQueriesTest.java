package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.history;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.mcpHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncHistoryQueriesTest {

    @Test
    void returnsEmptyViewsWhenRepositoryIsMissing() {
        List<McpToolDiscoverySyncHistoryEntry> history = McpToolDiscoverySyncHistoryQueries.listHistory(
                null,
                "tenant-1",
                null,
                null,
                10)
                .await().atMost(Duration.ofSeconds(3));
        List<McpToolDiscoverySyncHistoryEntry> latest = McpToolDiscoverySyncHistoryQueries.listLatestHistory(
                null,
                "tenant-1",
                null,
                null,
                10)
                .await().atMost(Duration.ofSeconds(3));
        McpToolDiscoverySyncHistorySummary summary = McpToolDiscoverySyncHistoryQueries.summarizeHistory(
                null,
                "tenant-1",
                null,
                null,
                10)
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(history.isEmpty());
        assertTrue(latest.isEmpty());
        assertEquals(0, summary.total());
        assertTrue(summary.servers().isEmpty());
    }

    @Test
    void listHistoryUsesFilteredScanLimitAndHistoryViews() {
        McpRegistrySyncHistoryRepositoryTestDouble repository = new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.add(mcpHistory(
                "tenant-1",
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        repository.add(mcpHistory(
                "tenant-1",
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(1)));
        repository.add(history(
                "tenant-1",
                "OPENAPI",
                "docs",
                "SUCCESS",
                "openapi synced",
                1,
                startedAt.plusSeconds(2)));

        List<McpToolDiscoverySyncHistoryEntry> result = McpToolDiscoverySyncHistoryQueries.listHistory(
                repository,
                "tenant-1",
                "docs",
                "success",
                10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, repository.listByRequestIdCalls());
        assertEquals("tenant-1", repository.lastRequestId());
        assertEquals(40, repository.lastLimit());
        assertEquals(1, result.size());
        assertEquals("docs", result.getFirst().serverName());
        assertEquals("SUCCESS", result.getFirst().status());
        assertEquals(10, result.getFirst().durationMs());
    }

    @Test
    void listLatestHistoryUsesExpandedScanLimitAndNewestPerServer() {
        McpRegistrySyncHistoryRepositoryTestDouble repository = new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.add(mcpHistory(
                "tenant-1",
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        repository.add(mcpHistory(
                "tenant-1",
                "docs",
                "ERROR",
                "docs blocked",
                0,
                startedAt.plusSeconds(2)));
        repository.add(mcpHistory(
                "tenant-1",
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(3)));
        repository.add(history(
                "tenant-1",
                "OPENAPI",
                "docs",
                "SUCCESS",
                "openapi synced",
                1,
                startedAt.plusSeconds(4)));

        List<McpToolDiscoverySyncHistoryEntry> result = McpToolDiscoverySyncHistoryQueries.listLatestHistory(
                repository,
                "tenant-1",
                null,
                null,
                10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, repository.listByRequestIdCalls());
        assertEquals(100, repository.lastLimit());
        assertEquals(2, result.size());
        assertEquals("crm", result.get(0).serverName());
        assertEquals("docs", result.get(1).serverName());
        assertEquals("ERROR", result.get(1).status());
    }

    @Test
    void summarizeHistoryUsesFilteredHistoryEntries() {
        McpRegistrySyncHistoryRepositoryTestDouble repository = new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.add(mcpHistory(
                "tenant-1",
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        repository.add(mcpHistory(
                "tenant-1",
                "docs",
                "ERROR",
                "docs blocked",
                0,
                startedAt.plusSeconds(1)));
        repository.add(mcpHistory(
                "tenant-1",
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(2)));

        McpToolDiscoverySyncHistorySummary result = McpToolDiscoverySyncHistoryQueries.summarizeHistory(
                repository,
                "tenant-1",
                null,
                "error",
                10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(40, repository.lastLimit());
        assertEquals(2, result.total());
        assertEquals(0, result.success());
        assertEquals(2, result.error());
        assertEquals("ERROR", result.latestStatus());
        assertEquals("crm blocked", result.latestMessage());
    }

}
