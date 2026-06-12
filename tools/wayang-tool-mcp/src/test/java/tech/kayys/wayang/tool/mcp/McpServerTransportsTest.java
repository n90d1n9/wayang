package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerTransportsTest {

    @Test
    void matchesTransportFiltersWithNormalization() {
        assertTrue(McpServerTransports.matches(" HTTP ", "http"));
        assertTrue(McpServerTransports.matches("http", " HTTP "));
        assertTrue(McpServerTransports.matches(McpServerTransports.STDIO, null));
        assertTrue(McpServerTransports.matches(McpServerTransports.STDIO, ""));
        assertFalse(McpServerTransports.matches(McpServerTransports.STDIO, McpServerTransports.HTTP));
    }

    @Test
    void identifiesHttpDiscoveryTransports() {
        assertTrue(McpServerTransports.supportsHttpDiscovery(""));
        assertTrue(McpServerTransports.supportsHttpDiscovery(McpServerTransports.HTTP));
        assertTrue(McpServerTransports.supportsHttpDiscovery(" SSE "));
        assertTrue(McpServerTransports.supportsHttpDiscovery(McpServerTransports.STREAMABLE_HTTP));
        assertTrue(McpServerTransports.supportsHttpDiscovery(McpServerTransports.HTTP_SSE));
        assertFalse(McpServerTransports.supportsHttpDiscovery(McpServerTransports.STDIO));
        assertFalse(McpServerTransports.supportsHttpDiscovery((McpServerRegistry) null));
    }

    @Test
    void readsHttpDiscoveryTransportFromRegistryServer() {
        McpServerRegistry server = new McpServerRegistry();
        server.setTransport("STREAMABLE-HTTP");

        assertTrue(McpServerTransports.supportsHttpDiscovery(server));
    }
}
