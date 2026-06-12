package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionPreviewStatusTest {

    @Test
    void derivesStatusFromOperationShape() {
        assertEquals(McpServerActionPreviewStatus.MANUAL,
                McpServerActionPreviewStatus.forAction(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        false,
                        true), false));
        assertEquals(McpServerActionPreviewStatus.AUTOMATABLE,
                McpServerActionPreviewStatus.forAction(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        true,
                        true), true));
        assertEquals(McpServerActionPreviewStatus.REVIEW_REQUIRED,
                McpServerActionPreviewStatus.forAction(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        false,
                        true), true));
    }

    @Test
    void describesKnownStatuses() {
        assertEquals("Action has a structured operation and is marked safe to automate.",
                McpServerActionPreviewStatus.reason(McpServerActionPreviewStatus.AUTOMATABLE));
        assertEquals("Action has a structured operation but requires review before execution.",
                McpServerActionPreviewStatus.reason(McpServerActionPreviewStatus.REVIEW_REQUIRED));
        assertEquals("Action has no structured operation and must be handled manually.",
                McpServerActionPreviewStatus.reason(McpServerActionPreviewStatus.MANUAL));
        assertEquals("Action preview is unavailable.",
                McpServerActionPreviewStatus.reason(McpServerActionPreviewStatus.NOT_FOUND));
    }
}
