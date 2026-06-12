package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.entry;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryQueryTestBuilder.query;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryServiceTestHarness.historyWithMaxEntries;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryServiceTestHarness.historyWithRetention;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryServiceTestHarness.newHistory;

class McpToolCallHistoryServiceTest {

    @Test
    void listFiltersByToolStatusSuccessAndFailureType() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                "ok",
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:02:00Z")));

        List<McpToolCallHistoryEntry> filtered = history.service()
                .list("run-1", "docs:search", "FAILURE", false, "http", 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, filtered.size());
        McpToolCallHistoryEntry entry = filtered.get(0);
        assertFalse(entry.success());
        assertEquals("docs:search", entry.toolId());
        assertEquals(McpFailureType.HTTP.name(), entry.failureType());
        assertEquals("server unavailable", entry.error());
    }

    @Test
    void listReturnsNewestEntriesFirstAndAppliesLimit() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:second",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));
        history.append(entry(
                "run-1",
                "docs:third",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));

        List<McpToolCallHistoryEntry> latest = history.service()
                .list("run-1", null, null, null, null, 2)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:second", "docs:third"),
                latest.stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void listFiltersByStartedAndFinishedTimeWindows() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:second",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "docs:third",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));

        List<McpToolCallHistoryEntry> entries = history.service()
                .list("run-1", query()
                        .withStartedAtFrom("2026-05-31T01:00:59.970Z")
                        .withFinishedAtTo("2026-05-31T01:02:00Z")
                        .withLimit(10)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:third", "docs:second"),
                entries.stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void listFiltersByDurationAndMcpDurationWindows() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:fast",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z"),
                20,
                5));
        history.append(entry(
                "run-1",
                "docs:orchestration-slow",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z"),
                120,
                8));
        history.append(entry(
                "run-1",
                "docs:mcp-slow",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z"),
                140,
                80));

        List<McpToolCallHistoryEntry> entries = history.service()
                .list("run-1", query()
                        .withDurationWindow(100L, 150L)
                        .withMcpDurationWindow(50L, 100L)
                        .withLimit(10)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:mcp-slow"),
                entries.stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void pageReturnsOffsetLimitAndNextOffset() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:second",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));
        history.append(entry(
                "run-1",
                "docs:third",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));

        McpToolCallHistoryPage page = history.service()
                .page("run-1", query()
                        .withPage(1, 1)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, page.offset());
        assertEquals(1, page.limit());
        assertEquals(1, page.returned());
        assertEquals(List.of("docs:third"),
                page.entries().stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, page.hasMore());
        assertEquals(2, page.nextOffset());
    }

    @Test
    void latestReturnsNewestEntryPerTool() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));

        List<McpToolCallHistoryEntry> latest = history.service()
                .latest("run-1", query()
                        .withLimit(0)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:search", "crm:search"),
                latest.stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, latest.get(0).success());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), latest.get(0).finishedAt());
    }

    @Test
    void latestPageReturnsMetadataForNewestEntryPerTool() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "files:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:03:00Z")));

        McpToolCallHistoryPage page = history.service()
                .latestPage("run-1", query()
                        .withPage(1, 1)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, page.offset());
        assertEquals(1, page.limit());
        assertEquals(1, page.returned());
        assertEquals(List.of("crm:search"),
                page.entries().stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, page.hasMore());
        assertEquals(2, page.nextOffset());
    }

    @Test
    void summarizeCountsStatusFailureTypeAndToolUsage() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:02:00Z")));

        McpToolCallHistorySummary summary = history.service()
                .summarize("run-1", null, null, null, null, 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(3, summary.total());
        assertEquals(1, summary.succeeded());
        assertEquals(2, summary.failed());
        assertEquals(25, summary.averageDurationMs());
        assertEquals(5, summary.averageMcpDurationMs());
        assertEquals(1, summary.byStatus().get(McpToolOutputFields.STATUS_SUCCESS));
        assertEquals(2, summary.byStatus().get(McpToolOutputFields.STATUS_FAILURE));
        assertEquals(1, summary.byFailureType().get(McpFailureType.HTTP.name()));
        assertEquals(1, summary.byFailureType().get(McpFailureType.TRANSPORT.name()));
        assertEquals(2, summary.byToolId().get("docs:search"));
        assertEquals(1, summary.byToolId().get("crm:search"));
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), summary.oldestFinishedAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), summary.newestFinishedAt());
    }

    @Test
    void summarizeToolsRollsUpCountsAndLatestStatusPerTool() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));

        McpToolCallHistoryToolSummaries summaries = history.service()
                .summarizeTools("run-1", query()
                        .withLimit(10)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, summaries.totalTools());
        assertEquals(3, summaries.totalCalls());
        assertNotNull(summaries.summarizedAt());
        assertEquals(List.of("docs:search", "crm:search"),
                summaries.tools().stream().map(McpToolCallHistoryToolSummary::toolId).toList());

        McpToolCallHistoryToolSummary docs = summaries.tools().get(0);
        assertEquals(2, docs.total());
        assertEquals(1, docs.succeeded());
        assertEquals(1, docs.failed());
        assertEquals(25, docs.averageDurationMs());
        assertEquals(5, docs.averageMcpDurationMs());
        assertEquals(McpToolOutputFields.STATUS_SUCCESS, docs.latestStatus());
        assertEquals(true, docs.latestSuccess());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), docs.oldestFinishedAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), docs.newestFinishedAt());
        assertEquals(1, docs.byStatus().get(McpToolOutputFields.STATUS_SUCCESS));
        assertEquals(1, docs.byStatus().get(McpToolOutputFields.STATUS_FAILURE));
        assertEquals(1, docs.byFailureType().get(McpFailureType.HTTP.name()));
    }

    @Test
    void summarizeFailuresRollsUpFailureTypesAndAffectedTools() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "gateway timeout",
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "docs:lookup",
                false,
                McpFailureType.HTTP.name(),
                "bad gateway",
                Instant.parse("2026-05-31T01:02:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:03:00Z")));

        McpToolCallHistoryFailureSummaries summaries = history.service()
                .summarizeFailures("run-1", query()
                        .withLimit(10)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, summaries.totalFailureTypes());
        assertEquals(3, summaries.totalFailures());
        assertNotNull(summaries.summarizedAt());
        assertEquals(List.of(McpFailureType.HTTP.name(), McpFailureType.TRANSPORT.name()),
                summaries.failureTypes().stream().map(McpToolCallHistoryFailureSummary::failureType).toList());

        McpToolCallHistoryFailureSummary http = summaries.failureTypes().get(0);
        assertEquals(2, http.total());
        assertEquals("docs:lookup", http.latestToolId());
        assertEquals("bad gateway", http.latestError());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), http.oldestFinishedAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), http.newestFinishedAt());
        assertEquals(1, http.byToolId().get("docs:search"));
        assertEquals(1, http.byToolId().get("docs:lookup"));
    }

    @Test
    void statsReportsRetainedRunStorageState() {
        McpToolCallHistoryServiceTestHarness.History history = historyWithMaxEntries(2);
        history.append(entry(
                "run-1",
                "docs:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:second",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "docs:third",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));

        McpToolCallHistoryStats stats = history.service()
                .stats("run-1")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, stats.runs());
        assertEquals(2, stats.entries());
        assertEquals(2, stats.maxEntriesPerRun());
        assertEquals(Duration.ofDays(7).toSeconds(), stats.retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:01:00Z"), stats.oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), stats.newestEntryAt());
        assertNotNull(stats.inspectedAt());
    }

    @Test
    void statsWithoutRunReportsAggregateStorageState() {
        McpToolCallHistoryServiceTestHarness.History history = historyWithMaxEntries(2);
        history.append(entry(
                "run-1",
                "docs:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:second",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-2",
                "crm:first",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:02:00Z")));

        McpToolCallHistoryStats stats = history.service()
                .stats()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, stats.runs());
        assertEquals(3, stats.entries());
        assertEquals(2, stats.maxEntriesPerRun());
        assertEquals(Duration.ofDays(7).toSeconds(), stats.retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), stats.oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), stats.newestEntryAt());
        assertNotNull(stats.inspectedAt());
    }

    @Test
    void pruneExpiredRemovesOnlyExpiredEntries() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpToolCallHistoryServiceTestHarness.History history =
                historyWithRetention(Duration.ofMinutes(5), clock);
        history.append(entry(
                "run-1",
                "docs:old",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:fresh",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:04:00Z")));

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        McpToolCallHistoryPruneResult result = history.service().pruneExpired("run-1")
                .await().atMost(Duration.ofSeconds(3));
        McpToolCallHistoryStats stats = history.service().stats("run-1")
                .await().atMost(Duration.ofSeconds(3));
        List<McpToolCallHistoryEntry> remaining = history.service().list("run-1")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(Duration.ofMinutes(5).toSeconds(), stats.retentionSeconds());
        assertEquals(List.of("docs:fresh"),
                remaining.stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void pruneExpiredWithoutRunRemovesExpiredEntriesAcrossRuns() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpToolCallHistoryServiceTestHarness.History history =
                historyWithRetention(Duration.ofMinutes(5), clock);
        history.append(entry(
                "run-1",
                "docs:old",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:fresh",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:04:00Z")));
        history.append(entry(
                "run-2",
                "crm:old",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:30Z")));

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        McpToolCallHistoryPruneResult result = history.service().pruneExpired()
                .await().atMost(Duration.ofSeconds(3));
        List<McpToolCallHistoryEntry> runOne = history.service().list("run-1")
                .await().atMost(Duration.ofSeconds(3));
        List<McpToolCallHistoryEntry> runTwo = history.service().list("run-2")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(List.of("docs:fresh"),
                runOne.stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(List.of(), runTwo);
    }

    @Test
    void previewClearCountsMatchesWithoutMutatingHistory() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:02:00Z")));

        McpToolCallHistoryClearPreview preview = history.service()
                .previewClear("run-1", query()
                        .withStatus(McpToolOutputFields.STATUS_FAILURE)
                        .withSuccess(false)
                        .withLimit(0)
                        .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, preview.matched());
        assertNotNull(preview.previewedAt());
        assertEquals(3, history.service().list("run-1").await().atMost(Duration.ofSeconds(3)).size());
    }

    @Test
    void clearRemovesOnlyMatchingEntries() {
        McpToolCallHistoryServiceTestHarness.History history = newHistory();
        history.append(entry(
                "run-1",
                "docs:search",
                true,
                null,
                null,
                Instant.parse("2026-05-31T01:00:00Z")));
        history.append(entry(
                "run-1",
                "docs:search",
                false,
                McpFailureType.HTTP.name(),
                "server unavailable",
                Instant.parse("2026-05-31T01:01:00Z")));
        history.append(entry(
                "run-1",
                "crm:search",
                false,
                McpFailureType.TRANSPORT.name(),
                "connection reset",
                Instant.parse("2026-05-31T01:02:00Z")));

        int cleared = history.service().clear("run-1", query()
                        .withToolId("docs:search")
                        .withStatus(McpToolOutputFields.STATUS_FAILURE)
                        .withSuccess(false)
                        .withFailureType("http")
                        .withLimit(0)
                        .build())
                .await().atMost(Duration.ofSeconds(3));
        List<McpToolCallHistoryEntry> remaining = history.service().list("run-1")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, cleared);
        assertEquals(2, remaining.size());
        assertEquals(List.of("crm:search", "docs:search"),
                remaining.stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

}
