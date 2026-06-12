package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.historyServiceWithStore;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.newHistoryService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.resourceWithService;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.executedResult;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.record;

class McpServerActionExecutionHistoryResourceTest {

    @Test
    void getAllServerActionExecutionHistoryStatsAggregatesRequests() {
        McpServerActionExecutionHistoryService historyService = newHistoryService();
        record(historyService, "tenant-1", executedResult("docs", Instant.parse("2026-05-31T01:00:00Z")));
        record(historyService, "tenant-1", executedResult("crm", Instant.parse("2026-05-31T01:01:00Z")));
        record(historyService, "tenant-2", executedResult("files", Instant.parse("2026-05-31T01:02:00Z")));
        McpServerActionExecutionHistoryResource resource = resourceWithService(historyService);

        RestResponse<McpServerActionExecutionHistoryStats> response =
                resource.getAllServerActionExecutionHistoryStats()
                        .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().requests());
        assertEquals(3, response.getEntity().entries());
        assertEquals(500, response.getEntity().maxEntriesPerRequest());
        assertEquals(Duration.ofDays(7).toSeconds(), response.getEntity().retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), response.getEntity().oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), response.getEntity().newestEntryAt());
        assertNotNull(response.getEntity().inspectedAt());
    }

    @Test
    void pruneAllExpiredServerActionExecutionsPrunesAcrossRequests() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpServerActionExecutionHistoryService historyService = historyServiceWithStore(
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofMinutes(5), clock));
        record(historyService, "tenant-1", executedResult("docs", Instant.parse("2026-05-31T01:00:00Z")));
        record(historyService, "tenant-1", executedResult("crm", Instant.parse("2026-05-31T01:04:00Z")));
        record(historyService, "tenant-2", executedResult("files", Instant.parse("2026-05-31T01:00:30Z")));
        McpServerActionExecutionHistoryResource resource = resourceWithService(historyService);

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        RestResponse<McpServerActionExecutionHistoryPruneResult> response =
                resource.pruneAllExpiredServerActionExecutions()
                        .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals(2, response.getEntity().pruned());
        assertNotNull(response.getEntity().prunedAt());
        assertEquals(List.of("crm"),
                historyService.list("tenant-1", null, null, null, 10)
                        .await().atMost(Duration.ofSeconds(3))
                        .stream()
                        .map(McpServerActionExecutionHistoryEntry::serverName)
                        .toList());
        assertEquals(List.of(),
                historyService.list("tenant-2", null, null, null, 10)
                        .await().atMost(Duration.ofSeconds(3)));
    }

    @Test
    void publicEndpointsUseFallbackServiceWhenUnconfigured() {
        McpServerActionExecutionHistoryResource resource = new McpServerActionExecutionHistoryResource();

        RestResponse<List<McpServerActionExecutionHistoryEntry>> listResponse =
                resource.listServerActionExecutions(null)
                        .await().atMost(Duration.ofSeconds(3));
        RestResponse<McpServerActionExecutionHistoryPage> pageResponse =
                resource.pageServerActionExecutions(null)
                        .await().atMost(Duration.ofSeconds(3));
        RestResponse<McpServerActionExecutionHistoryStats> statsResponse =
                resource.getAllServerActionExecutionHistoryStats()
                        .await().atMost(Duration.ofSeconds(3));
        RestResponse<McpServerActionExecutionHistoryClearPreview> previewResponse =
                resource.previewClearServerActionExecutions(null)
                        .await().atMost(Duration.ofSeconds(3));
        RestResponse<McpServerActionExecutionHistoryClearResult> clearResponse =
                resource.clearServerActionExecutions(null)
                        .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, listResponse.getStatus());
        assertEquals(List.of(), listResponse.getEntity());
        assertEquals(200, pageResponse.getStatus());
        assertEquals(List.of(), pageResponse.getEntity().entries());
        assertEquals(50, pageResponse.getEntity().limit());
        assertEquals(200, statsResponse.getStatus());
        assertEquals(0, statsResponse.getEntity().requests());
        assertEquals(0, statsResponse.getEntity().entries());
        assertEquals(500, statsResponse.getEntity().maxEntriesPerRequest());
        assertEquals(Duration.ofDays(7).toSeconds(), statsResponse.getEntity().retentionSeconds());
        assertEquals(200, previewResponse.getStatus());
        assertEquals(0, previewResponse.getEntity().matched());
        assertNotNull(previewResponse.getEntity().previewedAt());
        assertEquals(200, clearResponse.getStatus());
        assertEquals(0, clearResponse.getEntity().cleared());
        assertNotNull(clearResponse.getEntity().clearedAt());
    }

}
