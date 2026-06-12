package tech.kayys.wayang.tool.mcp;

import java.util.List;

record McpToolDiscoveryServerSyncResult(
        int imported,
        int stale,
        int reactivated,
        List<String> warnings) {

    McpToolDiscoveryServerSyncResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    static McpToolDiscoveryServerSyncResult success(McpToolDiscoveryImportResult result) {
        return new McpToolDiscoveryServerSyncResult(
                result.imported(),
                result.stale(),
                result.reactivated(),
                List.of());
    }

    static McpToolDiscoveryServerSyncResult error(String warning) {
        return new McpToolDiscoveryServerSyncResult(0, 0, 0, List.of(warning));
    }
}
