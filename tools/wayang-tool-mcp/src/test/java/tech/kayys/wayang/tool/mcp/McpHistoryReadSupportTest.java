package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHistoryReadSupportTest {

    private static final Comparator<HistoryItem> NEWEST_FIRST =
            Comparator.comparing(HistoryItem::finishedAt).reversed();

    @Test
    void filterAndSortAppliesPredicateComparatorAndScanLimit() {
        List<HistoryItem> items = List.of(
                item("a", "old", "2026-05-31T01:00:00Z"),
                item("b", "keep", "2026-05-31T01:03:00Z"),
                item("c", "keep", "2026-05-31T01:02:00Z"),
                item("d", "keep", "2026-05-31T01:01:00Z"));

        List<HistoryItem> result = McpHistoryReadSupport.filterAndSort(
                items,
                item -> item.group().equals("keep"),
                NEWEST_FIRST,
                2);

        assertEquals(List.of("b", "c"), result.stream().map(HistoryItem::id).toList());
    }

    @Test
    void pageAndHasMoreUseOffsetAndLimit() {
        List<HistoryItem> items = List.of(
                item("a", "keep", "2026-05-31T01:00:00Z"),
                item("b", "keep", "2026-05-31T01:01:00Z"),
                item("c", "keep", "2026-05-31T01:02:00Z"));

        assertEquals(List.of("b"), McpHistoryReadSupport.page(items, 1, 1)
                .stream()
                .map(HistoryItem::id)
                .toList());
        assertTrue(McpHistoryReadSupport.hasMore(items, 1, 1));
        assertFalse(McpHistoryReadSupport.hasMore(items, 2, 1));
    }

    @Test
    void pageMetadataHandlesNullEntriesAndNextOffset() {
        List<HistoryItem> entries = List.of(item("a", "keep", "2026-05-31T01:00:00Z"));

        assertEquals(List.of(), McpHistoryReadSupport.copyEntries(null));
        assertEquals(0, McpHistoryReadSupport.returned(null));
        assertEquals(1, McpHistoryReadSupport.returned(entries));
        assertEquals(3, McpHistoryReadSupport.nextOffset(2, 1, true));
        assertEquals(null, McpHistoryReadSupport.nextOffset(2, 1, false));
    }

    @Test
    void latestByKeyKeepsFirstEntryForEachKeyAndSortsResult() {
        List<HistoryItem> items = List.of(
                item("older-a", "a", "2026-05-31T01:02:00Z"),
                item("newer-b", "b", "2026-05-31T01:04:00Z"),
                item("newer-a", "a", "2026-05-31T01:03:00Z"));

        List<HistoryItem> result = McpHistoryReadSupport.latestByKey(
                items,
                HistoryItem::group,
                NEWEST_FIRST);

        assertEquals(List.of("newer-b", "older-a"), result.stream().map(HistoryItem::id).toList());
    }

    private static HistoryItem item(
            String id,
            String group,
            String finishedAt) {
        return new HistoryItem(id, group, Instant.parse(finishedAt));
    }

    private record HistoryItem(
            String id,
            String group,
            Instant finishedAt) {
    }
}
