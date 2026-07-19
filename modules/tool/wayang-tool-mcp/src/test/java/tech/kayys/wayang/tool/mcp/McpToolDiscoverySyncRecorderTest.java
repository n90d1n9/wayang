package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncRecorderTest {

    @Test
    void recordsSuccessHistoryWithMcpToolSourceKind() {
        McpRegistrySyncHistoryRepositoryTestDouble repository = new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant finishedAt = Instant.parse("2026-01-01T00:00:03Z");

        McpToolDiscoverySyncRecorder.recordSuccess(
                repository,
                server("tenant-1", "docs"),
                McpToolDiscoveryImportResult.success(
                        "docs",
                        "docs",
                        3,
                        List.of("docs:search", "docs:read"),
                        List.of("docs:old"),
                        List.of("docs:read"),
                        Map.of()),
                startedAt,
                finishedAt)
                .await().atMost(Duration.ofSeconds(3));

        RegistrySyncHistory history = repository.items().getFirst();
        assertTrue(McpToolDiscoverySyncHistorySource.isMcpToolHistory(history));
        assertEquals("tenant-1", history.getRequestId());
        assertEquals(McpToolDiscoverySyncHistorySource.MCP_TOOLS, history.getSourceKind());
        assertEquals("docs", history.getSourceRef());
        assertEquals("SUCCESS", history.getStatus());
        assertEquals("Live MCP tools synced (imported=2, stale=1, reactivated=1)", history.getMessage());
        assertEquals(3, history.getItemsAffected());
        assertEquals(startedAt, history.getStartedAt());
        assertEquals(finishedAt, history.getFinishedAt());
    }

    @Test
    void recordsErrorHistoryAndBuildsWarningMessage() {
        McpRegistrySyncHistoryRepositoryTestDouble repository = new McpRegistrySyncHistoryRepositoryTestDouble();
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant finishedAt = Instant.parse("2026-01-01T00:00:01Z");

        McpToolDiscoverySyncRecorder.recordError(
                repository,
                server("tenant-1", "docs"),
                "blocked",
                startedAt,
                finishedAt)
                .await().atMost(Duration.ofSeconds(3));

        RegistrySyncHistory history = repository.items().getFirst();
        assertEquals("ERROR", history.getStatus());
        assertEquals("blocked", history.getMessage());
        assertEquals(0, history.getItemsAffected());
        assertEquals("Live MCP tools sync failed for server docs: blocked",
                McpToolDiscoverySyncRecorder.failureWarning("docs", "blocked"));
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
