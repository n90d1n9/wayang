package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHistoryPageWindowsTest {

    private static final McpHistoryPageWindowLimits LIMITS = new McpHistoryPageWindowLimits(
            0,
            500,
            50,
            200,
            500);

    @Test
    void boundsOffsetAndLimitForSimpleWindows() {
        McpHistoryPageWindow defaulted = McpHistoryPageWindows.bounded(null, 0, LIMITS);
        assertEquals(0, defaulted.offset());
        assertEquals(50, defaulted.limit());
        assertEquals(50, defaulted.scanLimit());

        McpHistoryPageWindow capped = McpHistoryPageWindows.bounded(5_000, 1_000, LIMITS);
        assertEquals(500, capped.offset());
        assertEquals(200, capped.limit());
        assertEquals(200, capped.scanLimit());
    }

    @Test
    void derivesOffsetAwarePageScanLimit() {
        McpHistoryPageWindow page = McpHistoryPageWindows.page(25, 100, LIMITS);
        assertEquals(25, page.offset());
        assertEquals(100, page.limit());
        assertEquals(126, page.scanLimit());

        McpHistoryPageWindow capped = McpHistoryPageWindows.page(450, 200, LIMITS);
        assertEquals(450, capped.offset());
        assertEquals(200, capped.limit());
        assertEquals(500, capped.scanLimit());
    }

    @Test
    void expandsLatestAndFilteredScanLimits() {
        McpHistoryPageWindow latest = McpHistoryPageWindows.latest(25, 100, LIMITS, 10);
        assertEquals(25, latest.offset());
        assertEquals(100, latest.limit());
        assertEquals(500, latest.scanLimit());

        assertEquals(10, McpHistoryPageWindows.filteredScanLimit(10, LIMITS, 4, false));
        assertEquals(40, McpHistoryPageWindows.filteredScanLimit(10, LIMITS, 4, true));
        assertEquals(500, McpHistoryPageWindows.expandedScanLimit(200, LIMITS, 10));
    }
}
