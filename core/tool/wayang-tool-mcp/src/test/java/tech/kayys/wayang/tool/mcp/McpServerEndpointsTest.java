package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerEndpointsTest {

    @Test
    void resolvesRegistryEndpointFromUrlBeforeCommand() {
        McpServerRegistry server = new McpServerRegistry();
        server.setUrl("http://docs.local/mcp");
        server.setCommand("node server.js");

        assertEquals("http://docs.local/mcp", McpServerEndpoints.endpoint(server));
        assertEquals("http://docs.local/mcp", McpServerEndpoints.url(server));
    }

    @Test
    void fallsBackToCommandWhenUrlIsMissing() {
        McpServerRegistry server = new McpServerRegistry();
        server.setTransport(McpServerTransports.STDIO);
        server.setCommand("node files.js");

        assertEquals("node files.js", McpServerEndpoints.endpoint(server));
        assertNull(McpServerEndpoints.url(server));
    }

    @Test
    void treatsBlankUrlAsMissing() {
        McpServerRegistry server = new McpServerRegistry();
        server.setUrl(" ");
        server.setCommand("node files.js");

        assertEquals("node files.js", McpServerEndpoints.endpoint(server));
        assertNull(McpServerEndpoints.url(server));
        assertNull(McpServerEndpoints.endpoint(null));
    }
}
