package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerRegistryInspectionServiceTest {

    @Test
    void listsRegisteredServersWithFilters() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble repository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "files", "stdio", "node files.js", false, now),
                server("tenant-1", "docs", "http", "http://docs.local/mcp", true, now),
                server("tenant-1", "crm", "http", "http://crm.local/mcp", true, now),
                server("tenant-2", "other", "http", "http://other.local/mcp", true, now)));

        List<McpServerRegistryEntry> result = service(repository, true)
                .list("tenant-1", true, "HTTP")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.size());
        assertEquals("crm", result.get(0).serverName());
        assertEquals("http://crm.local/mcp", result.get(0).endpoint());
        assertEquals("docs", result.get(1).serverName());
        assertEquals("http://docs.local/mcp", result.get(1).endpoint());
    }

    @Test
    void getsRegisteredServerByName() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble repository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "files", "stdio", "node files.js", false, now)));

        McpServerRegistryEntry result = service(repository, true)
                .get("tenant-1", "files")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("files", result.serverName());
        assertEquals("stdio", result.transport());
        assertEquals("node files.js", result.endpoint());
        assertEquals(false, result.enabled());
        assertEquals("PT5M", result.syncSchedule());
        assertEquals(now, result.lastSyncAt());
    }

    @Test
    void returnsEmptyWhenRegistryModeIsUnavailable() {
        McpServerRegistryRepositoryTestDouble repository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", "http", "http://docs.local/mcp", true, Instant.now())));

        McpServerRegistryInspectionService service = service(repository, false);

        assertEquals(List.of(), service.list("tenant-1", null, null)
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service.get("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
    }

    private McpServerRegistryInspectionService service(
            McpServerRegistryRepository repository,
            boolean registryEnabled) {
        McpServerRegistryInspectionService service = new McpServerRegistryInspectionService();
        service.serverRegistryRepository = repository;
        service.editionModeService = EditionModeServiceTestDouble.mcpRegistryDatabaseSupported(registryEnabled);
        return service;
    }

    private McpServerRegistry server(
            String requestId,
            String name,
            String transport,
            String endpoint,
            boolean enabled,
            Instant lastSyncAt) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport(transport);
        if ("http".equals(transport)) {
            server.setUrl(endpoint);
        } else {
            server.setCommand(endpoint);
        }
        server.setArgsJson("[\"--stdio\"]");
        server.setEnvJson("{\"TOKEN\":\"secret\"}");
        server.setEnabled(enabled);
        server.setSource("registry.yaml");
        server.setSyncSchedule("PT5M");
        server.setLastSyncAt(lastSyncAt);
        server.setCreatedAt(lastSyncAt.minusSeconds(30));
        server.setUpdatedAt(lastSyncAt);
        return server;
    }

}
