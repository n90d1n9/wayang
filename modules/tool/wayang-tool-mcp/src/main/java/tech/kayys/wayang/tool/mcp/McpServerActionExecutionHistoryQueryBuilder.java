package tech.kayys.wayang.tool.mcp;

final class McpServerActionExecutionHistoryQueryBuilder {
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
    private int limit = McpServerActionExecutionHistoryPageWindow.DEFAULT_LIMIT;

    McpServerActionExecutionHistoryQueryBuilder withActionId(String actionId) {
        this.actionId = actionId;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withActionCode(String actionCode) {
        this.actionCode = actionCode;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withExecuted(Boolean executed) {
        this.executed = executed;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withExecutionMode(String executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withStartedAtFrom(String startedAtFrom) {
        this.startedAtFrom = startedAtFrom;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withStartedAtTo(String startedAtTo) {
        this.startedAtTo = startedAtTo;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withFinishedAtFrom(String finishedAtFrom) {
        this.finishedAtFrom = finishedAtFrom;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withFinishedAtTo(String finishedAtTo) {
        this.finishedAtTo = finishedAtTo;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    McpServerActionExecutionHistoryQueryBuilder withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    McpServerActionExecutionHistoryQuery build() {
        return new McpServerActionExecutionHistoryQuery(
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
