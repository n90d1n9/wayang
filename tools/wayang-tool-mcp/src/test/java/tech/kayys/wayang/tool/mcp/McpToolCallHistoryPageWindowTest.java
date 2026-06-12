package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolCallHistoryPageWindowTest {

    @Test
    void defaultsAndCapsPaginationValues() {
        McpToolCallHistoryPageWindow defaulted = McpToolCallHistoryPageWindow.page(null, 0);
        assertEquals(0, defaulted.offset());
        assertEquals(100, defaulted.limit());

        McpToolCallHistoryPageWindow requested = McpToolCallHistoryPageWindow.page(25, 75);
        assertEquals(25, requested.offset());
        assertEquals(75, requested.limit());

        McpToolCallHistoryPageWindow capped = McpToolCallHistoryPageWindow.page(20_000, 5_000);
        assertEquals(10_000, capped.offset());
        assertEquals(1_000, capped.limit());
    }
}
