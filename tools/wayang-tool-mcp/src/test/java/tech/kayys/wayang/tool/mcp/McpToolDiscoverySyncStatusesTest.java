package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncStatusesTest {

    @Test
    void exposesStableSyncStatusValues() {
        assertEquals("SUCCESS", McpToolDiscoverySyncStatuses.SUCCESS);
        assertEquals("ERROR", McpToolDiscoverySyncStatuses.ERROR);
    }

    @Test
    void matchesStatusesCaseInsensitively() {
        assertTrue(McpToolDiscoverySyncStatuses.isSuccess("success"));
        assertTrue(McpToolDiscoverySyncStatuses.isError("error"));
        assertFalse(McpToolDiscoverySyncStatuses.isSuccess("error"));
        assertFalse(McpToolDiscoverySyncStatuses.isError("success"));
        assertFalse(McpToolDiscoverySyncStatuses.isSuccess(null));
        assertFalse(McpToolDiscoverySyncStatuses.isError(null));
    }
}
