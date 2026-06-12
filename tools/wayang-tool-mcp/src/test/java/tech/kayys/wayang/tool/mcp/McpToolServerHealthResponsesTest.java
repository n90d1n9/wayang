package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolServerHealthResponsesTest {

    @Test
    void emptyResponseHasZeroCountsAndNoWarnings() {
        McpToolServerHealth health = McpToolServerHealthResponses.empty();

        assertEquals(0, health.totalServers());
        assertEquals(0, health.totalTools());
        assertEquals(0, health.actionQueueTotal());
        assertEquals(0, health.healthStatusCounts().get(McpServerHealthStatus.HEALTHY));
        assertEquals(0, health.lifecycleStates().get(McpToolLifecycle.LIFECYCLE_ACTIVE));
        assertEquals(List.of(), health.actionQueue());
        assertEquals(List.of(), health.servers());
        assertEquals(List.of(), health.warnings());
    }

    @Test
    void unavailableResponseCarriesSingleWarning() {
        McpToolServerHealth health = McpToolServerHealthResponses.unavailable("not configured");

        assertEquals(0, health.totalServers());
        assertEquals(List.of("not configured"), health.warnings());
    }

    @Test
    void namedUnavailableResponsesUseStableWarningMessages() {
        assertEquals(
                List.of(McpToolServerHealthResponses.WARNING_REGISTRY_DATABASE_MODE_DISABLED),
                McpToolServerHealthResponses.registryDatabaseModeDisabled().warnings());
        assertEquals(
                List.of(McpToolServerHealthResponses.WARNING_REGISTRY_NOT_CONFIGURED),
                McpToolServerHealthResponses.registryNotConfigured().warnings());
    }
}
