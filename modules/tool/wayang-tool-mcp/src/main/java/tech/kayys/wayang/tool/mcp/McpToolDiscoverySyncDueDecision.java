package tech.kayys.wayang.tool.mcp;

record McpToolDiscoverySyncDueDecision(boolean due, String warning) {

    static McpToolDiscoverySyncDueDecision dueNow() {
        return new McpToolDiscoverySyncDueDecision(true, null);
    }

    static McpToolDiscoverySyncDueDecision skip() {
        return new McpToolDiscoverySyncDueDecision(false, null);
    }

    static McpToolDiscoverySyncDueDecision warning(String warning) {
        return new McpToolDiscoverySyncDueDecision(false, warning);
    }
}
