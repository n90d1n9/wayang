package tech.kayys.wayang.tool.mcp;

final class McpServerActionExecutionHistoryFilterBuilder {
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
    private boolean latest;

    McpServerActionExecutionHistoryFilterBuilder withActionId(String actionId) {
        this.actionId = actionId;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withActionCode(String actionCode) {
        this.actionCode = actionCode;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withExecuted(Boolean executed) {
        this.executed = executed;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withExecutionMode(String executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withStartedAtFrom(String startedAtFrom) {
        this.startedAtFrom = startedAtFrom;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withStartedAtTo(String startedAtTo) {
        this.startedAtTo = startedAtTo;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withFinishedAtFrom(String finishedAtFrom) {
        this.finishedAtFrom = finishedAtFrom;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withFinishedAtTo(String finishedAtTo) {
        this.finishedAtTo = finishedAtTo;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    McpServerActionExecutionHistoryFilterBuilder latest() {
        latest = true;
        return this;
    }

    McpServerActionExecutionHistoryFilters build() {
        McpServerActionExecutionHistoryPageWindow pageWindow = pageWindow();
        return new McpServerActionExecutionHistoryFilters(
                actionId,
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                hasWarnings,
                McpHistoryFilterSupport.parseInstant(startedAtFrom),
                McpHistoryFilterSupport.parseInstant(startedAtTo),
                McpHistoryFilterSupport.parseInstant(finishedAtFrom),
                McpHistoryFilterSupport.parseInstant(finishedAtTo),
                pageWindow.offset(),
                pageWindow.limit(),
                pageWindow.scanLimit());
    }

    private McpServerActionExecutionHistoryPageWindow pageWindow() {
        if (latest) {
            return McpServerActionExecutionHistoryPageWindow.latest(offset, limit);
        }
        return McpServerActionExecutionHistoryPageWindow.page(offset, limit);
    }
}
