package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.append;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.entry;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryResourceTestHarness.query;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryResourceTestHarness.resourceWithHistory;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryResourceTestHarness.resourceWithStore;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryResourceTestHarness.runQuery;

class McpToolCallHistoryResourceTest {

    @Test
    void listToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<List<McpToolCallHistoryEntry>> response = resource.listToolCalls(
                        query("run-1")
                                .withToolId("docs:search")
                                .withStatus(McpToolOutputFields.STATUS_FAILURE)
                                .withSuccess(false)
                                .withFailureType("http")
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs:search", response.getEntity().get(0).toolId());
        assertEquals(McpFailureType.HTTP.name(), response.getEntity().get(0).failureType());
    }

    @Test
    void listToolCallsUsesTimeWindowParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:first", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:second", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "docs:third", true, null, Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<List<McpToolCallHistoryEntry>> response = resource.listToolCalls(
                        query("run-1")
                                .withStartedAtFrom("2026-05-31T01:00:59.970Z")
                                .withFinishedAtTo("2026-05-31T01:02:00Z")
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(List.of("docs:third", "docs:second"),
                response.getEntity().stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void listToolCallsUsesDurationWindowParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:fast", true, null, Instant.parse("2026-05-31T01:00:00Z"),
                        20, 5),
                entry("run-1", "docs:orchestration-slow", true, null,
                        Instant.parse("2026-05-31T01:01:00Z"), 120, 8),
                entry("run-1", "docs:mcp-slow", true, null,
                        Instant.parse("2026-05-31T01:02:00Z"), 140, 80));

        RestResponse<List<McpToolCallHistoryEntry>> response = resource.listToolCalls(
                        query("run-1")
                                .withDurationWindow(100L, 150L)
                                .withMcpDurationWindow(50L, 100L)
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(List.of("docs:mcp-slow"),
                response.getEntity().stream().map(McpToolCallHistoryEntry::toolId).toList());
    }

    @Test
    void clearToolCallsClearsRunHistory() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-2", "crm:search", true, null, Instant.parse("2026-05-31T01:01:00Z")));

        RestResponse<McpToolCallHistoryClearResult> clearResponse = resource.clearToolCalls(
                        runQuery("run-1"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, clearResponse.getStatus());
        assertEquals(1, clearResponse.getEntity().cleared());
        assertEquals(0, resource.listToolCalls(runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3))
                .getEntity()
                .size());
        assertEquals(1, resource.listToolCalls(runQuery("run-2", 10))
                .await().atMost(Duration.ofSeconds(3))
                .getEntity()
                .size());
    }

    @Test
    void pageToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:first", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:second", true, null, Instant.parse("2026-05-31T01:02:00Z")),
                entry("run-1", "docs:third", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-2", "docs:other", true, null, Instant.parse("2026-05-31T01:03:00Z")));

        RestResponse<McpToolCallHistoryPage> response = resource.pageToolCalls(
                        query("run-1")
                                .withPage(1, 1)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().offset());
        assertEquals(1, response.getEntity().limit());
        assertEquals(1, response.getEntity().returned());
        assertEquals(List.of("docs:third"),
                response.getEntity().entries().stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, response.getEntity().hasMore());
        assertEquals(2, response.getEntity().nextOffset());
    }

    @Test
    void listLatestToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "crm:search", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:02:00Z")),
                entry("run-2", "docs:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:03:00Z")));

        RestResponse<List<McpToolCallHistoryEntry>> response = resource.listLatestToolCalls(
                        runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(List.of("docs:search", "crm:search"),
                response.getEntity().stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, response.getEntity().get(0).success());
    }

    @Test
    void pageLatestToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "files:search", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "crm:search", true, null, Instant.parse("2026-05-31T01:02:00Z")),
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:03:00Z")),
                entry("run-2", "docs:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:04:00Z")));

        RestResponse<McpToolCallHistoryPage> response = resource.pageLatestToolCalls(
                        query("run-1")
                                .withPage(1, 1)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().offset());
        assertEquals(1, response.getEntity().limit());
        assertEquals(1, response.getEntity().returned());
        assertEquals(List.of("crm:search"),
                response.getEntity().entries().stream().map(McpToolCallHistoryEntry::toolId).toList());
        assertEquals(true, response.getEntity().hasMore());
        assertEquals(2, response.getEntity().nextOffset());
    }

    @Test
    void summarizeToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<McpToolCallHistorySummary> response = resource.summarizeToolCalls(
                        query("run-1")
                                .withStatus(McpToolOutputFields.STATUS_FAILURE)
                                .withSuccess(false)
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().total());
        assertEquals(0, response.getEntity().succeeded());
        assertEquals(2, response.getEntity().failed());
        assertEquals(1, response.getEntity().byFailureType().get(McpFailureType.HTTP.name()));
        assertEquals(1, response.getEntity().byFailureType().get(McpFailureType.TRANSPORT.name()));
    }

    @Test
    void summarizeToolCallsByToolUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:02:00Z")),
                entry("run-2", "docs:search", true, null, Instant.parse("2026-05-31T01:03:00Z")));

        RestResponse<McpToolCallHistoryToolSummaries> response = resource.summarizeToolCallsByTool(
                        runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().totalTools());
        assertEquals(3, response.getEntity().totalCalls());
        assertNotNull(response.getEntity().summarizedAt());
        assertEquals(List.of("docs:search", "crm:search"),
                response.getEntity().tools().stream().map(McpToolCallHistoryToolSummary::toolId).toList());

        McpToolCallHistoryToolSummary docs = response.getEntity().tools().get(0);
        assertEquals(2, docs.total());
        assertEquals(1, docs.succeeded());
        assertEquals(1, docs.failed());
        assertEquals(McpToolOutputFields.STATUS_SUCCESS, docs.latestStatus());
        assertEquals(true, docs.latestSuccess());
        assertEquals(1, docs.byFailureType().get(McpFailureType.HTTP.name()));
    }

    @Test
    void summarizeToolCallFailuresUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "docs:lookup", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:02:00Z")),
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:03:00Z")),
                entry("run-2", "docs:search", false, McpFailureType.PARSE.name(),
                        Instant.parse("2026-05-31T01:04:00Z")));

        RestResponse<McpToolCallHistoryFailureSummaries> response = resource.summarizeToolCallFailures(
                        runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().totalFailureTypes());
        assertEquals(3, response.getEntity().totalFailures());
        assertNotNull(response.getEntity().summarizedAt());
        assertEquals(List.of(McpFailureType.HTTP.name(), McpFailureType.TRANSPORT.name()),
                response.getEntity().failureTypes().stream()
                        .map(McpToolCallHistoryFailureSummary::failureType)
                        .toList());

        McpToolCallHistoryFailureSummary http = response.getEntity().failureTypes().get(0);
        assertEquals(2, http.total());
        assertEquals("docs:lookup", http.latestToolId());
        assertEquals(1, http.byToolId().get("docs:search"));
        assertEquals(1, http.byToolId().get("docs:lookup"));
    }

    @Test
    void getToolCallHistoryStatsUsesRunId() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "crm:search", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-2", "docs:search", true, null, Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<McpToolCallHistoryStats> response = resource.getToolCallHistoryStats(
                        runQuery("run-1"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().runs());
        assertEquals(2, response.getEntity().entries());
        assertEquals(500, response.getEntity().maxEntriesPerRun());
        assertEquals(Duration.ofDays(7).toSeconds(), response.getEntity().retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), response.getEntity().oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:01:00Z"), response.getEntity().newestEntryAt());
        assertNotNull(response.getEntity().inspectedAt());
    }

    @Test
    void getAllToolCallHistoryStatsAggregatesRuns() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "crm:search", true, null, Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-2", "docs:search", true, null, Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<McpToolCallHistoryStats> response = resource.getAllToolCallHistoryStats()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().runs());
        assertEquals(3, response.getEntity().entries());
        assertEquals(500, response.getEntity().maxEntriesPerRun());
        assertEquals(Duration.ofDays(7).toSeconds(), response.getEntity().retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), response.getEntity().oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), response.getEntity().newestEntryAt());
        assertNotNull(response.getEntity().inspectedAt());
    }

    @Test
    void pruneExpiredToolCallsPrunesRunHistory() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpToolCallHistoryStore store = new InMemoryMcpToolCallHistoryStore(Duration.ofMinutes(5), clock);
        append(store, entry("run-1", "docs:old", true, null, Instant.parse("2026-05-31T01:00:00Z")));
        append(store, entry("run-1", "docs:fresh", true, null, Instant.parse("2026-05-31T01:04:00Z")));
        McpToolCallHistoryResource resource = resourceWithStore(store);

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        RestResponse<McpToolCallHistoryPruneResult> response = resource.pruneExpiredToolCalls(
                        runQuery("run-1"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().pruned());
        assertNotNull(response.getEntity().prunedAt());
        assertEquals(List.of("docs:fresh"),
                resource.listToolCalls(runQuery("run-1", 10))
                        .await().atMost(Duration.ofSeconds(3))
                        .getEntity()
                        .stream()
                        .map(McpToolCallHistoryEntry::toolId)
                        .toList());
    }

    @Test
    void pruneAllExpiredToolCallsPrunesAcrossRuns() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpToolCallHistoryStore store = new InMemoryMcpToolCallHistoryStore(Duration.ofMinutes(5), clock);
        append(store, entry("run-1", "docs:old", true, null, Instant.parse("2026-05-31T01:00:00Z")));
        append(store, entry("run-1", "docs:fresh", true, null, Instant.parse("2026-05-31T01:04:00Z")));
        append(store, entry("run-2", "crm:old", true, null, Instant.parse("2026-05-31T01:00:30Z")));
        McpToolCallHistoryResource resource = resourceWithStore(store);

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        RestResponse<McpToolCallHistoryPruneResult> response = resource.pruneAllExpiredToolCalls()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().pruned());
        assertNotNull(response.getEntity().prunedAt());
        assertEquals(List.of("docs:fresh"),
                resource.listToolCalls(runQuery("run-1", 10))
                        .await().atMost(Duration.ofSeconds(3))
                        .getEntity()
                        .stream()
                        .map(McpToolCallHistoryEntry::toolId)
                        .toList());
        assertEquals(List.of(),
                resource.listToolCalls(runQuery("run-2", 10))
                        .await().atMost(Duration.ofSeconds(3))
                        .getEntity());
    }

    @Test
    void previewClearToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<McpToolCallHistoryClearPreview> response = resource.previewClearToolCalls(
                        query("run-1")
                                .withStatus(McpToolOutputFields.STATUS_FAILURE)
                                .withSuccess(false)
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().matched());
        assertNotNull(response.getEntity().previewedAt());
        assertEquals(3, resource.listToolCalls(runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3))
                .getEntity()
                .size());
    }

    @Test
    void clearToolCallsUsesQueryParameters() {
        McpToolCallHistoryResource resource = resourceWithHistory(
                entry("run-1", "docs:search", true, null, Instant.parse("2026-05-31T01:00:00Z")),
                entry("run-1", "docs:search", false, McpFailureType.HTTP.name(),
                        Instant.parse("2026-05-31T01:01:00Z")),
                entry("run-1", "crm:search", false, McpFailureType.TRANSPORT.name(),
                        Instant.parse("2026-05-31T01:02:00Z")));

        RestResponse<McpToolCallHistoryClearResult> response = resource.clearToolCalls(
                        query("run-1")
                                .withToolId("docs:search")
                                .withStatus(McpToolOutputFields.STATUS_FAILURE)
                                .withSuccess(false)
                                .withFailureType("http")
                                .withLimit(10)
                                .build())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().cleared());
        assertEquals(2, resource.listToolCalls(runQuery("run-1", 10))
                .await().atMost(Duration.ofSeconds(3))
                .getEntity()
                .size());
    }

}
