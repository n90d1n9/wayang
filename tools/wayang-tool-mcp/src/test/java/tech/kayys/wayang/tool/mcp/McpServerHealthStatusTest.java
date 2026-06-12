package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerHealthStatusTest {

    @Test
    void normalizesStatusValues() {
        assertEquals(McpServerHealthStatus.UNHEALTHY, McpServerHealthStatus.normalize("unhealthy"));
        assertEquals(McpServerHealthStatus.DEGRADED, McpServerHealthStatus.normalize(" degraded "));
        assertEquals("NEEDS_REVIEW", McpServerHealthStatus.normalize("needs-review"));
        assertNull(McpServerHealthStatus.normalize(" "));
    }

    @Test
    void ranksAndComparesKnownStatuses() {
        assertEquals(4, McpServerHealthStatus.rank("unhealthy"));
        assertEquals(3, McpServerHealthStatus.rank("degraded"));
        assertEquals(2, McpServerHealthStatus.rank("unsynced"));
        assertEquals(1, McpServerHealthStatus.rank("healthy"));
        assertEquals(1, McpServerHealthStatus.rank("disabled"));
        assertEquals(0, McpServerHealthStatus.rank("unknown"));
        assertEquals(McpServerHealthStatus.UNHEALTHY, McpServerHealthStatus.higher("degraded", "unhealthy"));
        assertEquals(McpServerHealthStatus.DEGRADED, McpServerHealthStatus.higher("degraded", "healthy"));
        assertNull(McpServerHealthStatus.higher("unknown", null));
    }

    @Test
    void createsStableEmptyCounts() {
        assertEquals(0, McpServerHealthStatus.emptyCounts().get(McpServerHealthStatus.HEALTHY));
        assertEquals(0, McpServerHealthStatus.emptyCounts().get(McpServerHealthStatus.DEGRADED));
        assertEquals(0, McpServerHealthStatus.emptyCounts().get(McpServerHealthStatus.UNHEALTHY));
        assertEquals(0, McpServerHealthStatus.emptyCounts().get(McpServerHealthStatus.UNSYNCED));
        assertEquals(0, McpServerHealthStatus.emptyCounts().get(McpServerHealthStatus.DISABLED));
    }
}
