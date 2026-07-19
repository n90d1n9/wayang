package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpToolDiscoverySyncResult(
        int scanned,
        int imported,
        int stale,
        int reactivated,
        List<String> warnings) {

    public McpToolDiscoverySyncResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public McpToolDiscoverySyncResult(
            int scanned,
            int imported,
            List<String> warnings) {
        this(scanned, imported, 0, 0, warnings);
    }
}
