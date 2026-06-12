package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolDiscoveryImportChangesTest {

    @Test
    void tracksImportedReactivatedAndSkippedRetiredToolIds() {
        McpToolDiscoveryImportChanges changes = McpToolDiscoveryImportChanges.empty();

        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:search"), false));
        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:lookup"), true));
        changes.add(McpToolDiscoveryImportChange.skippedRetired("docs:retired"));

        assertEquals(List.of("docs:search", "docs:lookup"), changes.toolIds());
        assertEquals(List.of("docs:lookup"), changes.reactivatedToolIds());
        assertEquals(List.of("docs:retired"), changes.skippedRetiredToolIds());
    }

    @Test
    void copyReturnsSnapshotOfCurrentState() {
        McpToolDiscoveryImportChanges changes = McpToolDiscoveryImportChanges.empty();
        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:search"), false));

        McpToolDiscoveryImportChanges snapshot = changes.copy();
        changes.add(McpToolDiscoveryImportChange.imported(tool("docs:lookup"), false));
        snapshot.add(McpToolDiscoveryImportChange.imported(tool("docs:snapshot"), false));

        assertEquals(List.of("docs:search", "docs:snapshot"), snapshot.toolIds());
        assertEquals(List.of("docs:search", "docs:lookup"), changes.toolIds());
    }

    private tech.kayys.wayang.tool.entity.McpTool tool(String toolId) {
        tech.kayys.wayang.tool.entity.McpTool tool = new tech.kayys.wayang.tool.entity.McpTool();
        tool.setToolId(toolId);
        return tool;
    }
}
