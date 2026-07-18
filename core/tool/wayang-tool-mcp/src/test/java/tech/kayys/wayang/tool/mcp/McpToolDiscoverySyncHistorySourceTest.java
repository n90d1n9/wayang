package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.history;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncHistorySourceTest {

    @Test
    void exposesStableMcpToolSourceKind() {
        assertEquals("MCP_TOOLS", McpToolDiscoverySyncHistorySource.MCP_TOOLS);
    }

    @Test
    void identifiesMcpToolSyncHistory() {
        assertTrue(McpToolDiscoverySyncHistorySource.isMcpToolHistory(history(
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS")));
        assertFalse(McpToolDiscoverySyncHistorySource.isMcpToolHistory(history(
                "OPENAPI",
                "docs",
                "SUCCESS")));
        assertFalse(McpToolDiscoverySyncHistorySource.isMcpToolHistory(null));
    }
}
