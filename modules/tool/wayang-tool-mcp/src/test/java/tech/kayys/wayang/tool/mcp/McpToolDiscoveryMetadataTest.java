package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoveryMetadataTest {

    @Test
    void buildsDiscoveryMetadataMaps() {
        assertEquals(Map.of(McpToolDiscoveryMetadata.PAGES, 3), McpToolDiscoveryMetadata.pages(3));
        assertEquals(Map.of(
                McpHttpMetadata.ENDPOINT, "http://localhost/mcp",
                McpToolDiscoveryMetadata.PAGES, 2,
                McpToolDiscoveryMetadata.TOOL_COUNT, 5),
                McpToolDiscoveryMetadata.success("http://localhost/mcp", 2, 5));
    }
}
