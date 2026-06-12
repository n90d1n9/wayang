package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

final class McpToolDiscoverySyncHistorySource {

    static final String MCP_TOOLS = "MCP_TOOLS";

    private McpToolDiscoverySyncHistorySource() {
    }

    static boolean isMcpToolHistory(RegistrySyncHistory history) {
        return history != null && MCP_TOOLS.equals(history.getSourceKind());
    }
}
