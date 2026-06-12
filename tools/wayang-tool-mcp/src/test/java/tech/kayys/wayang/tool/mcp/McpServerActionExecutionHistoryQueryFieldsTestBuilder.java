package tech.kayys.wayang.tool.mcp;

abstract class McpServerActionExecutionHistoryQueryFieldsTestBuilder<
        T extends McpServerActionExecutionHistoryQueryFieldsTestBuilder<T>> {
    private String actionId;
    private String serverName;
    private String actionCode;
    private String status;
    private Boolean executed;
    private String executionMode;
    private String riskLevel;
    private Boolean hasWarnings;
    private String startedAtFrom;
    private String startedAtTo;
    private String finishedAtFrom;
    private String finishedAtTo;
    private Integer offset;
    private int limit = 50;

    abstract T self();

    final T withServerName(String serverName) {
        this.serverName = serverName;
        return self();
    }

    final T withServerAction(
            String serverName,
            String actionCode) {
        this.serverName = serverName;
        this.actionCode = actionCode;
        return self();
    }

    final T withActionCode(String actionCode) {
        this.actionCode = actionCode;
        return self();
    }

    final T withStatus(String status) {
        this.status = status;
        return self();
    }

    final T withExecuted(Boolean executed) {
        this.executed = executed;
        return self();
    }

    final T withExecutionMode(String executionMode) {
        this.executionMode = executionMode;
        return self();
    }

    final T withRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return self();
    }

    final T withWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
        return self();
    }

    final T withStartedAtFrom(String startedAtFrom) {
        this.startedAtFrom = startedAtFrom;
        return self();
    }

    final T withStartedAtTo(String startedAtTo) {
        this.startedAtTo = startedAtTo;
        return self();
    }

    final T withFinishedAtFrom(String finishedAtFrom) {
        this.finishedAtFrom = finishedAtFrom;
        return self();
    }

    final T withFinishedAtTo(String finishedAtTo) {
        this.finishedAtTo = finishedAtTo;
        return self();
    }

    final T withPage(Integer offset, int limit) {
        this.offset = offset;
        this.limit = limit;
        return self();
    }

    final T withLimit(int limit) {
        this.limit = limit;
        return self();
    }

    final T withActionIdValue(String actionId) {
        this.actionId = actionId;
        return self();
    }

    final McpServerActionExecutionHistoryQuery buildQuery() {
        return McpServerActionExecutionHistoryQuery.builder()
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

    final McpServerActionExecutionHistoryQueryParams buildQueryParams() {
        return McpServerActionExecutionHistoryQueryParams.of(
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
                offset,
                limit);
    }
}
