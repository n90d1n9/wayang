package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.dto.ToolRequestContext;

record McpServerActionExecutionHistoryResourceQuery(
        String requestId,
        McpServerActionExecutionHistoryQuery historyQuery,
        McpServerActionExecutionHistoryQuery summaryQuery,
        McpServerActionExecutionHistoryQuery mutationQuery) {

    private static final int MUTATION_QUERY_LIMIT = 50;

    static McpServerActionExecutionHistoryResourceQuery from(
            McpServerActionExecutionHistoryQueryParams query,
            ToolRequestContext requestContext) {
        McpServerActionExecutionHistoryQueryParams params =
                McpResourceSupport.beanParam(query, McpServerActionExecutionHistoryQueryParams::new);
        return new McpServerActionExecutionHistoryResourceQuery(
                requestId(requestContext),
                params.toQuery(),
                params.toUnpagedQuery(),
                params.toFixedWindowQuery(MUTATION_QUERY_LIMIT));
    }

    static String requestId(ToolRequestContext requestContext) {
        return McpResourceSupport.currentRequestId(requestContext);
    }
}
