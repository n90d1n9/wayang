package tech.kayys.wayang.tool.mcp;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpResourceTestFixtures.requestContext;

class McpToolRegistryResourceTest {

    @Test
    void listToolsReturnsEmptyList() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of());

        RestResponse<List<McpTool>> response = resource.listTools(toolRegistryQuery(null, null))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertTrue(response.getEntity().isEmpty());
    }

    @Test
    void listToolsReturnsRepositoryToolsAndFiltersCapabilities() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of(
                entity("tenant-1", "docs.search", "docs", Set.of("search")),
                entity("tenant-1", "docs.write", "docs", Set.of("write"))));

        RestResponse<List<McpTool>> response = resource.listTools(toolRegistryQuery("docs", "search"))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs.search", response.getEntity().getFirst().id());
        assertEquals(Map.of("type", "object"), response.getEntity().getFirst().inputSchema());
    }

    @Test
    void listToolsDoesNotExposeDisabledTools() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool stale = entity(
                "tenant-1",
                "docs.old-search",
                "docs",
                Set.of("search", "stale"));
        stale.setEnabled(false);
        resource.toolRegistryService = toolRegistryService(List.of(
                entity("tenant-1", "docs.search", "docs", Set.of("search")),
                stale));

        RestResponse<List<McpTool>> response = resource.listTools(toolRegistryQuery("docs", null))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("docs.search", response.getEntity().getFirst().id());
    }

    @Test
    void listToolRegistryIncludesDisabledStaleToolsForInspection() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool active = entity(
                "tenant-1",
                "docs.search",
                "docs",
                Set.of("search"));
        tech.kayys.wayang.tool.entity.McpTool stale = entity(
                "tenant-1",
                "docs.old-search",
                "docs",
                Set.of("search", "stale", "mcp:docs"));
        stale.setEnabled(false);
        withEndpoint(stale, "http://docs.local/mcp");
        tech.kayys.wayang.tool.entity.McpTool disabled = entity(
                "tenant-1",
                "docs.disabled",
                "docs",
                Set.of("search"));
        disabled.setEnabled(false);
        resource.toolRegistryService = toolRegistryService(List.of(active, stale, disabled));

        RestResponse<List<McpToolRegistryEntry>> response =
                resource.listToolRegistry(toolRegistryQuery("docs", null, "docs", false, true, null, null, null))
                        .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        McpToolRegistryEntry entry = response.getEntity().getFirst();
        assertEquals("docs.old-search", entry.toolId());
        assertFalse(entry.enabled());
        assertTrue(entry.stale());
        assertFalse(entry.serverDisabled());
        assertFalse(entry.retired());
        assertEquals(McpToolLifecycle.LIFECYCLE_STALE, entry.lifecycleState());
        assertEquals("docs", entry.serverName());
        assertEquals("http://docs.local/mcp", entry.endpoint());
        assertTrue(entry.capabilities().contains("stale"));
    }

    @Test
    void listToolRegistryFiltersByServerName() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool docsTool = entity(
                "tenant-1",
                "shared.search",
                "shared",
                Set.of("search", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool crmTool = entity(
                "tenant-1",
                "shared.lookup",
                "shared",
                Set.of("search", "mcp:crm"));
        resource.toolRegistryService = toolRegistryService(List.of(docsTool, crmTool));

        RestResponse<List<McpToolRegistryEntry>> response =
                resource.listToolRegistry(toolRegistryQuery("shared", null, "docs", null, null, null, null, null))
                        .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        assertEquals("shared.search", response.getEntity().getFirst().toolId());
        assertEquals("docs", response.getEntity().getFirst().serverName());
    }

    @Test
    void listToolRegistryExposesAndFiltersServerDisabledLifecycle() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool active = entity(
                "tenant-1",
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool serverDisabled = entity(
                "tenant-1",
                "docs.lookup",
                "docs",
                Set.of("search", "mcp:docs"));
        serverDisabled.setEnabled(false);
        serverDisabled.setTags(Set.of("mcp", "docs", McpToolLifecycle.SERVER_DISABLED_TAG));
        resource.toolRegistryService = toolRegistryService(List.of(active, serverDisabled));

        RestResponse<List<McpToolRegistryEntry>> response =
                resource.listToolRegistry(toolRegistryQuery("docs", null, null, null, null, true, null, null))
                        .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().size());
        McpToolRegistryEntry entry = response.getEntity().getFirst();
        assertEquals("docs.lookup", entry.toolId());
        assertFalse(entry.enabled());
        assertFalse(entry.stale());
        assertTrue(entry.serverDisabled());
        assertFalse(entry.retired());
        assertEquals(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED, entry.lifecycleState());
    }

    @Test
    void listToolRegistryExposesRetiredStateAndFiltersByLifecycleState() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool active = entity(
                "tenant-1",
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool retired = entity(
                "tenant-1",
                "docs.retired",
                "docs",
                Set.of("search", "stale", "mcp:docs"));
        retired.setEnabled(false);
        retired.setTags(Set.of("mcp", "docs", McpToolLifecycle.STALE_TAG, McpToolLifecycle.SERVER_RETIRED_TAG));
        resource.toolRegistryService = toolRegistryService(List.of(active, retired));

        RestResponse<List<McpToolRegistryEntry>> lifecycleResponse =
                resource.listToolRegistry(toolRegistryQuery("docs", null, null, null, null, null, null, "retired"))
                        .await().indefinitely();
        RestResponse<List<McpToolRegistryEntry>> retiredResponse =
                resource.listToolRegistry(toolRegistryQuery("docs", null, null, null, null, null, true, null))
                        .await().indefinitely();

        assertEquals(200, lifecycleResponse.getStatus());
        assertEquals(1, lifecycleResponse.getEntity().size());
        assertEquals(200, retiredResponse.getStatus());
        assertEquals(1, retiredResponse.getEntity().size());
        assertEquals("docs.retired", retiredResponse.getEntity().getFirst().toolId());
        McpToolRegistryEntry entry = lifecycleResponse.getEntity().getFirst();
        assertEquals("docs.retired", entry.toolId());
        assertFalse(entry.enabled());
        assertTrue(entry.stale());
        assertFalse(entry.serverDisabled());
        assertTrue(entry.retired());
        assertEquals(McpToolLifecycle.LIFECYCLE_RETIRED, entry.lifecycleState());
    }

    @Test
    void summarizeToolRegistryRollsUpLifecycleCountsByServer() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool docsActive = entity(
                "tenant-1",
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"));
        docsActive.setReadOnly(true);
        withEndpoint(docsActive, "http://docs.local/mcp");
        tech.kayys.wayang.tool.entity.McpTool docsStale = entity(
                "tenant-1",
                "docs.old-search",
                "docs",
                Set.of("search", "stale", "mcp:docs"));
        docsStale.setEnabled(false);
        docsStale.setRequiresApproval(true);
        withEndpoint(docsStale, "http://docs.local/mcp");
        tech.kayys.wayang.tool.entity.McpTool docsServerDisabled = entity(
                "tenant-1",
                "docs.disabled-by-server",
                "docs",
                Set.of("search", "mcp:docs"));
        docsServerDisabled.setEnabled(false);
        docsServerDisabled.setTags(Set.of("mcp", "docs", McpToolLifecycle.SERVER_DISABLED_TAG));
        withEndpoint(docsServerDisabled, "http://docs.local/mcp");
        tech.kayys.wayang.tool.entity.McpTool docsRetired = entity(
                "tenant-1",
                "docs.retired",
                "docs",
                Set.of("search", "stale", "mcp:docs"));
        docsRetired.setEnabled(false);
        docsRetired.setTags(Set.of("mcp", "docs", McpToolLifecycle.STALE_TAG, McpToolLifecycle.SERVER_RETIRED_TAG));
        withEndpoint(docsRetired, "http://docs.local/mcp");
        tech.kayys.wayang.tool.entity.McpTool crmActive = entity(
                "tenant-1",
                "crm.lookup",
                "crm",
                Set.of("search", "mcp:crm"));
        resource.toolRegistryService = toolRegistryService(List.of(
                docsActive,
                docsStale,
                docsServerDisabled,
                docsRetired,
                crmActive));

        RestResponse<McpToolRegistrySummary> response =
                resource.summarizeToolRegistry(toolRegistryQuery(null, null, null, null, null, null, null, null))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(5, response.getEntity().total());
        assertEquals(2, response.getEntity().enabled());
        assertEquals(3, response.getEntity().disabled());
        assertEquals(2, response.getEntity().stale());
        assertEquals(2, response.getEntity().active());
        assertEquals(1, response.getEntity().serverDisabled());
        assertEquals(1, response.getEntity().retired());
        assertEquals(1, response.getEntity().readOnly());
        assertEquals(1, response.getEntity().requiresApproval());
        assertEquals(2, response.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(0, response.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_DISABLED));
        assertEquals(1, response.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED));
        assertEquals(1, response.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        assertEquals(1, response.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_RETIRED));
        assertEquals(2, response.getEntity().servers().size());

        McpToolRegistrySummary.ServerSummary docs = response.getEntity().servers().stream()
                .filter(server -> "docs".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("http://docs.local/mcp", docs.endpoint());
        assertEquals(4, docs.total());
        assertEquals(1, docs.enabled());
        assertEquals(3, docs.disabled());
        assertEquals(2, docs.stale());
        assertEquals(1, docs.active());
        assertEquals(1, docs.serverDisabled());
        assertEquals(1, docs.retired());
        assertEquals(1, docs.readOnly());
        assertEquals(1, docs.requiresApproval());
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(0, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_DISABLED));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        assertEquals(1, docs.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_RETIRED));
    }

    @Test
    void summarizeToolRegistryFiltersByServerName() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of(
                entity("tenant-1", "docs.search", "shared", Set.of("search", "mcp:docs")),
                entity("tenant-1", "crm.lookup", "shared", Set.of("search", "mcp:crm"))));

        RestResponse<McpToolRegistrySummary> response =
                resource.summarizeToolRegistry(toolRegistryQuery("shared", null, "crm", null, null, null, null, null))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().total());
        assertEquals(1, response.getEntity().servers().size());
        assertEquals("crm", response.getEntity().servers().getFirst().serverName());
        assertEquals(1, response.getEntity().servers().getFirst().total());
    }

    @Test
    void summarizeToolRegistryFiltersByCapability() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of(
                entity("tenant-1", "docs.search", "docs", Set.of("search", "mcp:docs")),
                entity("tenant-1", "docs.write", "docs", Set.of("write", "mcp:docs"))));

        RestResponse<McpToolRegistrySummary> response =
                resource.summarizeToolRegistry(toolRegistryQuery("docs", "write", null, null, null, null, null, null))
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getEntity().total());
        assertEquals(1, response.getEntity().enabled());
        assertEquals(1, response.getEntity().servers().size());
        assertEquals("docs", response.getEntity().servers().getFirst().serverName());
        assertEquals(1, response.getEntity().servers().getFirst().total());
    }

    @Test
    void summarizeToolRegistryFiltersByLifecycleStateAndServerDisabled() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool docsActive = entity(
                "tenant-1",
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"));
        tech.kayys.wayang.tool.entity.McpTool docsServerDisabled = entity(
                "tenant-1",
                "docs.paused",
                "docs",
                Set.of("search", "mcp:docs"));
        docsServerDisabled.setEnabled(false);
        docsServerDisabled.setTags(Set.of("mcp", "docs", McpToolLifecycle.SERVER_DISABLED_TAG));
        tech.kayys.wayang.tool.entity.McpTool docsRetired = entity(
                "tenant-1",
                "docs.retired",
                "docs",
                Set.of("search", "stale", "mcp:docs"));
        docsRetired.setEnabled(false);
        docsRetired.setTags(Set.of("mcp", "docs", McpToolLifecycle.STALE_TAG, McpToolLifecycle.SERVER_RETIRED_TAG));
        resource.toolRegistryService = toolRegistryService(List.of(docsActive, docsServerDisabled, docsRetired));

        RestResponse<McpToolRegistrySummary> retiredResponse =
                resource.summarizeToolRegistry(toolRegistryQuery("docs", null, null, null, null, null, null, "retired"))
                        .await().indefinitely();
        RestResponse<McpToolRegistrySummary> serverDisabledResponse =
                resource.summarizeToolRegistry(toolRegistryQuery("docs", null, null, null, null, true, null, null))
                        .await().indefinitely();

        assertEquals(200, retiredResponse.getStatus());
        assertEquals(1, retiredResponse.getEntity().total());
        assertEquals(1, retiredResponse.getEntity().retired());
        assertEquals(1, retiredResponse.getEntity().stale());
        assertEquals(0, retiredResponse.getEntity().serverDisabled());
        assertEquals(1, retiredResponse.getEntity().lifecycleStates().get(McpToolLifecycle.LIFECYCLE_RETIRED));
        assertEquals(1, retiredResponse.getEntity().servers().getFirst().retired());

        assertEquals(200, serverDisabledResponse.getStatus());
        assertEquals(1, serverDisabledResponse.getEntity().total());
        assertEquals(1, serverDisabledResponse.getEntity().serverDisabled());
        assertEquals(0, serverDisabledResponse.getEntity().retired());
        assertEquals(1, serverDisabledResponse.getEntity().lifecycleStates()
                .get(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED));
        assertEquals(1, serverDisabledResponse.getEntity().servers().getFirst().serverDisabled());
    }

    @Test
    void getToolRegistryEntryReturnsDisabledStaleTool() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool stale = entity(
                "tenant-1",
                "docs.old-search",
                "docs",
                Set.of("search", "stale"));
        stale.setEnabled(false);
        resource.toolRegistryService = toolRegistryService(List.of(stale));

        RestResponse<McpToolRegistryEntry> response = resource.getToolRegistryEntry("docs.old-search")
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals("docs.old-search", response.getEntity().toolId());
        assertFalse(response.getEntity().enabled());
        assertTrue(response.getEntity().stale());
    }

    @Test
    void getToolRegistryEntryReturnsNotFound() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of());

        RestResponse<McpToolRegistryEntry> response = resource.getToolRegistryEntry("missing-tool")
                .await().indefinitely();

        assertEquals(404, response.getStatus());
    }

    @Test
    void getToolReturnsNotFound() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of());

        RestResponse<McpTool> response = resource.getTool("missing-tool")
                .await().indefinitely();

        assertEquals(404, response.getStatus());
    }

    @Test
    void getToolReturnsNotFoundForDisabledTool() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        tech.kayys.wayang.tool.entity.McpTool stale = entity(
                "tenant-1",
                "docs.old-search",
                "docs",
                Set.of("search", "stale"));
        stale.setEnabled(false);
        resource.toolRegistryService = toolRegistryService(List.of(stale));

        RestResponse<McpTool> response = resource.getTool("docs.old-search")
                .await().indefinitely();

        assertEquals(404, response.getStatus());
    }

    @Test
    void getToolReturnsRepositoryTool() {
        McpToolRegistryResource resource = new McpToolRegistryResource();
        resource.requestContext = requestContext("tenant-1");
        resource.toolRegistryService = toolRegistryService(List.of(
                entity("tenant-1", "docs.search", "docs", Set.of("search"))));

        RestResponse<McpTool> response = resource.getTool("docs.search")
                .await().indefinitely();

        assertEquals(200, response.getStatus());
        assertEquals("docs.search", response.getEntity().id());
        assertEquals("docs.search", response.getEntity().name());
    }
    private McpToolRegistryService toolRegistryService(
            List<tech.kayys.wayang.tool.entity.McpTool> tools) {
        McpToolRegistryService service = new McpToolRegistryService();
        service.toolRepository = new McpToolRepositoryTestDouble(tools);
        return service;
    }

    private McpToolRegistryQuery toolRegistryQuery(String namespace, String capability) {
        return toolRegistryQuery(namespace, capability, null, null, null, null, null, null);
    }

    private McpToolRegistryQuery toolRegistryQuery(
            String namespace,
            String capability,
            String serverName,
            Boolean enabled,
            Boolean stale,
            Boolean serverDisabled,
            Boolean retired,
            String lifecycleState) {
        return McpToolRegistryQuery.of(
                namespace,
                capability,
                serverName,
                enabled,
                stale,
                serverDisabled,
                retired,
                lifecycleState);
    }

    private tech.kayys.wayang.tool.entity.McpTool entity(
            String requestId,
            String toolId,
            String namespace,
            Set<String> capabilities) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId(requestId);
        tool.setToolId(toolId);
        tool.setNamespace(namespace);
        tool.setName(toolId);
        tool.setDescription("Tool " + toolId);
        tool.setInputSchema(Map.of("type", "object"));
        tool.setCapabilities(capabilities);
        tool.setEnabled(true);
        return tool;
    }

    private void withEndpoint(tech.kayys.wayang.tool.entity.McpTool tool, String endpoint) {
        tech.kayys.wayang.tool.entity.HttpExecutionConfig config =
                new tech.kayys.wayang.tool.entity.HttpExecutionConfig();
        config.setBaseUrl(endpoint);
        config.setPath("");
        tool.setExecutionConfig(config);
    }
}
