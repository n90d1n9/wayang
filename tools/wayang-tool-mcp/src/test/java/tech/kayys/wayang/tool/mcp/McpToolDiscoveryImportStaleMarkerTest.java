package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;

class McpToolDiscoveryImportStaleMarkerTest {

    @Test
    void marksOnlyMissingImportedToolsInScopeAsStale() {
        McpToolRepositoryTestDouble repository = new McpToolRepositoryTestDouble();
        tech.kayys.wayang.tool.entity.McpTool stale = importedTool(
                "tenant-1",
                "docs",
                "docs:old",
                "docs");
        tech.kayys.wayang.tool.entity.McpTool active = importedTool(
                "tenant-1",
                "docs",
                "docs:active",
                "docs");
        tech.kayys.wayang.tool.entity.McpTool otherServer = importedTool(
                "tenant-1",
                "docs",
                "crm:old",
                "crm");
        otherServer.setTags(McpToolLifecycle.importTags("crm", "crm"));
        tech.kayys.wayang.tool.entity.McpTool otherTenant = importedTool(
                "tenant-2",
                "docs",
                "docs:other-tenant",
                "docs");
        repository.addAll(List.of(stale, active, otherServer, otherTenant));

        List<String> staleToolIds = McpToolDiscoveryImportStaleMarker.markStaleTools(
                repository,
                "tenant-1",
                importRequest("docs", "http://docs.local/mcp", "docs", Map.of()),
                "docs",
                List.of("docs:active"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(List.of("docs:old"), staleToolIds);
        assertFalse(stale.isEnabled());
        assertTrue(stale.getTags().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(stale.getCapabilities().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(active.isEnabled());
        assertTrue(otherServer.isEnabled());
        assertTrue(otherTenant.isEnabled());
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
        tool.setEnabled(true);
        tool.setTags(McpToolLifecycle.importTags(serverName, namespace));
        tool.setCapabilities(McpToolLifecycle.importCapabilities(serverName, false));
        return tool;
    }
}
