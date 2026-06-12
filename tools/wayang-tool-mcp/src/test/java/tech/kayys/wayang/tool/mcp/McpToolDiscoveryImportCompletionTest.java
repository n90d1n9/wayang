package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveryFailure;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoverySuccess;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;

class McpToolDiscoveryImportCompletionTest {

    @Test
    void buildsSuccessResultAndTouchesResolvedRegistryServer() {
        McpServerRegistry server = server();
        McpToolDiscoveryImportChanges changes = McpToolDiscoveryImportChanges.empty();
        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:search"), false));
        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:refresh"), true));
        changes.add(McpToolDiscoveryImportChange.skippedRetired("docs:retired"));

        McpToolDiscoveryImportResult result = McpToolDiscoveryImportCompletion.success(
                new McpToolDiscoveryImportResolution(request("docs", "knowledge"), server),
                "knowledge",
                discoverySuccess(
                        "docs",
                        42,
                        Map.of("endpoint", "http://docs.local/mcp"),
                        discoveredTool("docs", "search"),
                        discoveredTool("docs", "refresh"),
                        discoveredTool("docs", "retired")),
                changes,
                List.of("docs:stale"));

        assertTrue(result.success());
        assertEquals("docs", result.serverName());
        assertEquals("knowledge", result.namespace());
        assertEquals(3, result.discovered());
        assertEquals(List.of("docs:search", "docs:refresh"), result.toolIds());
        assertEquals(List.of("docs:stale"), result.staleToolIds());
        assertEquals(List.of("docs:refresh"), result.reactivatedToolIds());
        assertEquals(List.of("docs:retired"),
                result.metadata().get(McpToolDiscoveryImportPolicy.METADATA_SKIPPED_RETIRED_TOOL_IDS));
        assertNotNull(server.getLastSyncAt());
        assertEquals(server.getLastSyncAt(), server.getUpdatedAt());
    }

    @Test
    void buildsDiscoveryAndThrowableFailures() {
        McpToolDiscoveryImportResult discoveryFailureResult = McpToolDiscoveryImportCompletion.discoveryFailure(
                new McpToolDiscoveryImportResolution(request("docs", null), null),
                discoveryFailure("docs", "blocked", 10, Map.of("status", "denied")));

        assertFalse(discoveryFailureResult.success());
        assertEquals("docs", discoveryFailureResult.serverName());
        assertEquals("docs", discoveryFailureResult.namespace());
        assertEquals("blocked", discoveryFailureResult.error());
        assertEquals(Map.of("status", "denied"), discoveryFailureResult.metadata());

        McpToolDiscoveryImportResult throwableFailure = McpToolDiscoveryImportCompletion.failure(
                request("docs", "knowledge"),
                "knowledge",
                new IllegalStateException("registry unavailable"));

        assertFalse(throwableFailure.success());
        assertEquals("knowledge", throwableFailure.namespace());
        assertEquals("registry unavailable", throwableFailure.error());
        assertTrue(throwableFailure.metadata().isEmpty());
    }

    private McpToolDiscoveryImportRequest request(String serverName, String namespace) {
        return importRequest(
                serverName,
                "http://docs.local/mcp",
                namespace,
                Map.of());
    }

    private McpServerRegistry server() {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId("tenant-1");
        server.setName("docs");
        server.setTransport(McpServerTransports.HTTP);
        server.setUrl("http://docs.local/mcp");
        server.setEnabled(true);
        server.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        server.setUpdatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        return server;
    }

    private tech.kayys.wayang.tool.entity.McpTool tool(String toolId) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setToolId(toolId);
        return tool;
    }

}
