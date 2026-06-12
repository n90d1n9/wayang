package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;

class McpToolDiscoveryImportUpserterTest {

    @Test
    void savesNewToolsAndTracksReactivatedStaleTools() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool stale = importedTool(
                "tenant-1",
                "docs",
                "docs:refresh",
                "docs");
        McpToolDiscoveryImportPolicy.markStale(stale);
        stale.setDescription("Old refresh tool");
        repository.add(stale);

        McpToolDiscoveryImportChanges changes = McpToolDiscoveryImportUpserter.upsertTools(
                repository,
                "tenant-1",
                request(),
                "docs",
                List.of(
                        discoveredTool("docs", "search", "Search docs"),
                        discoveredTool("docs", "refresh", "Refresh docs")))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:search", "docs:refresh"), changes.toolIds());
        assertEquals(List.of("docs:refresh"), changes.reactivatedToolIds());
        assertTrue(changes.skippedRetiredToolIds().isEmpty());
        assertEquals(2, repository.tools().size());
        assertEquals("Search docs", repository.findLocal("docs:search").getDescription());
        assertEquals("Refresh docs", stale.getDescription());
        assertTrue(stale.isEnabled());
        assertFalse(McpToolLifecycle.isStale(stale));
    }

    @Test
    void skipsRetiredToolsWhenReactivationIsNotAllowed() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool retired = importedTool(
                "tenant-1",
                "docs",
                "docs:retired",
                "docs");
        retired.setTags(McpToolLifecycle.withValue(
                retired.getTags(),
                McpToolLifecycle.SERVER_RETIRED_TAG));
        retired.setDescription("Existing retired tool");
        repository.add(retired);

        McpToolDiscoveryImportChanges changes = McpToolDiscoveryImportUpserter.upsertTools(
                repository,
                "tenant-1",
                request(),
                "docs",
                List.of(discoveredTool("docs", "retired", "Discovered retired tool")))
                .await().atMost(Duration.ofSeconds(3));

        assertTrue(changes.toolIds().isEmpty());
        assertTrue(changes.reactivatedToolIds().isEmpty());
        assertEquals(List.of("docs:retired"), changes.skippedRetiredToolIds());
        assertEquals(1, repository.tools().size());
        assertEquals("Existing retired tool", retired.getDescription());
    }

    private McpToolDiscoveryImportRequest request() {
        return importRequest(
                "docs",
                "http://docs.local/mcp",
                "docs",
                Map.of());
    }

    private tech.kayys.wayang.tool.entity.McpTool importedTool(
            String requestId,
            String namespace,
            String toolId,
            String serverName) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setRequestId(requestId);
        tool.setNamespace(namespace);
        tool.setToolId(toolId);
        tool.setName(toolId.substring(toolId.indexOf(':') + 1));
        tool.setEnabled(true);
        tool.setTags(McpToolLifecycle.importTags(serverName, namespace));
        tool.setCapabilities(McpToolLifecycle.importCapabilities(serverName, false));
        return tool;
    }

}
