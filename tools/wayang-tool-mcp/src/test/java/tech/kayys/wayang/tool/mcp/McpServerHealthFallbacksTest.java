package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerHealthFallbacksTest {

    @Test
    void serverHealthServiceFallbackReturnsUnavailableHealth() {
        McpToolServerHealth health = McpServerHealthFallbacks.serverHealthService(null)
                .summarize("tenant-1", McpServerHealthFilters.byServerName(null))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, health.totalServers());
        assertEquals(List.of("MCP server health service is not configured"), health.warnings());
    }
}
