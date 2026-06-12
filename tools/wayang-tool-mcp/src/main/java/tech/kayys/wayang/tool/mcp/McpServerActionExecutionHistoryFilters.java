package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

record McpServerActionExecutionHistoryFilters(
        String actionId,
        String serverName,
        String actionCode,
        String status,
        Boolean executed,
        String executionMode,
        String riskLevel,
        Boolean hasWarnings,
        Instant startedAtFrom,
        Instant startedAtTo,
        Instant finishedAtFrom,
        Instant finishedAtTo,
        int offset,
        int limit,
        int scanLimit) {

    McpServerActionExecutionHistoryFilters {
        actionId = McpServerActionExecutionHistoryFilterValues.normalizeActionId(actionId);
        serverName = McpServerActionExecutionHistoryFilterValues.normalizeServerName(serverName);
        actionCode = McpServerActionExecutionHistoryFilterValues.normalizeActionCode(actionCode);
        status = McpServerActionExecutionHistoryFilterValues.normalizeStatus(status);
        executionMode = McpServerActionExecutionHistoryFilterValues.normalizeExecutionMode(executionMode);
        riskLevel = McpServerActionExecutionHistoryFilterValues.normalizeRiskLevel(riskLevel);
    }

    static McpServerActionExecutionHistoryFilters of(
            String serverName,
            String actionCode,
            String status,
            int limit) {
        return of(null, serverName, actionCode, status, null, null, null, null, limit);
    }

    static McpServerActionExecutionHistoryFilters of(
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

    static McpServerActionExecutionHistoryFilters of(
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

    static McpServerActionExecutionHistoryFilters of(
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

    static McpServerActionExecutionHistoryFilters latest(
            String serverName,
            String actionCode,
            String status,
            int limit) {
        return latest(null, serverName, actionCode, status, null, null, null, null, limit);
    }

    static McpServerActionExecutionHistoryFilters latest(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            Boolean executed,
            String executionMode,
            String riskLevel,
            Boolean hasWarnings,
            int limit) {
        return latest(
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

    static McpServerActionExecutionHistoryFilters latest(
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
        return latest(
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

    static McpServerActionExecutionHistoryFilters latest(
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
                .latest()
                .build();
    }

    static McpServerActionExecutionHistoryFilterBuilder builder() {
        return new McpServerActionExecutionHistoryFilterBuilder();
    }

    boolean matches(McpServerActionExecutionHistoryEntry entry) {
        return McpServerActionExecutionHistoryFilterMatcher.from(this).matches(entry);
    }

}
