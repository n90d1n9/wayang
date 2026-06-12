package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class McpServerActionExecutionHistoryQueryParams {

    @QueryParam("actionId")
    String actionId;

    @QueryParam("serverName")
    String serverName;

    @QueryParam("actionCode")
    String actionCode;

    @QueryParam("status")
    String status;

    @QueryParam("executed")
    Boolean executed;

    @QueryParam("executionMode")
    String executionMode;

    @QueryParam("riskLevel")
    String riskLevel;

    @QueryParam("hasWarnings")
    Boolean hasWarnings;

    @QueryParam("startedAtFrom")
    String startedAtFrom;

    @QueryParam("startedAtTo")
    String startedAtTo;

    @QueryParam("finishedAtFrom")
    String finishedAtFrom;

    @QueryParam("finishedAtTo")
    String finishedAtTo;

    @QueryParam("offset")
    Integer offset;

    @DefaultValue("50")
    @QueryParam("limit")
    int limit;

    static McpServerActionExecutionHistoryQueryParams of(
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
        McpServerActionExecutionHistoryQueryParams params =
                new McpServerActionExecutionHistoryQueryParams();
        params.actionId = actionId;
        params.serverName = serverName;
        params.actionCode = actionCode;
        params.status = status;
        params.executed = executed;
        params.executionMode = executionMode;
        params.riskLevel = riskLevel;
        params.hasWarnings = hasWarnings;
        params.startedAtFrom = startedAtFrom;
        params.startedAtTo = startedAtTo;
        params.finishedAtFrom = finishedAtFrom;
        params.finishedAtTo = finishedAtTo;
        params.offset = offset;
        params.limit = limit;
        return params;
    }

    McpServerActionExecutionHistoryQuery toQuery() {
        return toQuery(offset, limit);
    }

    McpServerActionExecutionHistoryQuery toUnpagedQuery() {
        return toQuery(null, limit);
    }

    McpServerActionExecutionHistoryQuery toFixedWindowQuery(int fixedLimit) {
        return toQuery(null, fixedLimit);
    }

    private McpServerActionExecutionHistoryQuery toQuery(Integer queryOffset, int queryLimit) {
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
                .withOffset(queryOffset)
                .withLimit(queryLimit)
                .build();
    }
}
