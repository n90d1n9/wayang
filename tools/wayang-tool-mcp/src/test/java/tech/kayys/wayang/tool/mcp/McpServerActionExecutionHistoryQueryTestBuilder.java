package tech.kayys.wayang.tool.mcp;

final class McpServerActionExecutionHistoryQueryTestBuilder
        extends McpServerActionExecutionHistoryQueryFieldsTestBuilder<
                McpServerActionExecutionHistoryQueryTestBuilder> {

    private McpServerActionExecutionHistoryQueryTestBuilder() {
    }

    static McpServerActionExecutionHistoryQueryTestBuilder query() {
        return new McpServerActionExecutionHistoryQueryTestBuilder();
    }

    @Override
    McpServerActionExecutionHistoryQueryTestBuilder self() {
        return this;
    }

    McpServerActionExecutionHistoryQueryTestBuilder withActionId(String actionId) {
        return withActionIdValue(actionId);
    }

    McpServerActionExecutionHistoryQuery build() {
        return buildQuery();
    }
}
