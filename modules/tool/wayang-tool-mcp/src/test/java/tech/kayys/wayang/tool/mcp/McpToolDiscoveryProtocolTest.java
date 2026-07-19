package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoveryProtocolTest {

    @Test
    void buildsInitializeAndToolsListParams() {
        assertEquals(Map.of(
                McpToolDiscoveryProtocol.FIELD_PROTOCOL_VERSION, "2025-11-25",
                McpToolDiscoveryProtocol.FIELD_CAPABILITIES, Map.of(),
                McpToolDiscoveryProtocol.FIELD_CLIENT_INFO, Map.of(
                        McpToolDiscoveryProtocol.FIELD_NAME, "wayang-test",
                        McpToolDiscoveryProtocol.FIELD_VERSION, "1.2.3")),
                McpToolDiscoveryProtocol.initializeParams("2025-11-25", "wayang-test", "1.2.3"));

        assertEquals(Map.of(), McpToolDiscoveryProtocol.toolsListParams(null));
        assertEquals(Map.of(), McpToolDiscoveryProtocol.toolsListParams(""));
        assertEquals(Map.of(McpToolDiscoveryProtocol.FIELD_CURSOR, "next"),
                McpToolDiscoveryProtocol.toolsListParams("next"));
    }

    @Test
    void parsesInitializeAndToolsListPayloads() {
        assertEquals("2025-11-25", McpToolDiscoveryProtocol.initializeProtocolVersion(
                Map.of(McpToolDiscoveryProtocol.FIELD_PROTOCOL_VERSION, "2025-11-25"),
                "fallback"));

        McpToolDiscoveryProtocol.ToolsListPayload result = McpToolDiscoveryProtocol.toolsList(Map.of(
                McpToolDiscoveryProtocol.FIELD_TOOLS, java.util.List.of(Map.of(
                        McpToolDiscoveryProtocol.FIELD_NAME, "search")),
                McpToolDiscoveryProtocol.FIELD_NEXT_CURSOR, "next"));

        assertEquals("next", result.nextCursor());
        assertEquals(1, result.tools().size());
        assertEquals("search", McpMaps.fromObject(result.tools().getFirst()).get(
                McpToolDiscoveryProtocol.FIELD_NAME));
    }

    @Test
    void readsReadOnlyHintFromDiscoveryAnnotations() {
        Map<String, Object> metadata = Map.of(
                McpToolDiscoveryProtocol.FIELD_ANNOTATIONS,
                Map.of(McpToolDiscoveryProtocol.FIELD_READ_ONLY_HINT, true));
        McpDiscoveredTool tool = new McpDiscoveredTool(
                "docs:search",
                "search",
                "Search",
                "Search docs",
                Map.of(),
                Map.of(),
                metadata);

        assertTrue(McpToolDiscoveryProtocol.readOnlyHint(metadata));
        assertTrue(McpToolDiscoveryProtocol.readOnlyHint(tool));
        assertFalse(McpToolDiscoveryProtocol.readOnlyHint(Map.of(
                McpToolDiscoveryProtocol.FIELD_ANNOTATIONS,
                Map.of(McpToolDiscoveryProtocol.FIELD_READ_ONLY_HINT, false))));
        assertFalse(McpToolDiscoveryProtocol.readOnlyHint(Map.of(
                McpToolDiscoveryProtocol.FIELD_ANNOTATIONS,
                Map.of(McpToolDiscoveryProtocol.FIELD_READ_ONLY_HINT, "true"))));
        assertFalse(McpToolDiscoveryProtocol.readOnlyHint((McpDiscoveredTool) null));
    }
}
