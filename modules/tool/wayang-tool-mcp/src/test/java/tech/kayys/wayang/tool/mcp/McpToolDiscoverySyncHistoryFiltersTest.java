package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static tech.kayys.wayang.tool.mcp.McpToolDiscoverySyncHistoryFixtures.history;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncHistoryFiltersTest {

    @Test
    void normalizesFiltersAndBoundsListScanLimit() {
        McpToolDiscoverySyncHistoryFilters filters = McpToolDiscoverySyncHistoryFilters.of(
                " DOCS ",
                " success ",
                0);

        assertEquals("docs", filters.serverName());
        assertEquals("SUCCESS", filters.status());
        assertEquals(50, filters.limit());
        assertEquals(200, filters.scanLimit());
    }

    @Test
    void latestFiltersExpandRepositoryScanLimit() {
        McpToolDiscoverySyncHistoryFilters filters = McpToolDiscoverySyncHistoryFilters.latest(
                null,
                null,
                200);

        assertEquals(200, filters.limit());
        assertEquals(500, filters.scanLimit());
    }

    @Test
    void matchesMcpHistoryByNormalizedServerAndStatus() {
        McpToolDiscoverySyncHistoryFilters filters = McpToolDiscoverySyncHistoryFilters.of(
                "DOCS",
                "success",
                10);

        assertTrue(filters.matches(history(
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "SUCCESS")));
        assertFalse(filters.matches(history(
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "crm",
                "SUCCESS")));
        assertFalse(filters.matches(history(
                McpToolDiscoverySyncHistorySource.MCP_TOOLS,
                "docs",
                "ERROR")));
        assertFalse(filters.matches(history(
                "OPENAPI",
                "docs",
                "SUCCESS")));
    }
}
