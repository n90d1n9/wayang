package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolCallProtocolTest {

    @Test
    void buildsToolsCallParams() {
        assertEquals(Map.of(
                McpToolCallProtocol.FIELD_NAME, "search",
                McpToolCallProtocol.FIELD_ARGUMENTS, Map.of("q", "wayang")),
                McpToolCallProtocol.callParams("search", Map.of("q", "wayang")));
    }

    @Test
    void parsesTextContentAndToolErrors() {
        McpToolCallProtocol.ToolCallPayload result = McpToolCallProtocol.parse(Map.of(
                McpToolCallProtocol.FIELD_IS_ERROR, true,
                McpToolCallProtocol.FIELD_CONTENT, List.of(
                        Map.of(
                                McpToolCallProtocol.FIELD_TYPE, McpToolCallProtocol.CONTENT_TYPE_TEXT,
                                McpToolCallProtocol.FIELD_TEXT, "one"),
                        Map.of(
                                McpToolCallProtocol.FIELD_TYPE, McpToolCallProtocol.CONTENT_TYPE_TEXT,
                                McpToolCallProtocol.FIELD_TEXT, "two"))));

        assertTrue(result.toolError());
        assertEquals("one\ntwo", result.text());
    }

    @Test
    void acceptsStructuredContentOnlyPayloads() {
        McpToolCallProtocol.ToolCallPayload result = McpToolCallProtocol.parse(Map.of(
                McpToolCallProtocol.FIELD_STRUCTURED_CONTENT, Map.of("answer", 42)));

        assertFalse(result.toolError());
        assertEquals("{structuredContent={answer=42}}", result.text());
        assertEquals(Map.of("answer", 42), McpMaps.fromObject(result.result().get(
                McpToolCallProtocol.FIELD_STRUCTURED_CONTENT)));
    }

    @Test
    void rejectsMalformedContent() {
        McpToolCallProtocolException error = assertThrows(
                McpToolCallProtocolException.class,
                () -> McpToolCallProtocol.parse(Map.of(McpToolCallProtocol.FIELD_CONTENT, "not-an-array")));

        assertEquals("MCP tools/call content must be an array", error.getMessage());
    }
}
