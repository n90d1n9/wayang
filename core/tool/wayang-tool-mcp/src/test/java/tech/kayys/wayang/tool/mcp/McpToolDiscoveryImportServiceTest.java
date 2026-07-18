package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveryFailure;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoverySuccess;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.emptyDiscoverySuccess;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.httpImportRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.readOnlyDiscoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.registeredImportRequest;

class McpToolDiscoveryImportServiceTest {

    @Test
    void importsDiscoveredToolsIntoTenantRegistry() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        20,
                        Map.of("endpoint", "http://localhost/mcp"),
                        readOnlyDiscoveredTool(
                                "docs",
                                "search",
                                "Search Docs",
                                "Search documentation")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                importRequest(
                        "docs",
                        "http://localhost/mcp",
                        "knowledge",
                        Map.of(
                                McpHttpJsonRpcClient.CONTEXT_MCP_HEADERS, Map.of("Authorization", "Bearer token"),
                                McpHttpJsonRpcClient.CONTEXT_TIMEOUT_MS, 1500)))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(1, result.imported());
        assertEquals(List.of("knowledge:search"), result.toolIds());

        tech.kayys.wayang.tool.entity.McpTool saved = repository.tools().getFirst();
        assertEquals("tenant-1", saved.getRequestId());
        assertEquals("knowledge", saved.getNamespace());
        assertEquals("knowledge:search", saved.getToolId());
        assertEquals("search", saved.getOperationId());
        assertEquals("http://localhost/mcp", saved.getExecutionConfig().getBaseUrl());
        assertEquals(Map.of("Authorization", "Bearer token"), saved.getExecutionConfig().getHeaders());
        assertEquals(1500, saved.getExecutionConfig().getTimeoutMs());
        assertTrue(saved.isReadOnly());
        assertFalse(saved.isRequiresApproval());
        assertTrue(saved.getCapabilities().contains(McpToolLifecycle.MCP_TAG));
        assertTrue(saved.getCapabilities().contains(McpToolLifecycle.READ_CAPABILITY));
        assertNotNull(saved.getGuardrails());
    }

    @Test
    void importsRegisteredServerByNameWhenEndpointIsOmitted() {
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        McpServerRegistry server = server("tenant-1", "docs", "http", "http://registry.local/mcp", true);
        serverRepository.add(server);
        McpToolDiscoveryClientTestDouble discoveryClient = McpToolDiscoveryClientTestDouble.succeeding(
                discoverySuccess(
                        "docs",
                        10,
                        Map.of(),
                        discoveredTool(
                                "docs",
                                "search",
                                "Search docs")));

        McpToolDiscoveryImportService service = service(toolRepository, discoveryClient, serverRepository);

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                registeredImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(List.of("docs:search"), result.toolIds());
        assertEquals("http://registry.local/mcp", discoveryClient.lastRequest().endpoint());
        assertEquals("http://registry.local/mcp", toolRepository.tools().getFirst().getExecutionConfig().getBaseUrl());
        assertNotNull(server.getLastSyncAt());
    }

    @Test
    void failsWhenRegisteredServerIsMissing() {
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble();
        McpToolDiscoveryImportService service = service(
                toolRepository,
                McpToolDiscoveryClientTestDouble.succeeding(
                        emptyDiscoverySuccess("docs")),
                new McpServerRegistryRepositoryTestDouble());

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                registeredImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP server `docs` was not found", result.error());
        assertTrue(toolRepository.tools().isEmpty());
    }

    @Test
    void failsWhenRegisteredServerTransportCannotUseHttpDiscovery() {
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble();
        serverRepository.add(server("tenant-1", "local", "stdio", null, true));
        McpToolDiscoveryImportService service = service(
                toolRepository,
                McpToolDiscoveryClientTestDouble.succeeding(
                        emptyDiscoverySuccess("local")),
                serverRepository);

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                registeredImportRequest("local"))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("MCP server `local` uses unsupported transport `stdio` for HTTP discovery import", result.error());
        assertTrue(toolRepository.tools().isEmpty());
    }

    @Test
    void updatesExistingImportedToolWithoutDuplicatingIt() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool existing = new tech.kayys.wayang.tool.entity.McpTool();
        existing.setRequestId("tenant-1");
        existing.setNamespace("docs");
        existing.setToolId("docs:search");
        existing.setName("search");
        existing.setDescription("Old description");
        existing.setCapabilities(Set.of("old"));
        repository.add(existing);

        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        discoveredTool(
                                "docs",
                                "search",
                                "Fresh description")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                httpImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(1, repository.tools().size());
        assertEquals("Fresh description", repository.tools().getFirst().getDescription());
        assertTrue(repository.tools().getFirst().getCapabilities().contains(McpToolLifecycle.serverCapability("docs")));
    }

    @Test
    void reportsReactivatedToolsWhenStaleImportReappears() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool stale = importedTool(
                "tenant-1",
                "docs",
                "docs:search",
                "search",
                staleImportedCapabilities("docs"),
                staleImportedTags("docs"));
        stale.setEnabled(false);
        repository.add(stale);

        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        discoveredTool(
                                "docs",
                                "search",
                                "Fresh description")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                httpImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(List.of("docs:search"), result.reactivatedToolIds());
        assertEquals(1, result.reactivated());
        assertTrue(stale.isEnabled());
        assertFalse(stale.getCapabilities().contains(McpToolLifecycle.STALE_TAG));
        assertFalse(stale.getTags().contains(McpToolLifecycle.STALE_TAG));
        assertEquals("Fresh description", stale.getDescription());
    }

    @Test
    void doesNotReactivateRetiredToolWithoutExplicitIntent() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool retired = importedTool(
                "tenant-1",
                "docs",
                "docs:search",
                "search",
                staleImportedCapabilities("docs"),
                retiredImportedTags("docs"));
        retired.setEnabled(false);
        retired.setDescription("Retired description");
        repository.add(retired);

        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        discoveredTool(
                                "docs",
                                "search",
                                "Fresh description")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                httpImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(1, result.discovered());
        assertEquals(0, result.imported());
        assertEquals(List.of(), result.toolIds());
        assertEquals(List.of(), result.reactivatedToolIds());
        assertEquals(List.of("docs:search"),
                result.metadata().get(McpToolDiscoveryImportPolicy.METADATA_SKIPPED_RETIRED_TOOL_IDS));
        assertFalse(retired.isEnabled());
        assertTrue(retired.getTags().contains(McpServerLifecycleService.SERVER_RETIRED_TAG));
        assertEquals("Retired description", retired.getDescription());
    }

    @Test
    void reactivatesRetiredToolWhenExplicitlyAllowed() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool retired = importedTool(
                "tenant-1",
                "docs",
                "docs:search",
                "search",
                staleImportedCapabilities("docs"),
                retiredImportedTags("docs"));
        retired.setEnabled(false);
        repository.add(retired);

        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        discoveredTool(
                                "docs",
                                "search",
                                "Fresh description")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                importRequest(
                        "docs",
                        "http://localhost/mcp",
                        "docs",
                        Map.of(McpToolDiscoveryImportPolicy.CONTEXT_ALLOW_RETIRED_REACTIVATION, true)))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(List.of("docs:search"), result.toolIds());
        assertEquals(List.of("docs:search"), result.reactivatedToolIds());
        assertEquals(1, result.reactivated());
        assertTrue(retired.isEnabled());
        assertFalse(retired.getTags().contains(McpServerLifecycleService.SERVER_RETIRED_TAG));
        assertFalse(retired.getTags().contains(McpToolLifecycle.STALE_TAG));
        assertFalse(retired.getCapabilities().contains(McpToolLifecycle.STALE_TAG));
        assertEquals("Fresh description", retired.getDescription());
    }

    @Test
    void disablesPreviouslyImportedToolsMissingFromLatestDiscovery() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool stale = importedTool(
                "tenant-1",
                "docs",
                "docs:old_search",
                "old_search",
                importedCapabilities("docs"),
                importedTags("docs"));
        tech.kayys.wayang.tool.entity.McpTool otherServer = importedTool(
                "tenant-1",
                "docs",
                "docs:other",
                "other",
                importedCapabilities("other"),
                importedTags("other"));
        tech.kayys.wayang.tool.entity.McpTool nonMcp = importedTool(
                "tenant-1",
                "docs",
                "docs:custom",
                "custom",
                Set.of("custom"),
                Set.of("custom"));
        repository.add(stale);
        repository.add(otherServer);
        repository.add(nonMcp);

        McpToolDiscoveryImportService service = service(
                repository,
                discoverySuccess(
                        "docs",
                        discoveredTool(
                                "docs",
                                "search",
                                "Search docs")));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                httpImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.success());
        assertEquals(List.of("docs:old_search"), result.staleToolIds());
        assertEquals(1, result.stale());
        assertFalse(stale.isEnabled());
        assertTrue(stale.getCapabilities().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(stale.getTags().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(otherServer.isEnabled());
        assertTrue(nonMcp.isEnabled());
    }

    @Test
    void doesNotPersistWhenDiscoveryFails() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        McpToolDiscoveryImportService service = service(
                repository,
                discoveryFailure("docs", "blocked"));

        McpToolDiscoveryImportResult result = service.discoverAndImport(
                "tenant-1",
                httpImportRequest("docs"))
                .await().atMost(Duration.ofSeconds(3));

        assertFalse(result.success());
        assertEquals("blocked", result.error());
        assertTrue(repository.tools().isEmpty());
    }

    private McpToolDiscoveryImportService service(
            ToolRepository repository,
            McpToolDiscoveryResult discoveryResult) {
        return service(repository, McpToolDiscoveryClientTestDouble.succeeding(discoveryResult), null);
    }

    private McpToolDiscoveryImportService service(
            ToolRepository repository,
            McpToolDiscoveryClient discoveryClient,
            McpServerRegistryRepository serverRegistryRepository) {
        McpToolDiscoveryImportService service = new McpToolDiscoveryImportService();
        service.toolRepository = repository;
        service.discoveryClient = discoveryClient;
        service.serverRegistryRepository = serverRegistryRepository;
        return service;
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

    private tech.kayys.wayang.tool.entity.McpTool importedTool(
            String requestId,
            String namespace,
            String toolId,
            String name,
            Set<String> capabilities,
            Set<String> tags) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId(requestId);
        tool.setNamespace(namespace);
        tool.setToolId(toolId);
        tool.setName(name);
        tool.setDescription(name);
        tool.setEnabled(true);
        tool.setCapabilities(capabilities);
        tool.setTags(tags);
        tool.setCreatedAt(Instant.now());
        tool.setUpdatedAt(Instant.now());
        return tool;
    }

    private Set<String> importedCapabilities(String serverName) {
        return McpToolLifecycle.importCapabilities(serverName, false);
    }

    private Set<String> staleImportedCapabilities(String serverName) {
        return McpToolLifecycle.withValue(importedCapabilities(serverName), McpToolLifecycle.STALE_TAG);
    }

    private Set<String> importedTags(String serverName) {
        return McpToolLifecycle.importTags(serverName, serverName);
    }

    private Set<String> staleImportedTags(String serverName) {
        return McpToolLifecycle.withValue(importedTags(serverName), McpToolLifecycle.STALE_TAG);
    }

    private Set<String> retiredImportedTags(String serverName) {
        return McpToolLifecycle.withValue(
                staleImportedTags(serverName),
                McpServerLifecycleService.SERVER_RETIRED_TAG);
    }

}
