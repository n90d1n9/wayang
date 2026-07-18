package tech.kayys.wayang.tool.mcp;

import java.util.ArrayList;
import java.util.List;

final class McpToolDiscoveryImportChanges {

    private final List<String> toolIds;
    private final List<String> reactivatedToolIds;
    private final List<String> skippedRetiredToolIds;

    private McpToolDiscoveryImportChanges() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    private McpToolDiscoveryImportChanges(
            List<String> toolIds,
            List<String> reactivatedToolIds,
            List<String> skippedRetiredToolIds) {
        this.toolIds = new ArrayList<>(toolIds);
        this.reactivatedToolIds = new ArrayList<>(reactivatedToolIds);
        this.skippedRetiredToolIds = new ArrayList<>(skippedRetiredToolIds);
    }

    static McpToolDiscoveryImportChanges empty() {
        return new McpToolDiscoveryImportChanges();
    }

    void add(McpToolDiscoveryImportChange change) {
        if (change.skippedRetired()) {
            skippedRetiredToolIds.add(change.skippedRetiredToolId());
            return;
        }
        String toolId = change.tool().getToolId();
        toolIds.add(toolId);
        if (change.reactivated()) {
            reactivatedToolIds.add(toolId);
        }
    }

    List<String> toolIds() {
        return List.copyOf(toolIds);
    }

    List<String> reactivatedToolIds() {
        return List.copyOf(reactivatedToolIds);
    }

    List<String> skippedRetiredToolIds() {
        return List.copyOf(skippedRetiredToolIds);
    }

    McpToolDiscoveryImportChanges copy() {
        return new McpToolDiscoveryImportChanges(
                toolIds(),
                reactivatedToolIds(),
                skippedRetiredToolIds());
    }
}
