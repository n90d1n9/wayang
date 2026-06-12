package tech.kayys.wayang.tool.mcp;

record McpToolCallHistoryPageWindow(
        int offset,
        int limit) {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1_000;
    private static final int DEFAULT_OFFSET = 0;
    private static final int MAX_OFFSET = 10_000;
    private static final McpHistoryPageWindowLimits LIMITS = new McpHistoryPageWindowLimits(
            DEFAULT_OFFSET,
            MAX_OFFSET,
            DEFAULT_LIMIT,
            MAX_LIMIT,
            MAX_LIMIT);

    static McpToolCallHistoryPageWindow page(Integer offset, int limit) {
        McpHistoryPageWindow pageWindow = McpHistoryPageWindows.bounded(offset, limit, LIMITS);
        return new McpToolCallHistoryPageWindow(
                pageWindow.offset(),
                pageWindow.limit());
    }
}
