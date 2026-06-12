package tech.kayys.wayang.tool.mcp;

final class McpToolCallHistoryQueryBuilder {
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
    private int limit;

    McpToolCallHistoryQueryBuilder withToolId(String toolId) {
        this.toolId = toolId;
        return this;
    }

    McpToolCallHistoryQueryBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    McpToolCallHistoryQueryBuilder withSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    McpToolCallHistoryQueryBuilder withFailureType(String failureType) {
        this.failureType = failureType;
        return this;
    }

    McpToolCallHistoryQueryBuilder withStartedAtFrom(String startedAtFrom) {
        this.startedAtFrom = startedAtFrom;
        return this;
    }

    McpToolCallHistoryQueryBuilder withStartedAtTo(String startedAtTo) {
        this.startedAtTo = startedAtTo;
        return this;
    }

    McpToolCallHistoryQueryBuilder withFinishedAtFrom(String finishedAtFrom) {
        this.finishedAtFrom = finishedAtFrom;
        return this;
    }

    McpToolCallHistoryQueryBuilder withFinishedAtTo(String finishedAtTo) {
        this.finishedAtTo = finishedAtTo;
        return this;
    }

    McpToolCallHistoryQueryBuilder withDurationWindow(
            Long minDurationMs,
            Long maxDurationMs) {
        this.minDurationMs = minDurationMs;
        this.maxDurationMs = maxDurationMs;
        return this;
    }

    McpToolCallHistoryQueryBuilder withMcpDurationWindow(
            Long minMcpDurationMs,
            Long maxMcpDurationMs) {
        this.minMcpDurationMs = minMcpDurationMs;
        this.maxMcpDurationMs = maxMcpDurationMs;
        return this;
    }

    McpToolCallHistoryQueryBuilder withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    McpToolCallHistoryQueryBuilder withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    McpToolCallHistoryQueryBuilder withPage(Integer offset, int limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }

    McpToolCallHistoryQuery build() {
        return new McpToolCallHistoryQuery(
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
