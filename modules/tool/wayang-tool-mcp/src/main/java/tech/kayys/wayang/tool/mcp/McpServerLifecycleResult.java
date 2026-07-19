package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpServerLifecycleResult(
        McpServerRegistryEntry server,
        List<String> disabledToolIds,
        List<String> reactivatedToolIds) {

    public int affectedTools() {
        return disabledToolIds.size() + reactivatedToolIds.size();
    }
}
