package tech.kayys.wayang.tool.mcp;

record McpToolCallHistoryQuery(
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

    static McpToolCallHistoryQuery of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            int limit) {
        return of(toolId, status, success, failureType, null, limit);
    }

    static McpToolCallHistoryQuery of(
            String toolId,
            String status,
            Boolean success,
            String failureType,
            Integer offset,
            int limit) {
        return of(toolId, status, success, failureType, null, null, null, null, offset, limit);
    }

    static McpToolCallHistoryQuery of(
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

    static McpToolCallHistoryQuery of(
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
        return builder()
                .withToolId(toolId)
                .withStatus(status)
                .withSuccess(success)
                .withFailureType(failureType)
                .withStartedAtFrom(startedAtFrom)
                .withStartedAtTo(startedAtTo)
                .withFinishedAtFrom(finishedAtFrom)
                .withFinishedAtTo(finishedAtTo)
                .withDurationWindow(minDurationMs, maxDurationMs)
                .withMcpDurationWindow(minMcpDurationMs, maxMcpDurationMs)
                .withOffset(offset)
                .withLimit(limit)
                .build();
    }

    static McpToolCallHistoryQueryBuilder builder() {
        return new McpToolCallHistoryQueryBuilder();
    }

    McpToolCallHistoryFilters filters() {
        return McpToolCallHistoryFilters.of(
                toolId,
                status,
                success,
                failureType,
                startedAtFrom,
                startedAtTo,
                finishedAtFrom,
                finishedAtTo,
                minDurationMs,
                maxDurationMs,
                minMcpDurationMs,
                maxMcpDurationMs,
                offset,
                limit);
    }
}
