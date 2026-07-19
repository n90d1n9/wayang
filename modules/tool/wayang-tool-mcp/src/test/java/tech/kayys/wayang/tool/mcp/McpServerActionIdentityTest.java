package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerActionIdentityTest {

    @Test
    void parsesServerActionIdentity() {
        McpServerActionIdentity identity = McpServerActionIdentity.parse(" Docs:run-sync ");

        assertEquals(" Docs:run-sync ", identity.actionId());
        assertEquals("Docs", identity.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, identity.actionCode());
        assertEquals("docs:RUN_SYNC", identity.normalizedActionId());
    }

    @Test
    void rejectsMalformedActionIds() {
        assertNull(McpServerActionIdentity.parse(null));
        assertNull(McpServerActionIdentity.parse(" "));
        assertNull(McpServerActionIdentity.parse("docs"));
        assertNull(McpServerActionIdentity.parse(":RUN_SYNC"));
        assertNull(McpServerActionIdentity.parse("docs: "));
    }

    @Test
    void normalizesActionPieces() {
        assertEquals("docs", McpServerActionIdentity.normalizeServerName(" Docs "));
        assertEquals(McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                McpServerActionIdentity.normalizeActionCode("review-stale-tools"));
        assertEquals("docs:RUN_SYNC", McpServerActionIdentity.normalizeActionId(" Docs:run-sync "));
        assertEquals("docs", McpServerActionIdentity.normalizeActionId(" Docs "));
    }
}
