package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.dto.ToolRequestContext;

record McpToolCallHistoryResourceQuery(
        String runId,
        McpToolCallHistoryQuery historyQuery,
        McpToolCallHistoryQuery summaryQuery) {

    static McpToolCallHistoryResourceQuery from(
            McpToolCallHistoryQueryParams query,
            ToolRequestContext requestContext) {
        McpToolCallHistoryQueryParams params =
                McpResourceSupport.beanParam(query, McpToolCallHistoryQueryParams::new);
        return new McpToolCallHistoryResourceQuery(
                params.runId(McpResourceSupport.currentRequestId(requestContext)),
                params.toQuery(),
                params.toUnpagedQuery());
    }
}
