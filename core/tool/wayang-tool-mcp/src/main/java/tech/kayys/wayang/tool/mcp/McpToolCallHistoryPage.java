package tech.kayys.wayang.tool.mcp;

import java.util.List;

public record McpToolCallHistoryPage(
        List<McpToolCallHistoryEntry> entries,
        int offset,
        int limit,
        int returned,
        boolean hasMore,
        Integer nextOffset) {

    public McpToolCallHistoryPage {
        entries = McpHistoryReadSupport.copyEntries(entries);
        returned = McpHistoryReadSupport.returned(entries);
        nextOffset = McpHistoryReadSupport.nextOffset(offset, returned, hasMore);
    }

    static McpToolCallHistoryPage of(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters,
            boolean hasMore) {
        return new McpToolCallHistoryPage(
                entries,
                filters.offset(),
                filters.limit(),
                McpHistoryReadSupport.returned(entries),
                hasMore,
                null);
    }
}
