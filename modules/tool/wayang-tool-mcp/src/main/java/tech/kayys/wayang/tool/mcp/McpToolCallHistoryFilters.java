package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

record McpToolCallHistoryFilters(
        String toolId,
        String status,
        Boolean success,
        String failureType,
        Instant startedAtFrom,
        Instant startedAtTo,
        Instant finishedAtFrom,
        Instant finishedAtTo,
        Long minDurationMs,
        Long maxDurationMs,
        Long minMcpDurationMs,
        Long maxMcpDurationMs,
        int offset,
        int limit) {

    McpToolCallHistoryFilters {
        McpToolCallHistoryPageWindow pageWindow = McpToolCallHistoryPageWindow.page(offset, limit);
        toolId = McpToolCallHistoryFilterValues.normalizeToolId(toolId);
        status = McpToolCallHistoryFilterValues.normalizeStatus(status);
        failureType = McpToolCallHistoryFilterValues.normalizeFailureType(failureType);
        minDurationMs = McpToolCallHistoryFilterValues.normalizeDurationBound(minDurationMs);
        maxDurationMs = McpToolCallHistoryFilterValues.normalizeDurationBound(maxDurationMs);
        minMcpDurationMs = McpToolCallHistoryFilterValues.normalizeDurationBound(minMcpDurationMs);
        maxMcpDurationMs = McpToolCallHistoryFilterValues.normalizeDurationBound(maxMcpDurationMs);
        offset = pageWindow.offset();
        limit = pageWindow.limit();
    }

    static McpToolCallHistoryFilters of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            int limit) {
        return of(toolId, status, success, failureType, null, limit);
    }

    static McpToolCallHistoryFilters of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            Integer offset,
            int limit) {
        return of(
                toolId,
                status,
                success,
                failureType,
                null,
                null,
                null,
                null,
                offset,
                limit);
    }

    static McpToolCallHistoryFilters of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            String startedAtFrom,
            String startedAtTo,
            String finishedAtFrom,
            String finishedAtTo,
            Integer offset,
            int limit) {
        return of(
                toolId,
                status,
                success,
                failureType,
                startedAtFrom,
                startedAtTo,
                finishedAtFrom,
                finishedAtTo,
                null,
                null,
                null,
                null,
                offset,
                limit);
    }

    static McpToolCallHistoryFilters of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            String startedAtFrom,
            String startedAtTo,
            String finishedAtFrom,
            String finishedAtTo,
            Long minDurationMs,
            Long maxDurationMs,
            Long minMcpDurationMs,
            Long maxMcpDurationMs,
            Integer offset,
            int limit) {
        McpToolCallHistoryPageWindow pageWindow = McpToolCallHistoryPageWindow.page(offset, limit);
        return new McpToolCallHistoryFilters(toolId, status, success, failureType,
                McpHistoryFilterSupport.parseInstant(startedAtFrom),
                McpHistoryFilterSupport.parseInstant(startedAtTo),
                McpHistoryFilterSupport.parseInstant(finishedAtFrom),
                McpHistoryFilterSupport.parseInstant(finishedAtTo),
                minDurationMs,
                maxDurationMs,
                minMcpDurationMs,
                maxMcpDurationMs,
                pageWindow.offset(),
                pageWindow.limit());
    }

    boolean matches(McpToolCallHistoryEntry entry) {
        return McpToolCallHistoryFilterMatcher.from(this).matches(entry);
    }
}
