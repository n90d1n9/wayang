package tech.kayys.wayang.tool.mcp;

import java.util.Comparator;
import java.util.List;

final class McpToolCallHistoryViews {

    private static final Comparator<McpToolCallHistoryEntry> NEWEST_TOOL_CALLS =
            Comparator.comparing(McpToolCallHistorySummaryKeys::sortFinishedAt).reversed();
    private static final Comparator<McpToolCallHistoryEntry> NEWEST_TOOLS =
            NEWEST_TOOL_CALLS.thenComparing(McpToolCallHistorySummaryKeys::sortToolId);

    private McpToolCallHistoryViews() {
    }

    static List<McpToolCallHistoryEntry> filteredEntries(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return McpHistoryReadSupport.filterAndSort(
                entries,
                filters::matches,
                NEWEST_TOOL_CALLS);
    }

    static List<McpToolCallHistoryEntry> pageEntries(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return McpHistoryReadSupport.page(entries, filters.offset(), filters.limit());
    }

    static McpToolCallHistoryPage page(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return McpToolCallHistoryPage.of(
                pageEntries(entries, filters),
                filters,
                McpHistoryReadSupport.hasMore(entries, filters.offset(), filters.limit()));
    }

    static List<McpToolCallHistoryEntry> latestEntries(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return pageEntries(latestSortedEntries(entries), filters);
    }

    static McpToolCallHistoryPage latestPage(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return page(latestSortedEntries(entries), filters);
    }

    static List<McpToolCallHistoryEntry> summaryEntries(
            List<McpToolCallHistoryEntry> entries,
            McpToolCallHistoryFilters filters) {
        return McpHistoryReadSupport.page(entries, 0, filters.limit());
    }

    private static List<McpToolCallHistoryEntry> latestSortedEntries(List<McpToolCallHistoryEntry> entries) {
        return McpHistoryReadSupport.latestByKey(
                entries,
                McpToolCallHistorySummaryKeys::toolIdentityKey,
                NEWEST_TOOLS);
    }
}
