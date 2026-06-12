package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.HttpExecutionConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;

class McpToolDiscoveryImportPolicyTest {

    @Test
    void marksOnlyActiveImportedToolsFromCurrentImportScopeAsStale() {
        tech.kayys.wayang.tool.entity.McpTool docsTool = importedTool("docs:old", "docs");
        tech.kayys.wayang.tool.entity.McpTool activeTool = importedTool("docs:active", "docs");
        tech.kayys.wayang.tool.entity.McpTool otherServerTool = importedTool("crm:old", "crm");
        tech.kayys.wayang.tool.entity.McpTool customTool = importedTool("docs:custom", "docs");
        customTool.setCapabilities(Set.of("custom"));
        McpToolDiscoveryImportRequest request = importRequest("docs", "http://docs.local/mcp", Map.of());

        assertTrue(McpToolDiscoveryImportPolicy.shouldMarkStale(docsTool, request, Set.of("docs:active")));
        assertFalse(McpToolDiscoveryImportPolicy.shouldMarkStale(activeTool, request, Set.of("docs:active")));
        assertFalse(McpToolDiscoveryImportPolicy.shouldMarkStale(otherServerTool, request, Set.of()));
        assertFalse(McpToolDiscoveryImportPolicy.shouldMarkStale(customTool, request, Set.of()));
    }

    @Test
    void matchesEndpointScopedImportsWhenServerNameIsMissing() {
        tech.kayys.wayang.tool.entity.McpTool matchingTool = importedTool("docs:old", "docs");
        withEndpoint(matchingTool, "http://docs.local", "/mcp");
        tech.kayys.wayang.tool.entity.McpTool otherEndpointTool = importedTool("crm:old", "crm");
        withEndpoint(otherEndpointTool, "http://crm.local", "/mcp");
        McpToolDiscoveryImportRequest request = importRequest(null, "http://docs.local/mcp", Map.of());

        assertTrue(McpToolDiscoveryImportPolicy.shouldMarkStale(matchingTool, request, Set.of()));
        assertFalse(McpToolDiscoveryImportPolicy.shouldMarkStale(otherEndpointTool, request, Set.of()));
    }

    @Test
    void marksStaleAndIdentifiesReactivationCandidates() {
        tech.kayys.wayang.tool.entity.McpTool tool = importedTool("docs:old", "docs");

        McpToolDiscoveryImportPolicy.markStale(tool);

        assertFalse(tool.isEnabled());
        assertTrue(tool.getTags().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(tool.getCapabilities().contains(McpToolLifecycle.STALE_TAG));
        assertTrue(McpToolDiscoveryImportPolicy.isReactivationCandidate(tool));
    }

    @Test
    void skipsRetiredToolsUnlessReactivationIsExplicitlyAllowed() {
        tech.kayys.wayang.tool.entity.McpTool retired = importedTool("docs:retired", "docs");
        retired.setTags(McpToolLifecycle.withValue(
                retired.getTags(),
                McpToolLifecycle.SERVER_RETIRED_TAG));
        McpToolDiscoveryImportRequest blocked = importRequest("docs", "http://docs.local/mcp", Map.of());
        McpToolDiscoveryImportRequest directAllowed = importRequest("docs", "http://docs.local/mcp", Map.of(
                McpToolDiscoveryImportPolicy.CONTEXT_ALLOW_RETIRED_REACTIVATION,
                true));
        McpToolDiscoveryImportRequest customAllowed = importRequest("docs", "http://docs.local/mcp", Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA,
                Map.of(McpToolDiscoveryImportPolicy.CONTEXT_ALLOW_RETIRED_REACTIVATION, "true")));

        assertTrue(McpToolDiscoveryImportPolicy.shouldSkipRetired(retired, blocked));
        assertFalse(McpToolDiscoveryImportPolicy.shouldSkipRetired(retired, directAllowed));
        assertFalse(McpToolDiscoveryImportPolicy.shouldSkipRetired(retired, customAllowed));
    }

    @Test
    void enrichesMetadataOnlyWhenRetiredToolsWereSkipped() {
        Map<String, Object> metadata = Map.of("endpoint", "http://docs.local/mcp");

        assertSame(metadata, McpToolDiscoveryImportPolicy.metadataWithSkippedRetired(metadata, List.of()));
        assertEquals(Map.of(
                "endpoint",
                "http://docs.local/mcp",
                McpToolDiscoveryImportPolicy.METADATA_SKIPPED_RETIRED_TOOL_IDS,
                List.of("docs:retired")),
                McpToolDiscoveryImportPolicy.metadataWithSkippedRetired(metadata, List.of("docs:retired")));
    }

    private tech.kayys.wayang.tool.entity.McpTool importedTool(String toolId, String serverName) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setToolId(toolId);
        tool.setEnabled(true);
        tool.setCapabilities(McpToolLifecycle.importCapabilities(serverName, false));
        tool.setTags(McpToolLifecycle.importTags(serverName, serverName));
        return tool;
    }

    private void withEndpoint(
            tech.kayys.wayang.tool.entity.McpTool tool,
            String baseUrl,
            String path) {
        HttpExecutionConfig config = new HttpExecutionConfig();
        config.setBaseUrl(baseUrl);
        config.setPath(path);
        tool.setExecutionConfig(config);
    }
}
