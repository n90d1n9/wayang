package tech.kayys.wayang.tool.mcp;

final class McpToolCallHistoryQueryTestBuilder {
    private final McpToolCallHistoryQueryBuilder builder =
            McpToolCallHistoryQuery.builder()
                    .withLimit(50);

    private McpToolCallHistoryQueryTestBuilder() {
    }

    static McpToolCallHistoryQueryTestBuilder query() {
        return new McpToolCallHistoryQueryTestBuilder();
    }

    McpToolCallHistoryQueryTestBuilder withToolId(String toolId) {
        builder.withToolId(toolId);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withStatus(String status) {
        builder.withStatus(status);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withSuccess(Boolean success) {
        builder.withSuccess(success);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withFailureType(String failureType) {
        builder.withFailureType(failureType);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withStartedAtFrom(String startedAtFrom) {
        builder.withStartedAtFrom(startedAtFrom);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withStartedAtTo(String startedAtTo) {
        builder.withStartedAtTo(startedAtTo);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withFinishedAtFrom(String finishedAtFrom) {
        builder.withFinishedAtFrom(finishedAtFrom);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withFinishedAtTo(String finishedAtTo) {
        builder.withFinishedAtTo(finishedAtTo);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withDurationWindow(
            Long minDurationMs,
            Long maxDurationMs) {
        builder.withDurationWindow(minDurationMs, maxDurationMs);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withMcpDurationWindow(
            Long minMcpDurationMs,
            Long maxMcpDurationMs) {
        builder.withMcpDurationWindow(minMcpDurationMs, maxMcpDurationMs);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withPage(Integer offset, int limit) {
        builder.withPage(offset, limit);
        return this;
    }

    McpToolCallHistoryQueryTestBuilder withLimit(int limit) {
        builder.withLimit(limit);
        return this;
    }

    McpToolCallHistoryQuery build() {
        return builder.build();
    }
}
