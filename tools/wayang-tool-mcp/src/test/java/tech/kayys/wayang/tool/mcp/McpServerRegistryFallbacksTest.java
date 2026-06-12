package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerRegistryFallbacksTest {

    @Test
    void serverRegistryInspectionFallbackReturnsEmptyAndMissingEntries() {
        McpServerRegistryInspectionService service =
                McpServerRegistryFallbacks.serverRegistryInspectionService(null);

        assertEquals(List.of(), service.list("tenant-1", null, null)
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service.get("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
    }

    @Test
    void serverLifecycleFallbackReturnsMissingResults() {
        McpServerLifecycleService service = McpServerRegistryFallbacks.serverLifecycleService(null);

        assertNull(service.setEnabled("tenant-1", "docs", true)
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service.impact("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
        assertNull(service.retire("tenant-1", "docs")
                .await().atMost(Duration.ofSeconds(3)));
    }
}
