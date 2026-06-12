package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoveryScheduledSyncTest {

    @Test
    void treatsNullCandidatesAsEmptySync() {
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryScheduledSync.sync(
                importService,
                new McpRegistrySyncHistoryRepositoryTestDouble(),
                null)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertTrue(result.warnings().isEmpty());
        assertEquals(0, importService.calls());
    }

    @Test
    void warnsWhenRegistryRepositoryIsMissing() {
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();

        McpToolDiscoverySyncResult result = McpToolDiscoveryScheduledSync.sync(
                null,
                importService,
                historyRepository)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed: MCP server registry is not configured"), result.warnings());
        assertEquals(0, importService.calls());
        assertTrue(historyRepository.items().isEmpty());
    }

    @Test
    void syncsDueServersAndAggregatesSkippedWarnings() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryScheduledSync.sync(
                importService,
                historyRepository,
                List.of(
                        server("tenant-1", "docs", "PT1S", null),
                        server("tenant-1", "bad", "definitely-not-an-interval", null),
                        server("tenant-1", "fresh", "PT1H", Instant.now())))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(2, result.imported());
        assertEquals(1, result.stale());
        assertEquals(1, result.reactivated());
        assertEquals(1, result.warnings().size());
        assertEquals("Live MCP tools sync skipped for server bad: Invalid interval format: "
                        + "definitely-not-an-interval. Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m).",
                result.warnings().getFirst());
        assertEquals(1, importService.calls());
        assertEquals("docs", importService.lastRequest().serverName());
        assertEquals(1, historyRepository.items().size());
        assertEquals("SUCCESS", historyRepository.items().getFirst().getStatus());
    }

    @Test
    void recordsDueServerImportFailuresAsWarnings() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(McpToolDiscoveryImportResult.failure(
                        "docs",
                        "docs",
                        "blocked",
                        Map.of()));

        McpToolDiscoverySyncResult result = McpToolDiscoveryScheduledSync.sync(
                importService,
                historyRepository,
                List.of(server("tenant-1", "docs", "PT1S", null)))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: blocked"), result.warnings());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("blocked", historyRepository.items().getFirst().getMessage());
    }

    private static McpToolDiscoveryImportResult successResult() {
        return McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                3,
                List.of("docs:search", "docs:read"),
                List.of("docs:old"),
                List.of("docs:read"),
                Map.of());
    }

    private static McpServerRegistry server(
            String requestId,
            String name,
            String schedule,
            Instant lastSyncAt) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport(McpServerTransports.HTTP);
        server.setUrl("http://" + name + ".local/mcp");
        server.setEnabled(true);
        server.setSyncSchedule(schedule);
        server.setLastSyncAt(lastSyncAt);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());
        return server;
    }

}
