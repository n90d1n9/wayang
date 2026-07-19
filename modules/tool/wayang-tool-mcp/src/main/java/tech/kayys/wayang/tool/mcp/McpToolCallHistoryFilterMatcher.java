package tech.kayys.wayang.tool.mcp;

record McpToolCallHistoryFilterMatcher(McpToolCallHistoryFilters filters) {

    static McpToolCallHistoryFilterMatcher from(McpToolCallHistoryFilters filters) {
        return new McpToolCallHistoryFilterMatcher(filters);
    }

    boolean matches(McpToolCallHistoryEntry entry) {
        if (entry == null) {
            return false;
        }
        return matchesToolId(entry)
                && matchesStatus(entry)
                && matchesSuccess(entry)
                && matchesFailureType(entry)
                && matchesStartedAt(entry)
                && matchesFinishedAt(entry)
                && matchesDuration(entry)
                && matchesMcpDuration(entry);
    }

    private boolean matchesToolId(McpToolCallHistoryEntry entry) {
        return filters.toolId() == null || filters.toolId().equals(entry.toolId());
    }

    private boolean matchesStatus(McpToolCallHistoryEntry entry) {
        return filters.status() == null
                || filters.status().equals(McpToolCallHistoryFilterValues.normalizeStatus(entry.status()));
    }

    private boolean matchesSuccess(McpToolCallHistoryEntry entry) {
        return filters.success() == null || filters.success() == entry.success();
    }

    private boolean matchesFailureType(McpToolCallHistoryEntry entry) {
        return filters.failureType() == null
                || filters.failureType().equals(
                        McpToolCallHistoryFilterValues.normalizeFailureType(entry.failureType()));
    }

    private boolean matchesStartedAt(McpToolCallHistoryEntry entry) {
        return McpHistoryFilterSupport.matchesInstantRange(
                entry.startedAt(),
                filters.startedAtFrom(),
                filters.startedAtTo());
    }

    private boolean matchesFinishedAt(McpToolCallHistoryEntry entry) {
        return McpHistoryFilterSupport.matchesInstantRange(
                entry.finishedAt(),
                filters.finishedAtFrom(),
                filters.finishedAtTo());
    }

    private boolean matchesDuration(McpToolCallHistoryEntry entry) {
        return matchesLongRange(entry.durationMs(), filters.minDurationMs(), filters.maxDurationMs());
    }

    private boolean matchesMcpDuration(McpToolCallHistoryEntry entry) {
        return matchesLongRange(entry.mcpDurationMs(), filters.minMcpDurationMs(), filters.maxMcpDurationMs());
    }

    private static boolean matchesLongRange(long candidate, Long min, Long max) {
        return (min == null || candidate >= min)
                && (max == null || candidate <= max);
    }
}
