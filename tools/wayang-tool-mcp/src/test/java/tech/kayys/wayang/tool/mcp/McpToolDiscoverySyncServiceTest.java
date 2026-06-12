package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncServiceTest {

    @Test
    void importsDueScheduledServersAndRecordsHistory() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1S", null));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                2,
                List.of("docs:search", "docs:read"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(2, result.imported());
        assertTrue(result.warnings().isEmpty());
        assertEquals("tenant-1", importService.lastRequestId());
        assertEquals("docs", importService.lastRequest().serverName());
        assertEquals(1, historyRepository.items().size());
        assertEquals(McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                historyRepository.items().getFirst().getSourceKind());
        assertEquals("SUCCESS", historyRepository.items().getFirst().getStatus());
        assertEquals("Live MCP tools synced (imported=2, stale=0, reactivated=0)",
                historyRepository.items().getFirst().getMessage());
        assertEquals(2, historyRepository.items().getFirst().getItemsAffected());
    }

    @Test
    void recordsStaleAndReactivatedCountsFromScheduledImport() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1S", null));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                2,
                List.of("docs:search", "docs:read"),
                List.of("docs:old"),
                List.of("docs:read"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(2, result.imported());
        assertEquals(1, result.stale());
        assertEquals(1, result.reactivated());
        assertEquals(1, historyRepository.items().size());
        assertEquals("Live MCP tools synced (imported=2, stale=1, reactivated=1)",
                historyRepository.items().getFirst().getMessage());
        assertEquals(3, historyRepository.items().getFirst().getItemsAffected());
    }

    @Test
    void syncsRegisteredServerImmediatelyAndRecordsHistory() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1H", Instant.now()));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                2,
                List.of("docs:search", "docs:read"),
                List.of("docs:old"),
                List.of("docs:read"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncRegisteredServer("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(2, result.imported());
        assertEquals(1, result.stale());
        assertEquals(1, result.reactivated());
        assertTrue(result.warnings().isEmpty());
        assertEquals(1, importService.calls());
        assertEquals("tenant-1", importService.lastRequestId());
        assertEquals("docs", importService.lastRequest().serverName());
        assertEquals(1, historyRepository.items().size());
        assertEquals("SUCCESS", historyRepository.items().getFirst().getStatus());
        assertEquals("Live MCP tools synced (imported=2, stale=1, reactivated=1)",
                historyRepository.items().getFirst().getMessage());
    }

    @Test
    void syncRegisteredServerWarnsWhenServerIsMissing() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                1,
                List.of("docs:search"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncRegisteredServer("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: MCP server was not found"),
                result.warnings());
        assertEquals(0, importService.calls());
        assertTrue(historyRepository.items().isEmpty());
    }

    @Test
    void syncRegisteredServerRecordsDisabledServerFailure() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpServerRegistry disabled = server("tenant-1", "docs", "PT1S", null);
        disabled.setEnabled(false);
        serverRepository.add(disabled);
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                1,
                List.of("docs:search"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncRegisteredServer("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: MCP server `docs` is disabled"),
                result.warnings());
        assertEquals(0, importService.calls());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("MCP server `docs` is disabled", historyRepository.items().getFirst().getMessage());
    }

    @Test
    void listHistoryFiltersMcpToolSyncHistoryByServerAndStatus() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.now();
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(1)));
        historyRepository.add(history(
                "tenant-1",
                "OPENAPI",
                "docs",
                "SUCCESS",
                "openapi synced",
                1,
                startedAt.plusSeconds(2)));

        List<McpToolDiscoverySyncHistoryEntry> result = service(
                new McpServerRegistryRepositoryTestDouble(),
                historyRepository,
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        1,
                        List.of("docs:search"),
                        Map.of())),
                true)
                .listHistory("tenant-1", "docs", "success", 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.size());
        assertEquals("docs", result.getFirst().serverName());
        assertEquals("SUCCESS", result.getFirst().status());
        assertEquals("docs synced", result.getFirst().message());
        assertEquals(2, result.getFirst().itemsAffected());
        assertEquals(10, result.getFirst().durationMs());
    }

    @Test
    void summarizeHistoryAggregatesRecentMcpToolHistoryByServer() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.now();
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "ERROR",
                "docs blocked",
                0,
                startedAt.plusSeconds(1)));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "crm",
                "SUCCESS",
                "crm synced",
                4,
                startedAt.plusSeconds(2)));
        historyRepository.add(history(
                "tenant-1",
                "OPENAPI",
                "docs",
                "SUCCESS",
                "openapi synced",
                1,
                startedAt.plusSeconds(3)));

        McpToolDiscoverySyncHistorySummary result = service(
                new McpServerRegistryRepositoryTestDouble(),
                historyRepository,
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        1,
                        List.of("docs:search"),
                        Map.of())),
                true)
                .summarizeHistory("tenant-1", null, 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(3, result.total());
        assertEquals(2, result.success());
        assertEquals(1, result.error());
        assertEquals(6, result.itemsAffected());
        assertEquals(30, result.totalDurationMs());
        assertEquals("SUCCESS", result.latestStatus());
        assertEquals("crm synced", result.latestMessage());
        assertEquals(startedAt.plusSeconds(2), result.lastStartedAt());
        assertEquals(2, result.servers().size());

        McpToolDiscoverySyncHistorySummary.ServerSummary docs = result.servers().stream()
                .filter(server -> "docs".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, docs.total());
        assertEquals(1, docs.success());
        assertEquals(1, docs.error());
        assertEquals(2, docs.itemsAffected());
        assertEquals("ERROR", docs.latestStatus());
        assertEquals("docs blocked", docs.latestMessage());
    }

    @Test
    void summarizeHistoryCanFilterByStatus() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.now();
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "ERROR",
                "docs blocked",
                0,
                startedAt.plusSeconds(1)));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(2)));

        McpToolDiscoverySyncHistorySummary result = service(
                new McpServerRegistryRepositoryTestDouble(),
                historyRepository,
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        1,
                        List.of("docs:search"),
                        Map.of())),
                true)
                .summarizeHistory("tenant-1", null, "error", 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.total());
        assertEquals(0, result.success());
        assertEquals(2, result.error());
        assertEquals(2, result.servers().size());
        assertEquals("ERROR", result.latestStatus());
        assertEquals("crm blocked", result.latestMessage());
    }

    @Test
    void listLatestHistoryReturnsNewestEntryPerServer() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.now();
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS",
                "docs synced",
                2,
                startedAt));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "crm",
                "ERROR",
                "crm blocked",
                0,
                startedAt.plusSeconds(3)));
        historyRepository.add(history(
                "tenant-1",
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "ERROR",
                "docs blocked",
                0,
                startedAt.plusSeconds(2)));
        historyRepository.add(history(
                "tenant-1",
                "OPENAPI",
                "docs",
                "SUCCESS",
                "openapi synced",
                1,
                startedAt.plusSeconds(4)));

        List<McpToolDiscoverySyncHistoryEntry> result = service(
                new McpServerRegistryRepositoryTestDouble(),
                historyRepository,
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        1,
                        List.of("docs:search"),
                        Map.of())),
                true)
                .listLatestHistory("tenant-1", null, null, 10)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.size());
        assertEquals("crm", result.get(0).serverName());
        assertEquals("ERROR", result.get(0).status());
        assertEquals("crm blocked", result.get(0).message());
        assertEquals("docs", result.get(1).serverName());
        assertEquals("ERROR", result.get(1).status());
        assertEquals("docs blocked", result.get(1).message());
    }

    @Test
    void skipsServersThatAreNotDueYet() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1H", Instant.now()));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                1,
                List.of("docs:search"),
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(0, importService.calls());
        assertTrue(historyRepository.items().isEmpty());
    }

    @Test
    void recordsImportFailuresAsWarningsAndHistory() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1S", null));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.failure(
                "docs",
                "docs",
                "blocked",
                Map.of()));

        McpToolDiscoverySyncResult result = service(serverRepository, historyRepository, importService, true)
                .syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: blocked"), result.warnings());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("blocked", historyRepository.items().getFirst().getMessage());
    }

    @Test
    void skipsWhenMcpRegistryDatabaseModeIsUnavailable() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs", "PT1S", null));

        McpToolDiscoverySyncResult result = service(
                serverRepository,
                new McpRegistrySyncHistoryRepositoryTestDouble(),
                new McpToolDiscoveryImportServiceTestDouble(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        1,
                        List.of("docs:search"),
                        Map.of())),
                false)
                .syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP discovery sync skipped: MCP registry database mode is not enabled."),
                result.warnings());
    }

    private McpToolDiscoverySyncService service(
            McpServerRegistryRepository serverRepository,
            RegistrySyncHistoryRepository historyRepository,
            McpToolDiscoveryImportService importService,
            boolean registryEnabled) {
        McpToolDiscoverySyncService service = new McpToolDiscoverySyncService();
        service.serverRegistryRepository = serverRepository;
        service.historyRepository = historyRepository;
        service.importService = importService;
        service.editionModeService = EditionModeServiceTestDouble.mcpRegistryDatabaseSupported(registryEnabled);
        return service;
    }

    private McpServerRegistry server(String requestId, String name, String schedule, Instant lastSyncAt) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport("http");
        server.setUrl("http://localhost/mcp");
        server.setEnabled(true);
        server.setSyncSchedule(schedule);
        server.setLastSyncAt(lastSyncAt);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());
        return server;
    }

    private RegistrySyncHistory history(
            String requestId,
            String sourceKind,
            String sourceRef,
            String status,
            String message,
            int itemsAffected,
            Instant startedAt) {
        RegistrySyncHistory history = new RegistrySyncHistory();
        history.setRequestId(requestId);
        history.setSourceKind(sourceKind);
        history.setSourceRef(sourceRef);
        history.setStatus(status);
        history.setMessage(message);
        history.setItemsAffected(itemsAffected);
        history.setStartedAt(startedAt);
        history.setFinishedAt(startedAt.plusMillis(10));
        return history;
    }

}
