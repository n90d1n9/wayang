package tech.kayys.wayang.tool.mcp;

record McpServerActionExecutionHistoryQuery(
        String actionId,
        String serverName,
        String actionCode,
        String status,
        Boolean executed,
        String executionMode,
        String riskLevel,
        Boolean hasWarnings,
        String startedAtFrom,
        String startedAtTo,
        String finishedAtFrom,
        String finishedAtTo,
        Integer offset,
        int limit) {

    static McpServerActionExecutionHistoryQuery of(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return of(
                actionId,
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                null,
                null,
                null,
                null,
                limit);
    }

    static McpServerActionExecutionHistoryQuery of(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            String startedAtFrom,
            String startedAtTo,
            String finishedAtFrom,
            String finishedAtTo,
            int limit) {
        return of(
                actionId,
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                startedAtFrom,
                startedAtTo,
                finishedAtFrom,
                finishedAtTo,
                null,
                limit);
    }

    static McpServerActionExecutionHistoryQuery of(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            String startedAtFrom,
            String startedAtTo,
            String finishedAtFrom,
            String finishedAtTo,
            Integer offset,
            int limit) {
        return builder()
                .withActionId(actionId)
                .withServerName(serverName)
                .withActionCode(actionCode)
                .withStatus(status)
                .withExecuted(executed)
                .withExecutionMode(executionMode)
                .withRiskLevel(riskLevel)
                .withWarnings(hasWarnings)
                .withStartedAtFrom(startedAtFrom)
                .withStartedAtTo(startedAtTo)
                .withFinishedAtFrom(finishedAtFrom)
                .withFinishedAtTo(finishedAtTo)
                .withOffset(offset)
                .withLimit(limit)
                .build();
    }

    static McpServerActionExecutionHistoryQueryBuilder builder() {
        return new McpServerActionExecutionHistoryQueryBuilder();
    }

    McpServerActionExecutionHistoryFilters filters() {
        return filterBuilder().build();
    }

    McpServerActionExecutionHistoryFilters latestFilters() {
        return filterBuilder()
                .latest()
                .build();
    }

    private McpServerActionExecutionHistoryFilterBuilder filterBuilder() {
        return McpServerActionExecutionHistoryFilters.builder()
                .withActionId(actionId)
                .withServerName(serverName)
                .withActionCode(actionCode)
                .withStatus(status)
                .withExecuted(executed)
                .withExecutionMode(executionMode)
                .withRiskLevel(riskLevel)
                .withWarnings(hasWarnings)
                .withStartedAtFrom(startedAtFrom)
                .withStartedAtTo(startedAtTo)
                .withFinishedAtFrom(finishedAtFrom)
                .withFinishedAtTo(finishedAtTo)
                .withOffset(offset)
                .withLimit(limit);
    }
}
