package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpServerActionExecutionHistoryPage(
        List<McpServerActionExecutionHistoryEntry> entries,
        int offset,
        int limit,
        int returned,
        boolean hasMore,
        Integer nextOffset) {

    public McpServerActionExecutionHistoryPage {
        entries = McpHistoryReadSupport.copyEntries(entries);
        returned = McpHistoryReadSupport.returned(entries);
        nextOffset = McpHistoryReadSupport.nextOffset(offset, returned, hasMore);
    }

    static McpServerActionExecutionHistoryPage of(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters,
            boolean hasMore) {
        return new McpServerActionExecutionHistoryPage(
                entries,
                filters.offset(),
                filters.limit(),
                McpHistoryReadSupport.returned(entries),
                hasMore,
                null);
    }
}
