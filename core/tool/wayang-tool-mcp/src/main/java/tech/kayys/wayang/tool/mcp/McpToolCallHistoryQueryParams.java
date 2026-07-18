package tech.kayys.wayang.tool.mcp;

import jakarta.ws.rs.QueryParam;

public class McpToolCallHistoryQueryParams {

    @QueryParam("runId")
    String runId;

    @QueryParam("toolId")
    String toolId;

    @QueryParam("status")
    String status;

    @QueryParam("success")
    Boolean success;

    @QueryParam("failureType")
    String failureType;

    @QueryParam("startedAtFrom")
    String startedAtFrom;

    @QueryParam("startedAtTo")
    String startedAtTo;

    @QueryParam("finishedAtFrom")
    String finishedAtFrom;

    @QueryParam("finishedAtTo")
    String finishedAtTo;

    @QueryParam("minDurationMs")
    Long minDurationMs;

    @QueryParam("maxDurationMs")
    Long maxDurationMs;

    @QueryParam("minMcpDurationMs")
    Long minMcpDurationMs;

    @QueryParam("maxMcpDurationMs")
    Long maxMcpDurationMs;

    @QueryParam("offset")
    Integer offset;

    @QueryParam("limit")
    Integer limit;

    static McpToolCallHistoryQueryParams of(
            String runId,
            String toolId,
            String status,
            Boolean success,
            String failureType,
            Integer limit) {
        return of(runId, toolId, status, success, failureType, null, limit);
    }

    static McpToolCallHistoryQueryParams of(
            String runId,
            String toolId,
            String status,
            Boolean success,
            String failureType,
            Integer offset,
            Integer limit) {
        return of(runId, toolId, status, success, failureType, null, null, null, null, offset, limit);
    }

    static McpToolCallHistoryQueryParams of(
            String runId,
            String toolId,
            String status,
            Boolean success,
            String failureType,
            String startedAtFrom,
            String startedAtTo,
            String finishedAtFrom,
            String finishedAtTo,
            Integer offset,
            Integer limit) {
        return of(
                runId,
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

    static McpToolCallHistoryQueryParams of(
            String runId,
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
            Integer limit) {
        McpToolCallHistoryQueryParams params = new McpToolCallHistoryQueryParams();
        params.runId = runId;
        params.toolId = toolId;
        params.status = status;
        params.success = success;
        params.failureType = failureType;
        params.startedAtFrom = startedAtFrom;
        params.startedAtTo = startedAtTo;
        params.finishedAtFrom = finishedAtFrom;
        params.finishedAtTo = finishedAtTo;
        params.minDurationMs = minDurationMs;
        params.maxDurationMs = maxDurationMs;
        params.minMcpDurationMs = minMcpDurationMs;
        params.maxMcpDurationMs = maxMcpDurationMs;
        params.offset = offset;
        params.limit = limit;
        return params;
    }

    String runId(String fallbackRunId) {
        if (runId == null || runId.isBlank()) {
            return fallbackRunId;
        }
        return runId.trim();
    }

    McpToolCallHistoryQuery toQuery() {
        return toQuery(offset);
    }

    McpToolCallHistoryQuery toUnpagedQuery() {
        return toQuery(null);
    }

    private McpToolCallHistoryQuery toQuery(Integer queryOffset) {
        return McpToolCallHistoryQuery.builder()
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
                .withOffset(queryOffset)
                .withLimit(limit == null ? 0 : limit)
                .build();
    }
}
