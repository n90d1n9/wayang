package tech.kayys.wayang.tool.mcp;

import java.util.Comparator;
import java.util.List;

final class McpServerActionExecutionHistoryViews {

    private static final Comparator<McpServerActionExecutionHistoryEntry> NEWEST_ACTION_EXECUTIONS =
            Comparator
                    .comparing(McpServerActionExecutionHistorySummaryKeys::sortFinishedAt)
                    .reversed()
                    .thenComparing(entry -> McpServerActionExecutionHistorySummaryKeys.nullSafe(entry.actionId()));

    private McpServerActionExecutionHistoryViews() {
    }

    static List<McpServerActionExecutionHistoryEntry> filteredEntries(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters) {
        return McpHistoryReadSupport.filterAndSort(
                entries,
                filters::matches,
                NEWEST_ACTION_EXECUTIONS,
                filters.scanLimit());
    }

    static List<McpServerActionExecutionHistoryEntry> pageEntries(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters) {
        return McpHistoryReadSupport.page(entries, filters.offset(), filters.limit());
    }

    static List<McpServerActionExecutionHistoryEntry> latestEntries(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters) {
        return pageEntries(latestSortedEntries(entries), filters);
    }

    static McpServerActionExecutionHistoryPage page(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters) {
        return McpServerActionExecutionHistoryPage.of(
                pageEntries(entries, filters),
                filters,
                McpHistoryReadSupport.hasMore(entries, filters.offset(), filters.limit()));
    }

    static McpServerActionExecutionHistoryPage latestPage(
            List<McpServerActionExecutionHistoryEntry> entries,
            McpServerActionExecutionHistoryFilters filters) {
        return page(latestSortedEntries(entries), filters);
    }

    private static List<McpServerActionExecutionHistoryEntry> latestSortedEntries(
            List<McpServerActionExecutionHistoryEntry> entries) {
        return McpHistoryReadSupport.latestByKey(
                entries,
                McpServerActionExecutionHistorySummaryKeys::actionIdentityKey,
                NEWEST_ACTION_EXECUTIONS);
    }
}
