package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerActionExecutionModeTest {

    @Test
    void normalizesExecutionModeValues() {
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionExecutionMode.normalize(" automatable "));
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED,
                McpServerActionExecutionMode.normalize("review-required"));
        assertNull(McpServerActionExecutionMode.normalize(" "));
    }

    @Test
    void resolvesDefaultModeFromOperationShape() {
        assertEquals(McpServerActionExecutionMode.MANUAL,
                McpServerActionExecutionMode.resolve(null, true, false));
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionExecutionMode.resolve(null, true, true));
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED,
                McpServerActionExecutionMode.resolve(null, false, true));
    }

    @Test
    void explicitModeWinsOverDefaultInference() {
        assertEquals(McpServerActionExecutionMode.REVIEW_REQUIRED,
                McpServerActionExecutionMode.resolve("review-required", true, true));
    }
}
