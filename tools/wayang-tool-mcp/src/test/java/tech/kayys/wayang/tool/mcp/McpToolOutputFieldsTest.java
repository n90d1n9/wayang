package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolOutputFieldsTest {

    @Test
    void buildsSuccessfulExecutorOutput() {
        Map<String, Object> output = McpToolOutputFields.executorOutput(
                "docs:search",
                McpToolCallResult.success(Map.of(McpToolCallProtocol.FIELD_TEXT, "found"), 12));

        assertEquals(true, output.get(McpToolOutputFields.KEY_SUCCESS));
        assertEquals(McpToolOutputFields.STATUS_SUCCESS, output.get(McpToolOutputFields.KEY_STATUS));
        assertEquals(McpToolOutputFields.PROTOCOL, output.get(McpToolOutputFields.KEY_PROTOCOL));
        assertEquals("docs:search", output.get(McpToolOutputFields.KEY_TOOL_ID));
        assertEquals(12L, output.get(McpToolOutputFields.KEY_DURATION_MS));
        assertEquals("found", output.get(McpToolOutputFields.KEY_TEXT));
        assertTrue(output.containsKey(McpToolOutputFields.KEY_RESULT));
    }

    @Test
    void buildsFailureExecutorOutput() {
        Map<String, Object> output = McpToolOutputFields.executorOutput(
                "docs:search",
                McpToolCallResult.failure(null, 7, McpFailureType.metadata(McpFailureType.HTTP)));

        assertEquals(false, output.get(McpToolOutputFields.KEY_SUCCESS));
        assertEquals(McpToolOutputFields.STATUS_FAILURE, output.get(McpToolOutputFields.KEY_STATUS));
        assertEquals(McpToolOutputFields.DEFAULT_ERROR, output.get(McpToolOutputFields.KEY_ERROR));
        assertEquals(McpFailureType.HTTP.name(), output.get(McpFailureType.METADATA_KEY));
        assertEquals(McpFailureType.HTTP.name(), McpMaps.fromObject(
                output.get(McpToolOutputFields.KEY_METADATA)).get(McpFailureType.METADATA_KEY));
    }
}
