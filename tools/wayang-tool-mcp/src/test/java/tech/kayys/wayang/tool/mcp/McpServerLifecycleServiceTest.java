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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerLifecycleServiceTest {

    @Test
    void disablingServerDisablesOnlyActiveToolsImportedFromThatServer() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", true, now)));
        tech.kayys.wayang.tool.entity.McpTool docsSearch =
                tool("tenant-1", "docs:search", "docs", true, Set.of("mcp", "tool", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool docsStale =
                tool("tenant-1", "docs:old-search", "docs", false, Set.of("mcp", "tool", "mcp:docs", "stale"));
        docsStale.setTags(Set.of("mcp", "docs", "stale"));
        tech.kayys.wayang.tool.entity.McpTool crmSearch =
                tool("tenant-1", "crm:search", "crm", true, Set.of("mcp", "tool", "mcp:crm"));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(docsSearch, docsStale, crmSearch));

        McpServerLifecycleResult result = service(serverRepository, toolRepository, true)
                .setEnabled("tenant-1", "docs", false)
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("docs", result.server().serverName());
        assertFalse(result.server().enabled());
        assertEquals(List.of("docs:search"), result.disabledToolIds());
        assertEquals(List.of(), result.reactivatedToolIds());
        assertEquals(1, result.affectedTools());
        assertFalse(docsSearch.isEnabled());
        assertTrue(docsSearch.getTags().contains(McpServerLifecycleService.SERVER_DISABLED_TAG));
        assertFalse(docsStale.isEnabled());
        assertFalse(docsStale.getTags().contains(McpServerLifecycleService.SERVER_DISABLED_TAG));
        assertTrue(crmSearch.isEnabled());
    }

    @Test
    void enablingServerReactivatesOnlyToolsDisabledByServerLifecycle() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", false, now)));
        tech.kayys.wayang.tool.entity.McpTool docsSearch =
                tool("tenant-1", "docs:search", "docs", false, Set.of("mcp", "tool", "mcp:docs"));
        docsSearch.setTags(Set.of("mcp", "docs", McpServerLifecycleService.SERVER_DISABLED_TAG));
        tech.kayys.wayang.tool.entity.McpTool docsStale =
                tool("tenant-1", "docs:old-search", "docs", false, Set.of("mcp", "tool", "mcp:docs", "stale"));
        docsStale.setTags(Set.of("mcp", "docs", "stale"));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(docsSearch, docsStale));

        McpServerLifecycleResult result = service(serverRepository, toolRepository, true)
                .setEnabled("tenant-1", "docs", true)
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(result.server().enabled());
        assertEquals(List.of(), result.disabledToolIds());
        assertEquals(List.of("docs:search"), result.reactivatedToolIds());
        assertTrue(docsSearch.isEnabled());
        assertFalse(docsSearch.getTags().contains(McpServerLifecycleService.SERVER_DISABLED_TAG));
        assertFalse(docsStale.isEnabled());
    }

    @Test
    void previewsLifecycleImpactWithoutMutatingTools() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", false, now)));
        tech.kayys.wayang.tool.entity.McpTool active =
                tool("tenant-1", "docs:search", "docs", true, Set.of("mcp", "tool", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool serverDisabled =
                tool("tenant-1", "docs:lookup", "docs", false, Set.of("mcp", "tool", "mcp:docs"));
        serverDisabled.setTags(Set.of("mcp", "docs", McpServerLifecycleService.SERVER_DISABLED_TAG));
        tech.kayys.wayang.tool.entity.McpTool stale =
                tool("tenant-1", "docs:old-search", "docs", false, Set.of("mcp", "tool", "mcp:docs", "stale"));
        stale.setTags(Set.of("mcp", "docs", "stale"));
        tech.kayys.wayang.tool.entity.McpTool retired =
                tool("tenant-1", "docs:retired", "docs", false, Set.of("mcp", "tool", "mcp:docs", "stale"));
        retired.setTags(Set.of("mcp", "docs", "stale", McpServerLifecycleService.SERVER_RETIRED_TAG));
        tech.kayys.wayang.tool.entity.McpTool crm =
                tool("tenant-1", "crm:search", "crm", true, Set.of("mcp", "tool", "mcp:crm"));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(retired, stale, active, crm, serverDisabled));

        McpServerLifecycleImpact impact = service(serverRepository, toolRepository, true)
                .impact("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("docs", impact.server().serverName());
        assertEquals(List.of("docs:lookup", "docs:old-search", "docs:retired", "docs:search"),
                impact.importedToolIds());
        assertEquals(List.of("docs:search"), impact.activeToolIds());
        assertEquals(List.of("docs:old-search", "docs:retired"), impact.staleToolIds());
        assertEquals(List.of("docs:lookup"), impact.serverDisabledToolIds());
        assertEquals(List.of("docs:retired"), impact.retiredToolIds());
        assertEquals(List.of("docs:search"), impact.disableAffectedToolIds());
        assertEquals(List.of("docs:lookup"), impact.enableAffectedToolIds());
        assertEquals(List.of("docs:lookup", "docs:old-search", "docs:search"), impact.retireAffectedToolIds());
        assertTrue(active.isEnabled());
        assertFalse(serverDisabled.isEnabled());
        assertTrue(crm.isEnabled());
    }

    @Test
    void retiringServerDeletesRegistryEntryAndMarksImportedToolsStale() {
        Instant now = Instant.now();
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", true, now),
                server("tenant-1", "crm", true, now)));
        tech.kayys.wayang.tool.entity.McpTool docsSearch =
                tool("tenant-1", "docs:search", "docs", true, Set.of("mcp", "tool", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool docsDisabled =
                tool("tenant-1", "docs:disabled", "docs", false, Set.of("mcp", "tool", "mcp:docs"));
        docsDisabled.setTags(Set.of("mcp", "docs", McpServerLifecycleService.SERVER_DISABLED_TAG));
        tech.kayys.wayang.tool.entity.McpTool crmSearch =
                tool("tenant-1", "crm:search", "crm", true, Set.of("mcp", "tool", "mcp:crm"));
        McpToolRepositoryTestDouble toolRepository = new McpToolRepositoryTestDouble(List.of(docsSearch, docsDisabled, crmSearch));

        McpServerRetirementResult result = service(serverRepository, toolRepository, true)
                .retire("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("docs", result.server().serverName());
        assertEquals(List.of("docs:search", "docs:disabled"), result.retiredToolIds());
        assertEquals(2, result.affectedTools());
        assertEquals(1, serverRepository.servers().size());
        assertEquals("crm", serverRepository.servers().getFirst().getName());
        assertFalse(docsSearch.isEnabled());
        assertTrue(docsSearch.getTags().contains("stale"));
        assertTrue(docsSearch.getTags().contains(McpServerLifecycleService.SERVER_RETIRED_TAG));
        assertTrue(docsSearch.getCapabilities().contains("stale"));
        assertFalse(docsDisabled.getTags().contains(McpServerLifecycleService.SERVER_DISABLED_TAG));
        assertTrue(docsDisabled.getTags().contains(McpServerLifecycleService.SERVER_RETIRED_TAG));
        assertTrue(crmSearch.isEnabled());
    }

    @Test
    void retiringMissingServerReturnsNull() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", true, Instant.now())));

        assertNull(service(serverRepository, new McpToolRepositoryTestDouble(List.of()), true)
                .impact("tenant-1", "missing")
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service(serverRepository, new McpToolRepositoryTestDouble(List.of()), true)
                .retire("tenant-1", "missing")
                .await().atMost(Duration.ofSeconds(3)));
    }

    @Test
    void returnsNullWhenRegistryModeIsUnavailable() {
        McpServerRegistryRepositoryTestDouble serverRepository = new McpServerRegistryRepositoryTestDouble(List.of(
                server("tenant-1", "docs", true, Instant.now())));

        assertNull(service(serverRepository, new McpToolRepositoryTestDouble(List.of()), false)
                .setEnabled("tenant-1", "docs", false)
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service(serverRepository, new McpToolRepositoryTestDouble(List.of()), false)
                .impact("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service(serverRepository, new McpToolRepositoryTestDouble(List.of()), false)
                .retire("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
    }

    private McpServerLifecycleService service(
            McpServerRegistryRepository serverRepository,
            ToolRepository toolRepository,
            boolean registryEnabled) {
        McpServerLifecycleService service = new McpServerLifecycleService();
        service.serverRegistryRepository = serverRepository;
        service.toolRepository = toolRepository;
        service.editionModeService = EditionModeServiceTestDouble.mcpRegistryDatabaseSupported(registryEnabled);
        return service;
    }

    private McpServerRegistry server(String requestId, String name, boolean enabled, Instant timestamp) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId(requestId);
        server.setName(name);
        server.setTransport("http");
        server.setUrl("http://" + name + ".local/mcp");
        server.setEnabled(enabled);
        server.setSource("registry.yaml");
        server.setSyncSchedule("PT5M");
        server.setCreatedAt(timestamp.minusSeconds(30));
        server.setUpdatedAt(timestamp);
        return server;
    }

    private tech.kayys.wayang.tool.entity.McpTool tool(
            String requestId,
            String toolId,
            String namespace,
            boolean enabled,
            Set<String> capabilities) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId(requestId);
        tool.setToolId(toolId);
        tool.setName(toolId);
        tool.setNamespace(namespace);
        tool.setEnabled(enabled);
        tool.setTags(Set.of("mcp", namespace));
        tool.setCapabilities(capabilities);
        tool.setInputSchema(Map.of("type", "object"));
        tool.setCreatedAt(Instant.now().minusSeconds(30));
        tool.setUpdatedAt(Instant.now().minusSeconds(30));
        return tool;
    }

}
