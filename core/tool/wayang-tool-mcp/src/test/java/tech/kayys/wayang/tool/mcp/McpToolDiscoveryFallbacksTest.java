package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoveryFallbacksTest {

    @Test
    void discoverySyncServiceFallbackReturnsWarningAndEmptyHistory() {
        McpToolDiscoverySyncService service = McpToolDiscoveryFallbacks.discoverySyncService(null);

        McpToolDiscoverySyncResult result = service.syncScheduled()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.scanned());
        assertEquals(0, result.imported());
        assertEquals(List.of("MCP discovery sync service is not configured"), result.warnings());
        assertEquals(List.of(), service.listHistory("tenant-1", null, null, 10)
                .await().atMost(Duration.ofSeconds(3)));
    }
}
