package tech.kayys.wayang.tool.mcp;

final class McpServerActionExecutionHistoryQueryParamsTestBuilder
        extends McpServerActionExecutionHistoryQueryFieldsTestBuilder<
                McpServerActionExecutionHistoryQueryParamsTestBuilder> {

    private McpServerActionExecutionHistoryQueryParamsTestBuilder(String actionId) {
        withActionIdValue(actionId);
    }

    static McpServerActionExecutionHistoryQueryParamsTestBuilder forAction(String actionId) {
        return new McpServerActionExecutionHistoryQueryParamsTestBuilder(actionId);
    }

    @Override
    McpServerActionExecutionHistoryQueryParamsTestBuilder self() {
        return this;
    }

    McpServerActionExecutionHistoryQueryParams build() {
        return buildQueryParams();
    }
}
