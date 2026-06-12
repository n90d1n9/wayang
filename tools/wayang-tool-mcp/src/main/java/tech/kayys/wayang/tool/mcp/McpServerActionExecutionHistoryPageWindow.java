package tech.kayys.wayang.tool.mcp;

record McpServerActionExecutionHistoryPageWindow(
        int offset,
        int limit,
        int scanLimit) {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;
    static final int DEFAULT_OFFSET = 0;
    static final int MAX_OFFSET = 500;
    static final int MAX_SCAN_LIMIT = 500;
    private static final int LATEST_SCAN_MULTIPLIER = 10;
    private static final McpHistoryPageWindowLimits LIMITS = new McpHistoryPageWindowLimits(
            DEFAULT_OFFSET,
            MAX_OFFSET,
            DEFAULT_LIMIT,
            MAX_LIMIT,
            MAX_SCAN_LIMIT);

    static McpServerActionExecutionHistoryPageWindow page(Integer offset, int limit) {
        McpHistoryPageWindow pageWindow = McpHistoryPageWindows.page(offset, limit, LIMITS);
        return new McpServerActionExecutionHistoryPageWindow(
                pageWindow.offset(),
                pageWindow.limit(),
                pageWindow.scanLimit());
    }

    static McpServerActionExecutionHistoryPageWindow latest(Integer offset, int limit) {
        McpHistoryPageWindow pageWindow = McpHistoryPageWindows.latest(
                offset,
                limit,
                LIMITS,
                LATEST_SCAN_MULTIPLIER);
        return new McpServerActionExecutionHistoryPageWindow(
                pageWindow.offset(),
                pageWindow.limit(),
                pageWindow.scanLimit());
    }
}
