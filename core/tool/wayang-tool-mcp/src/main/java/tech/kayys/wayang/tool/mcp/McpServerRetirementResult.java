package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpServerRetirementResult(
        McpServerRegistryEntry server,
        List<String> retiredToolIds) {

    public int affectedTools() {
        return retiredToolIds.size();
    }
}
