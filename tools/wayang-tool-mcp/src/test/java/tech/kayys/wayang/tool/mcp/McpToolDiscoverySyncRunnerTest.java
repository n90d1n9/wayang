package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncRunnerTest {

    @Test
    void syncServerRecordsSuccessfulImport() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        3,
                        List.of("docs:search", "docs:read"),
                        List.of("docs:old"),
                        List.of("docs:read"),
                        Map.of()));

        McpToolDiscoveryServerSyncResult result = McpToolDiscoverySyncRunner.syncServer(
                importService,
                historyRepository,
                server("tenant-1", "docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.imported());
        assertEquals(1, result.stale());
        assertEquals(1, result.reactivated());
        assertTrue(result.warnings().isEmpty());
        assertEquals(1, importService.calls());
        assertEquals("tenant-1", importService.lastRequestId());
        assertEquals("docs", importService.lastRequest().serverName());
        assertEquals("system-scheduler", importService.lastRequest().createdBy());
        assertEquals(1, historyRepository.items().size());
        assertEquals("SUCCESS", historyRepository.items().getFirst().getStatus());
        assertEquals("Live MCP tools synced (imported=2, stale=1, reactivated=1)",
                historyRepository.items().getFirst().getMessage());
        assertEquals(3, historyRepository.items().getFirst().getItemsAffected());
    }

    @Test
    void syncServerRecordsFailedImportResult() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(McpToolDiscoveryImportResult.failure(
                        "docs",
                        "docs",
                        "blocked",
                        Map.of()));

        McpToolDiscoveryServerSyncResult result = McpToolDiscoverySyncRunner.syncServer(
                importService,
                historyRepository,
                server("tenant-1", "docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.imported());
        assertEquals(0, result.stale());
        assertEquals(0, result.reactivated());
        assertEquals(List.of("Live MCP tools sync failed for server docs: blocked"), result.warnings());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("blocked", historyRepository.items().getFirst().getMessage());
    }

    @Test
    void syncServerRecordsThrownImportFailure() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.failing(new IllegalStateException("network down"));

        McpToolDiscoveryServerSyncResult result = McpToolDiscoverySyncRunner.syncServer(
                importService,
                historyRepository,
                server("tenant-1", "docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: network down"), result.warnings());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("network down", historyRepository.items().getFirst().getMessage());
    }

    @Test
    void recordServerErrorRecordsHistoryAndSyncWarning() {
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();

        McpToolDiscoverySyncResult result = McpToolDiscoverySyncRunner.recordServerError(
                historyRepository,
                server("tenant-1", "docs"),
                "MCP server `docs` is disabled")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(1, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(0, result.stale());
        assertEquals(0, result.reactivated());
        assertEquals(List.of("Live MCP tools sync failed for server docs: MCP server `docs` is disabled"),
                result.warnings());
        assertEquals(1, historyRepository.items().size());
        assertEquals("ERROR", historyRepository.items().getFirst().getStatus());
        assertEquals("MCP server `docs` is disabled", historyRepository.items().getFirst().getMessage());
    }

    private McpServerRegistry server(String requestId, String name) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport(McpServerTransports.HTTP);
        server.setUrl("http://docs.local/mcp");
        server.setEnabled(true);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());
        return server;
    }

}
