package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryQueryTestBuilder.query;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.historyServiceWithStore;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.newHistoryService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.record;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.result;

class McpServerActionExecutionHistoryServiceTest {

    @Test
    void listLatestReturnsNewestEntryPerServerAction() {
        McpServerActionExecutionHistoryService service = newHistoryService();
        Instant base = Instant.parse("2026-05-30T05:00:00Z");

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(1)));
        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_FAILED,
                false,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(2)));
        record(service, "tenant-2", result(
                "docs:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "docs",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                base.plusSeconds(3)));

        List<McpServerActionExecutionHistoryEntry> latest = service
                .listLatest("tenant-1", null, null, null, 10)
                .await().indefinitely();

        assertEquals(2, latest.size());
        assertEquals("docs", latest.get(0).serverName());
        assertEquals(McpServerActionExecutionResult.STATUS_FAILED, latest.get(0).status());
        assertEquals("crm", latest.get(1).serverName());
        assertEquals(McpServerActionExecutionResult.STATUS_REJECTED, latest.get(1).status());

        List<McpServerActionExecutionHistoryEntry> secondListPage = service
                .list("tenant-1", query()
                        .withPage(1, 1)
                        .build())
                .await().indefinitely();
        assertEquals(1, secondListPage.size());
        assertEquals("crm", secondListPage.getFirst().serverName());

        McpServerActionExecutionHistoryPage firstPage = service
                .page("tenant-1", query()
                        .withPage(0, 1)
                        .build())
                .await().indefinitely();
        assertEquals(0, firstPage.offset());
        assertEquals(1, firstPage.limit());
        assertEquals(1, firstPage.returned());
        assertEquals(true, firstPage.hasMore());
        assertEquals(1, firstPage.nextOffset());
        assertEquals("docs", firstPage.entries().getFirst().serverName());

        List<McpServerActionExecutionHistoryEntry> secondLatestPage = service
                .listLatest("tenant-1", query()
                        .withPage(1, 1)
                        .build())
                .await().indefinitely();
        assertEquals(1, secondLatestPage.size());
        assertEquals("crm", secondLatestPage.getFirst().serverName());

        McpServerActionExecutionHistoryPage latestTailPage = service
                .pageLatest("tenant-1", query()
                        .withPage(1, 1)
                        .build())
                .await().indefinitely();
        assertEquals(1, latestTailPage.offset());
        assertEquals(1, latestTailPage.returned());
        assertEquals(false, latestTailPage.hasMore());
        assertEquals(null, latestTailPage.nextOffset());
        assertEquals("crm", latestTailPage.entries().getFirst().serverName());

        List<McpServerActionExecutionHistoryEntry> latestSuccessful = service
                .listLatest("tenant-1", "docs", "run-sync", "executed", 10)
                .await().indefinitely();

        assertEquals(1, latestSuccessful.size());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, latestSuccessful.getFirst().status());
        assertEquals(base, latestSuccessful.getFirst().finishedAt());
    }

    @Test
    void summarizeRollsUpRecentEntriesByServerAction() {
        McpServerActionExecutionHistoryService service = newHistoryService();
        Instant base = Instant.parse("2026-05-30T05:00:00Z");

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_FAILED,
                false,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(2),
                List.of("sync warning")));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                base.plusSeconds(1)));

        McpServerActionExecutionHistorySummary summary = service
                .summarize("tenant-1", null, null, null, 10)
                .await().indefinitely();

        assertEquals(3, summary.total());
        assertEquals(1, summary.withWarnings());
        assertEquals(Map.of(
                McpServerActionExecutionMode.AUTOMATABLE, 1,
                McpServerActionExecutionMode.REVIEW_REQUIRED, 2), summary.executionModes());
        assertEquals(Map.of(McpServerActionRiskLevel.LOW, 1, McpServerActionRiskLevel.MEDIUM, 2), summary.riskLevels());
        assertEquals(2, summary.servers().size());
        assertEquals(2, summary.actions().size());

        McpServerActionExecutionHistorySummary.ActionSummary docsRunSync = actionSummary(
                summary,
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC);
        assertEquals("docs", docsRunSync.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, docsRunSync.actionCode());
        assertEquals(2, docsRunSync.total());
        assertEquals(1, docsRunSync.executed());
        assertEquals(1, docsRunSync.failed());
        assertEquals(1, docsRunSync.withWarnings());
        assertEquals(Map.of(
                McpServerActionExecutionMode.AUTOMATABLE, 1,
                McpServerActionExecutionMode.REVIEW_REQUIRED, 1), docsRunSync.executionModes());
        assertEquals(Map.of(McpServerActionRiskLevel.LOW, 1, McpServerActionRiskLevel.MEDIUM, 1), docsRunSync.riskLevels());
        assertEquals(20, docsRunSync.totalDurationMs());
        assertEquals(McpServerActionExecutionResult.STATUS_FAILED, docsRunSync.latestStatus());
        assertEquals(base.plusSeconds(2), docsRunSync.lastFinishedAt());

        McpServerActionExecutionHistorySummary.ActionSummary crmReview = actionSummary(
                summary,
                "crm:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS);
        assertEquals(1, crmReview.total());
        assertEquals(1, crmReview.rejected());
        assertEquals(0, crmReview.withWarnings());
    }

    @Test
    void clearRemovesOnlyCurrentRequestHistory() {
        McpServerActionExecutionHistoryService service = newHistoryService();
        Instant base = Instant.parse("2026-05-30T05:00:00Z");

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(1)));
        record(service, "tenant-2", result(
                "docs:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "docs",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                base.plusSeconds(2)));

        McpServerActionExecutionHistoryQuery rejectedQuery = query()
                .withStatus("rejected")
                .withLimit(10)
                .build();
        McpServerActionExecutionHistoryClearPreview rejectedPreview =
                service.previewClear("tenant-1", rejectedQuery)
                        .await().indefinitely();

        assertEquals(1, rejectedPreview.matched());
        assertEquals(true, rejectedPreview.previewedAt() != null);
        assertEquals(2, service.list("tenant-1", null, null, null, 10)
                .await().indefinitely().size());

        McpServerActionExecutionHistoryClearResult rejectedResult =
                service.clear("tenant-1", rejectedQuery)
                        .await().indefinitely();

        assertEquals(1, rejectedResult.cleared());
        assertEquals(1, service.list("tenant-1", null, null, null, 10)
                .await().indefinitely().size());
        assertEquals(1, service.list("tenant-2", null, null, null, 10)
                .await().indefinitely().size());

        McpServerActionExecutionHistoryClearResult result =
                service.clear("tenant-1").await().indefinitely();

        assertEquals(1, result.cleared());
        assertEquals(true, result.clearedAt() != null);
        assertEquals(0, service.list("tenant-1", null, null, null, 10)
                .await().indefinitely().size());
        assertEquals(1, service.list("tenant-2", null, null, null, 10)
                .await().indefinitely().size());
    }

    @Test
    void pruneExpiredRequestHistoryReturnsPrunedCount() {
        Instant base = Instant.parse("2026-05-30T05:00:00Z");
        MutableClock clock = new MutableClock(base);
        McpServerActionExecutionHistoryService service = historyServiceWithStore(
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofHours(1), clock));

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.minus(Duration.ofMinutes(30))));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));

        clock.advance(Duration.ofMinutes(31));

        McpServerActionExecutionHistoryPruneResult result = service
                .pruneExpired("tenant-1")
                .await().indefinitely();

        List<McpServerActionExecutionHistoryEntry> retained = service
                .list("tenant-1", null, null, null, 10)
                .await().indefinitely();

        assertEquals(1, result.pruned());
        assertEquals(true, result.prunedAt() != null);
        assertEquals(1, retained.size());
        assertEquals("crm", retained.getFirst().serverName());
    }

    @Test
    void pruneExpiredWithoutRequestPrunesExpiredEntriesAcrossRequests() {
        Instant base = Instant.parse("2026-05-30T05:00:00Z");
        MutableClock clock = new MutableClock(base);
        McpServerActionExecutionHistoryService service = historyServiceWithStore(
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofHours(1), clock));

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.minus(Duration.ofMinutes(30))));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-2", result(
                "files:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "files",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                base.minus(Duration.ofMinutes(30))));

        clock.advance(Duration.ofMinutes(31));

        McpServerActionExecutionHistoryPruneResult result = service
                .pruneExpired()
                .await().indefinitely();
        List<McpServerActionExecutionHistoryEntry> tenantOne = service
                .list("tenant-1", null, null, null, 10)
                .await().indefinitely();
        List<McpServerActionExecutionHistoryEntry> tenantTwo = service
                .list("tenant-2", null, null, null, 10)
                .await().indefinitely();

        assertEquals(2, result.pruned());
        assertEquals(true, result.prunedAt() != null);
        assertEquals(1, tenantOne.size());
        assertEquals("crm", tenantOne.getFirst().serverName());
        assertEquals(List.of(), tenantTwo);
    }

    @Test
    void inMemoryStoreUsesConfiguredMaxEntriesAndReportsStats() {
        Instant base = Instant.parse("2026-05-30T05:00:00Z");
        MutableClock clock = new MutableClock(base.plusSeconds(10));
        McpServerActionExecutionHistoryService service = historyServiceWithStore(
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofHours(2), 2, clock));

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(1)));
        record(service, "tenant-1", result(
                "billing:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_FAILED,
                false,
                "billing",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(2)));

        List<McpServerActionExecutionHistoryEntry> retained = service
                .list("tenant-1", null, null, null, 10)
                .await().indefinitely();
        McpServerActionExecutionHistoryStats stats = service
                .stats("tenant-1")
                .await().indefinitely();

        assertEquals(2, retained.size());
        assertEquals("billing", retained.get(0).serverName());
        assertEquals("crm", retained.get(1).serverName());
        assertEquals(1, stats.requests());
        assertEquals(2, stats.entries());
        assertEquals(2, stats.maxEntriesPerRequest());
        assertEquals(Duration.ofHours(2).toSeconds(), stats.retentionSeconds());
        assertEquals(base.plusSeconds(1), stats.oldestEntryAt());
        assertEquals(base.plusSeconds(2), stats.newestEntryAt());
        assertEquals(base.plusSeconds(10), stats.inspectedAt());
    }

    @Test
    void statsWithoutRequestReportsAggregateStorageState() {
        Instant base = Instant.parse("2026-05-30T05:00:00Z");
        MutableClock clock = new MutableClock(base.plusSeconds(10));
        McpServerActionExecutionHistoryService service = historyServiceWithStore(
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofHours(2), 2, clock));

        record(service, "tenant-1", result(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base));
        record(service, "tenant-1", result(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                base.plusSeconds(1)));
        record(service, "tenant-2", result(
                "files:" + McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionExecutionResult.STATUS_REJECTED,
                false,
                "files",
                McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                base.plusSeconds(2)));

        McpServerActionExecutionHistoryStats stats = service
                .stats()
                .await().indefinitely();

        assertEquals(2, stats.requests());
        assertEquals(3, stats.entries());
        assertEquals(2, stats.maxEntriesPerRequest());
        assertEquals(Duration.ofHours(2).toSeconds(), stats.retentionSeconds());
        assertEquals(base, stats.oldestEntryAt());
        assertEquals(base.plusSeconds(2), stats.newestEntryAt());
        assertEquals(base.plusSeconds(10), stats.inspectedAt());
    }

    private static McpServerActionExecutionHistorySummary.ActionSummary actionSummary(
            McpServerActionExecutionHistorySummary summary,
            String actionId) {
        return summary.actions().stream()
                .filter(action -> actionId.equals(action.actionId()))
                .findFirst()
                .orElseThrow();
    }
}
