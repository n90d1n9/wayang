package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.registeredImportRequest;

class McpToolDiscoveryImportResolverTest {

    @Test
    void usesDirectEndpointWithoutRegistryLookup() {
        McpToolDiscoveryImportRequest request = importRequest(
                "docs",
                "http://direct.local/mcp",
                "docs",
                Map.of());

        McpToolDiscoveryImportResolution resolution = McpToolDiscoveryImportResolver.resolve(
                "tenant-1",
                request,
                null)
                .await().atMost(Duration.ofSeconds(3));

        assertSame(request, resolution.request());
        assertNull(resolution.server());
    }

    @Test
    void resolvesRegisteredServerAndInjectsEndpointContext() {
        McpServerRegistryRepositoryTestDouble repository = new McpServerRegistryRepositoryTestDouble();
        McpServerRegistry server = server(
                "tenant-1",
                "docs",
                McpServerTransports.HTTP,
                "http://registry.local/mcp",
                true);
        repository.add(server);

        McpToolDiscoveryImportResolution resolution = McpToolDiscoveryImportResolver.resolve(
                "tenant-1",
                registeredImportRequest("docs"),
                repository)
                .await().atMost(Duration.ofSeconds(3));

        assertSame(server, resolution.server());
        assertEquals("http://registry.local/mcp", resolution.request().endpoint());
        assertEquals("http://registry.local/mcp",
                resolution.request().context().get(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT));
    }

    @Test
    void validatesMissingRegistryInputsAndUnsupportedServers() {
        McpServerRegistryRepositoryTestDouble repository = new McpServerRegistryRepositoryTestDouble();
        repository.add(server("tenant-1", "disabled", McpServerTransports.HTTP, "http://local/mcp", false));
        repository.add(server("tenant-1", "stdio", McpServerTransports.STDIO, null, true));
        repository.add(server("tenant-1", "missing-url", McpServerTransports.HTTP, null, true));

        assertFailure("MCP endpoint or serverName is required for discovery import",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        importRequest(null, null, null, Map.of()),
                        repository));
        assertFailure("MCP server registry is not configured",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        registeredImportRequest("docs"),
                        null));
        assertFailure("MCP server `docs` was not found",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        registeredImportRequest("docs"),
                        repository));
        assertFailure("MCP server `disabled` is disabled",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        registeredImportRequest("disabled"),
                        repository));
        assertFailure("MCP server `stdio` uses unsupported transport `stdio` for HTTP discovery import",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        registeredImportRequest("stdio"),
                        repository));
        assertFailure("MCP server `missing-url` does not define a URL",
                () -> McpToolDiscoveryImportResolver.resolve(
                        "tenant-1",
                        registeredImportRequest("missing-url"),
                        repository));
    }

    private void assertFailure(String message, ResolverCall call) {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> call.resolve().await().atMost(Duration.ofSeconds(3)));

        assertEquals(message, error.getMessage());
    }

    private McpServerRegistry server(
            String requestId,
            String name,
            String transport,
            String url,
            boolean enabled) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport(transport);
        server.setUrl(url);
        server.setEnabled(enabled);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());
        return server;
    }

    @FunctionalInterface
    private interface ResolverCall {
        Uni<McpToolDiscoveryImportResolution> resolve();
    }

}
