package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolLifecycleTest {

    @Test
    void identifiesServerOwnershipFromCapabilityOrMcpTagPair() {
        tech.kayys.wayang.tool.entity.McpTool capabilityTool = tool(
                Set.of("mcp:docs"),
                Set.of());
        tech.kayys.wayang.tool.entity.McpTool tagTool = tool(
                Set.of(McpToolLifecycle.MCP_TAG),
                Set.of("docs"));
        tech.kayys.wayang.tool.entity.McpTool nonMcpTagTool = tool(
                Set.of("custom"),
                Set.of("docs"));

        assertTrue(McpToolLifecycle.belongsToServer(capabilityTool, "docs"));
        assertTrue(McpToolLifecycle.belongsToServer(tagTool, "docs"));
        assertFalse(McpToolLifecycle.belongsToServer(nonMcpTagTool, "docs"));
    }

    @Test
    void extractsCanonicalServerNameAndLifecycleFlags() {
        tech.kayys.wayang.tool.entity.McpTool tool = tool(
                Set.of("stale", "mcp:docs"),
                Set.of("Docs", McpToolLifecycle.SERVER_DISABLED_TAG, McpToolLifecycle.SERVER_RETIRED_TAG));

        assertEquals("Docs", McpToolLifecycle.serverName(tool));
        assertTrue(McpToolLifecycle.isStale(tool));
        assertTrue(McpToolLifecycle.isServerDisabled(tool));
        assertTrue(McpToolLifecycle.isRetired(tool));
    }

    @Test
    void resolvesLifecycleStateWithExplicitPrecedence() {
        tech.kayys.wayang.tool.entity.McpTool active = tool(Set.of(), Set.of());
        active.setEnabled(true);
        tech.kayys.wayang.tool.entity.McpTool disabled = tool(Set.of(), Set.of());
        disabled.setEnabled(false);
        tech.kayys.wayang.tool.entity.McpTool serverDisabled = tool(
                Set.of(),
                Set.of(McpToolLifecycle.SERVER_DISABLED_TAG));
        serverDisabled.setEnabled(false);
        tech.kayys.wayang.tool.entity.McpTool stale = tool(
                Set.of(McpToolLifecycle.STALE_TAG),
                Set.of(McpToolLifecycle.SERVER_DISABLED_TAG));
        stale.setEnabled(false);
        tech.kayys.wayang.tool.entity.McpTool retired = tool(
                Set.of(McpToolLifecycle.STALE_TAG),
                Set.of(McpToolLifecycle.SERVER_RETIRED_TAG));
        retired.setEnabled(false);

        assertEquals(McpToolLifecycle.LIFECYCLE_ACTIVE, McpToolLifecycle.lifecycleState(active));
        assertEquals(McpToolLifecycle.LIFECYCLE_DISABLED, McpToolLifecycle.lifecycleState(disabled));
        assertEquals(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED, McpToolLifecycle.lifecycleState(serverDisabled));
        assertEquals(McpToolLifecycle.LIFECYCLE_STALE, McpToolLifecycle.lifecycleState(stale));
        assertEquals(McpToolLifecycle.LIFECYCLE_RETIRED, McpToolLifecycle.lifecycleState(retired));
    }

    @Test
    void updatesTagsWithoutDuplicatingCallersLogic() {
        Set<String> withRetired = McpToolLifecycle.withValues(
                Set.of(McpToolLifecycle.MCP_TAG, McpToolLifecycle.SERVER_DISABLED_TAG),
                McpToolLifecycle.STALE_TAG,
                McpToolLifecycle.SERVER_RETIRED_TAG);
        Set<String> cleaned = McpToolLifecycle.withoutValue(
                withRetired,
                McpToolLifecycle.SERVER_DISABLED_TAG);

        assertTrue(cleaned.contains(McpToolLifecycle.MCP_TAG));
        assertTrue(cleaned.contains(McpToolLifecycle.STALE_TAG));
        assertTrue(cleaned.contains(McpToolLifecycle.SERVER_RETIRED_TAG));
        assertFalse(cleaned.contains(McpToolLifecycle.SERVER_DISABLED_TAG));
    }

    @Test
    void buildsImportTagsAndCapabilities() {
        assertEquals(Set.of(
                McpToolLifecycle.MCP_TAG,
                "knowledge",
                "Docs"), McpToolLifecycle.importTags("Docs", "knowledge"));
        assertEquals(Set.of(
                McpToolLifecycle.MCP_TAG,
                McpToolLifecycle.TOOL_TAG,
                McpToolLifecycle.serverCapability("Docs"),
                McpToolLifecycle.READ_CAPABILITY), McpToolLifecycle.importCapabilities("Docs", true));
    }

    private tech.kayys.wayang.tool.entity.McpTool tool(Set<String> capabilities, Set<String> tags) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setCapabilities(capabilities);
        tool.setTags(tags);
        return tool;
    }
}
