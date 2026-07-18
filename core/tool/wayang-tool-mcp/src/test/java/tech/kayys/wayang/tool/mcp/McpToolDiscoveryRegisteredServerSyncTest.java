package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoveryRegisteredServerSyncTest {

    @Test
    void warnsWhenServerNameIsMissing() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryRegisteredServerSync.sync(
                serverRepository,
                importService,
                new McpRegistrySyncHistoryRepositoryTestDouble(),
                "tenant-1",
                " ")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed: serverName is required"), result.warnings());
        assertEquals(0, serverRepository.findByRequestIdAndNameCalls());
        assertEquals(0, importService.calls());
    }

    @Test
    void warnsWhenRegistryRepositoryIsMissing() {
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryRegisteredServerSync.sync(
                null,
                importService,
                new McpRegistrySyncHistoryRepositoryTestDouble(),
                "tenant-1",
                "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: MCP server registry is not configured"),
                result.warnings());
        assertEquals(0, importService.calls());
    }

    @Test
    void warnsWhenServerIsMissing() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryRegisteredServerSync.sync(
                serverRepository,
                importService,
                new McpRegistrySyncHistoryRepositoryTestDouble(),
                "tenant-1",
                "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("Live MCP tools sync failed for server docs: MCP server was not found"), result.warnings());
        assertEquals(1, serverRepository.findByRequestIdAndNameCalls());
        assertEquals(0, importService.calls());
    }

    @Test
    void recordsDisabledServerFailureWithoutImporting() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpServerRegistry disabled = server("tenant-1", "docs");
        disabled.setEnabled(false);
        serverRepository.add(disabled);
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryRegisteredServerSync.sync(
                serverRepository,
                importService,
                historyRepository,
                "tenant-1",
                "docs")
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
    void syncsEnabledServerThroughRunner() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "docs"));
        McpRegistrySyncHistoryRepositoryTestDouble historyRepository =
                new McpRegistrySyncHistoryRepositoryTestDouble();
        McpToolDiscoveryImportServiceTestDouble importService =
                McpToolDiscoveryImportServiceTestDouble.succeeding(successResult());

        McpToolDiscoverySyncResult result = McpToolDiscoveryRegisteredServerSync.sync(
                serverRepository,
                importService,
                historyRepository,
                "tenant-1",
                "docs")
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

    private static McpServerRegistry server(String requestId, String name) {
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
