package tech.kayys.wayang.tool.mcp;

final class McpHistoryPageWindows {
    private McpHistoryPageWindows() {
    }

    static McpHistoryPageWindow bounded(
            Integer offset,
            int limit,
            McpHistoryPageWindowLimits limits) {
        int boundedOffset = boundedOffset(offset, limits);
        int boundedLimit = boundedLimit(limit, limits);
        return new McpHistoryPageWindow(boundedOffset, boundedLimit, boundedLimit);
    }

    static McpHistoryPageWindow page(
            Integer offset,
            int limit,
            McpHistoryPageWindowLimits limits) {
        int boundedOffset = boundedOffset(offset, limits);
        int boundedLimit = boundedLimit(limit, limits);
        return new McpHistoryPageWindow(
                boundedOffset,
                boundedLimit,
                cappedScanLimit(boundedOffset + boundedLimit + 1, limits));
    }

    static McpHistoryPageWindow latest(
            Integer offset,
            int limit,
            McpHistoryPageWindowLimits limits,
            int multiplier) {
        int boundedOffset = boundedOffset(offset, limits);
        int boundedLimit = boundedLimit(limit, limits);
        int pageEnd = boundedOffset + boundedLimit + 1;
        return new McpHistoryPageWindow(
                boundedOffset,
                boundedLimit,
                expandedScanLimit(pageEnd, limits, multiplier));
    }

    static int boundedLimit(int limit, McpHistoryPageWindowLimits limits) {
        return McpHistoryFilterSupport.boundedPageLimit(
                limit,
                limits.defaultLimit(),
                limits.maxLimit());
    }

    static int filteredScanLimit(
            int boundedLimit,
            McpHistoryPageWindowLimits limits,
            int multiplier,
            boolean hasFilters) {
        return hasFilters
                ? expandedScanLimit(boundedLimit, limits, multiplier)
                : boundedLimit;
    }

    static int expandedScanLimit(
            int base,
            McpHistoryPageWindowLimits limits,
            int multiplier) {
        int effectiveMultiplier = Math.max(1, multiplier);
        long expanded = (long) Math.max(0, base) * effectiveMultiplier;
        int requested = (int) Math.min(Integer.MAX_VALUE, Math.max(base, expanded));
        return cappedScanLimit(requested, limits);
    }

    private static int boundedOffset(Integer offset, McpHistoryPageWindowLimits limits) {
        return McpHistoryFilterSupport.boundedPageOffset(
                offset,
                limits.defaultOffset(),
                limits.maxOffset());
    }

    private static int cappedScanLimit(int scanLimit, McpHistoryPageWindowLimits limits) {
        return Math.min(limits.maxScanLimit(), Math.max(0, scanLimit));
    }
}

record McpHistoryPageWindow(
        int offset,
        int limit,
        int scanLimit) {
}

record McpHistoryPageWindowLimits(
        int defaultOffset,
        int maxOffset,
        int defaultLimit,
        int maxLimit,
        int maxScanLimit) {
}
