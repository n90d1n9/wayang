package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tools.spi.ToolContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolRegistryComponentsTest {

    @Test
    void filtersMatchCapabilityServerAndLifecyclePredicates() {
        tech.kayys.wayang.tool.entity.McpTool staleDocsSearch = tool(
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs", McpToolLifecycle.STALE_TAG),
                Set.of("Docs"));
        staleDocsSearch.setEnabled(false);
        tech.kayys.wayang.tool.entity.McpTool activeDocsWrite = tool(
                "docs.write",
                "docs",
                Set.of("write", "mcp:docs"),
                Set.of("Docs"));

        McpToolRegistryFilters filters = McpToolRegistryFilters.registry(
                "  ",
                " search ",
                " docs ",
                false,
                true,
                null,
                null,
                "stale");

        assertNull(filters.namespace());
        assertEquals("search", filters.capability());
        assertEquals("docs", filters.serverName());
        assertEquals(McpToolLifecycle.LIFECYCLE_STALE, filters.lifecycleState());
        assertTrue(filters.matches(staleDocsSearch));
        assertFalse(filters.matches(activeDocsWrite));
        assertFalse(filters.matches(null));
    }

    @Test
    void filtersNormalizeHyphenatedLifecycleState() {
        tech.kayys.wayang.tool.entity.McpTool serverDisabled = tool(
                "docs.paused",
                "docs",
                Set.of("mcp:docs"),
                Set.of("Docs", McpToolLifecycle.SERVER_DISABLED_TAG));
        serverDisabled.setEnabled(false);
        McpToolRegistryFilters filters = McpToolRegistryFilters.registry(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "server-disabled");

        assertEquals(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED, filters.lifecycleState());
        assertTrue(filters.matches(serverDisabled));
    }

    @Test
    void summariesRollUpTotalsLifecycleAndServerEndpoint() {
        tech.kayys.wayang.tool.entity.McpTool docsActive = tool(
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"),
                Set.of("Docs"));
        docsActive.setReadOnly(true);
        withEndpoint(docsActive, "http://docs.local", "/mcp");
        tech.kayys.wayang.tool.entity.McpTool docsStale = tool(
                "docs.old-search",
                "docs",
                Set.of("search", "mcp:docs", McpToolLifecycle.STALE_TAG),
                Set.of("Docs"));
        docsStale.setEnabled(false);
        docsStale.setRequiresApproval(true);
        withEndpoint(docsStale, "http://docs.local", "/mcp");
        tech.kayys.wayang.tool.entity.McpTool crmActive = tool(
                "crm.lookup",
                "crm",
                Set.of("search", "mcp:crm"),
                Set.of("crm"));

        McpToolRegistrySummary summary = McpToolRegistrySummaries.from(List.of(
                docsActive,
                docsStale,
                crmActive));

        assertEquals(3, summary.total());
        assertEquals(2, summary.enabled());
        assertEquals(1, summary.disabled());
        assertEquals(1, summary.stale());
        assertEquals(1, summary.readOnly());
        assertEquals(1, summary.requiresApproval());
        assertEquals(2, summary.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(1, summary.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_STALE));
        McpToolRegistrySummary.ServerSummary docs = summary.servers().stream()
                .filter(server -> "Docs".equals(server.serverName()))
                .findFirst()
                .orElseThrow();
        assertEquals("http://docs.local/mcp", docs.endpoint());
        assertEquals(2, docs.total());
        assertEquals(1, docs.stale());
        assertEquals(1, docs.readOnly());
        assertEquals(1, docs.requiresApproval());
    }

    @Test
    void mapperBuildsRegistryEntryAndDefaultMcpContext() {
        tech.kayys.wayang.tool.entity.McpTool source = tool(
                "docs.search",
                "docs",
                Set.of("search", "mcp:docs"),
                Set.of("Docs"));
        source.setOperationId("search");
        source.setReadOnly(true);
        source.setRequiresApproval(true);
        withEndpoint(source, "http://docs.local", "/mcp");
        source.getExecutionConfig().setHeaders(Map.of("Authorization", "Bearer token"));
        source.getExecutionConfig().setTimeoutMs(1234);
        McpToolClientTestDouble client = McpToolClientTestDouble.succeedingOk();

        McpToolRegistryEntry entry = McpToolRegistryMapper.toRegistryEntry(source);
        McpTool executable = McpToolRegistryMapper.toMcpTool(source, client);

        executable.executeAsync(Map.of("query", "wayang"), ToolContext.defaults())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals("docs.search", entry.toolId());
        assertEquals("Docs", entry.serverName());
        assertEquals("http://docs.local/mcp", entry.endpoint());
        assertTrue(entry.readOnly());
        assertTrue(entry.requiresApproval());
        assertEquals(Map.of("type", "object"), entry.inputSchema());
        assertEquals("docs.search", executable.id());
        assertEquals("http://docs.local/mcp",
                client.lastInvocation().context().get(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT));
        assertEquals(Map.of("Authorization", "Bearer token"),
                client.lastInvocation().context().get(McpHttpJsonRpcClient.CONTEXT_MCP_HEADERS));
        assertEquals(1234,
                client.lastInvocation().context().get(McpHttpJsonRpcClient.CONTEXT_TIMEOUT_MS));
        assertEquals("search",
                client.lastInvocation().context().get(HttpMcpToolClient.CONTEXT_TOOL_NAME));
    }

    private tech.kayys.wayang.tool.entity.McpTool tool(
            String toolId,
            String namespace,
            Set<String> capabilities,
            Set<String> tags) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId("tenant-1");
        tool.setToolId(toolId);
        tool.setNamespace(namespace);
        tool.setName(toolId);
        tool.setDescription("Tool " + toolId);
        tool.setInputSchema(Map.of("type", "object"));
        tool.setCapabilities(capabilities);
        tool.setTags(tags);
        tool.setEnabled(true);
        return tool;
    }

    private void withEndpoint(
            tech.kayys.wayang.tool.entity.McpTool tool,
            String baseUrl,
            String path) {
        tech.kayys.wayang.tool.entity.HttpExecutionConfig config =
                new tech.kayys.wayang.tool.entity.HttpExecutionConfig();
        config.setBaseUrl(baseUrl);
        config.setPath(path);
        tool.setExecutionConfig(config);
    }

}
