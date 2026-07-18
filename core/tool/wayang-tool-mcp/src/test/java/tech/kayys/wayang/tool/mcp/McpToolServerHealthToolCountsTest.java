package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.tool;

class McpToolServerHealthToolCountsTest {

    @Test
    void groupsLifecycleCountsByNormalizedServerName() {
        Map<String, McpToolLifecycleCounts> counts = McpToolServerHealthToolCounts.byServer(List.of(
                tool("docs.search", true, Set.of("mcp", "tool", "mcp:docs"), Set.of("DOCS")),
                tool("docs.old", false, Set.of("mcp", "tool", "mcp:docs", "stale"), Set.of("docs")),
                tool("docs.paused", false, Set.of("mcp", "tool", "mcp:docs"),
                        Set.of("docs", McpToolLifecycle.SERVER_DISABLED_TAG)),
                tool("crm.lookup", true, Set.of("mcp", "tool", "mcp:crm"), Set.of("crm")),
                tool("plain", true, Set.of("mcp", "tool"), Set.of("plain"))));

        McpToolLifecycleCounts docs = counts.get("docs");
        McpToolLifecycleCounts crm = counts.get("crm");

        assertEquals(3, docs.total());
        assertEquals(1, docs.enabled());
        assertEquals(2, docs.disabled());
        assertEquals(1, docs.stale());
        assertEquals(1, docs.serverDisabled());
        assertEquals(1, docs.active());
        assertEquals(1, crm.total());
        assertEquals(1, crm.active());
        assertFalse(counts.containsKey("plain"));
    }

    @Test
    void exposesStableServerKeyNormalization() {
        assertEquals("docs", McpToolServerHealthToolCounts.serverKey("DOCS"));
        assertEquals("", McpToolServerHealthToolCounts.serverKey(null));
    }
}
