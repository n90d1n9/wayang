package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoverySyncMessagesTest {

    @Test
    void buildsSharedSyncWarnings() {
        assertEquals(
                "Live MCP discovery sync skipped: MCP registry database mode is not enabled.",
                McpToolDiscoverySyncMessages.registryModeDisabled());
        assertEquals(
                "Live MCP tools sync failed: serverName is required",
                McpToolDiscoverySyncMessages.serverNameRequired());
        assertEquals(
                "Live MCP tools sync failed: MCP server registry is not configured",
                McpToolDiscoverySyncMessages.serverRegistryNotConfigured());
        assertEquals(
                "Live MCP tools sync failed for server docs: MCP server registry is not configured",
                McpToolDiscoverySyncMessages.serverRegistryNotConfigured("docs"));
        assertEquals(
                "Live MCP tools sync failed for server docs: MCP server was not found",
                McpToolDiscoverySyncMessages.serverNotFound("docs"));
        assertEquals(
                "Live MCP tools sync failed for server docs: blocked",
                McpToolDiscoverySyncMessages.syncFailedForServer("docs", "blocked"));
    }

    @Test
    void buildsSharedSyncRecordMessages() {
        McpToolDiscoveryImportResult result = McpToolDiscoveryImportResult.success(
                "docs",
                "docs",
                3,
                List.of("docs:search", "docs:read"),
                List.of("docs:old"),
                List.of("docs:read"),
                Map.of());

        assertEquals("MCP server `docs` is disabled", McpToolDiscoverySyncMessages.serverDisabled("docs"));
        assertEquals(
                "Live MCP tools sync skipped for server docs: invalid interval",
                McpToolDiscoverySyncMessages.invalidSchedule("docs", "invalid interval"));
        assertEquals(
                "Live MCP tools synced (imported=2, stale=1, reactivated=1)",
                McpToolDiscoverySyncMessages.success(result));
    }
}
