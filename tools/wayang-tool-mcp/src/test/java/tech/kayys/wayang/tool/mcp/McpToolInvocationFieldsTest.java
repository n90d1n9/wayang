package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpToolInvocationFieldsTest {

    @Test
    void readsInvocationKeysFromContext() {
        Map<String, Object> context = Map.of(
                McpToolInvocationFields.KEY_TOOL_ID, "docs:search",
                McpToolInvocationFields.KEY_ARGUMENTS, Map.of("q", "wayang"),
                McpToolInvocationFields.KEY_CUSTOM_DATA, Map.of("tenant", "demo"));

        assertEquals("docs:search", McpToolInvocationFields.toolId(context));
        assertEquals(Map.of("q", "wayang"), McpToolInvocationFields.arguments(context));
        assertEquals(Map.of("tenant", "demo"), McpToolInvocationFields.customData(context));
    }

    @Test
    void treatsMissingOrInvalidValuesAsEmpty() {
        assertNull(McpToolInvocationFields.toolId(null));
        assertEquals(Map.of(), McpToolInvocationFields.arguments(null));
        assertEquals(Map.of(), McpToolInvocationFields.customData(Map.of(
                McpToolInvocationFields.KEY_CUSTOM_DATA, "not-a-map")));
    }
}
