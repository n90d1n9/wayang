package tech.kayys.wayang.tool.mcp;

final class McpToolCallHistoryQueryParamsTestBuilder {
    private final String runId;
    private String toolId;
    private String status;
    private Boolean success;
    private String failureType;
    private String startedAtFrom;
    private String startedAtTo;
    private String finishedAtFrom;
    private String finishedAtTo;
    private Long minDurationMs;
    private Long maxDurationMs;
    private Long minMcpDurationMs;
    private Long maxMcpDurationMs;
    private Integer offset;
    private Integer limit;

    private McpToolCallHistoryQueryParamsTestBuilder(String runId) {
        this.runId = runId;
    }

    static McpToolCallHistoryQueryParamsTestBuilder forRun(String runId) {
        return new McpToolCallHistoryQueryParamsTestBuilder(runId);
    }

    McpToolCallHistoryQueryParamsTestBuilder withToolId(String toolId) {
        this.toolId = toolId;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withFailureType(String failureType) {
        this.failureType = failureType;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withStartedAtFrom(String startedAtFrom) {
        this.startedAtFrom = startedAtFrom;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withStartedAtTo(String startedAtTo) {
        this.startedAtTo = startedAtTo;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withFinishedAtFrom(String finishedAtFrom) {
        this.finishedAtFrom = finishedAtFrom;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withFinishedAtTo(String finishedAtTo) {
        this.finishedAtTo = finishedAtTo;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withDurationWindow(
            Long minDurationMs,
            Long maxDurationMs) {
        this.minDurationMs = minDurationMs;
        this.maxDurationMs = maxDurationMs;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withMcpDurationWindow(
            Long minMcpDurationMs,
            Long maxMcpDurationMs) {
        this.minMcpDurationMs = minMcpDurationMs;
        this.maxMcpDurationMs = maxMcpDurationMs;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withPage(Integer offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }

    McpToolCallHistoryQueryParamsTestBuilder withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    McpToolCallHistoryQueryParams build() {
        return McpToolCallHistoryQueryParams.of(
                runId,
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
