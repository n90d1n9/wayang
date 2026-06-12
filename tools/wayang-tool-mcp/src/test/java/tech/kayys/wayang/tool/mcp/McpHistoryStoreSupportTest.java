package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHistoryStoreSupportTest {

    @Test
    void scopeKeyCollapsesNullAndBlankScopes() {
        assertEquals("", McpHistoryStoreSupport.scopeKey(null));
        assertEquals("", McpHistoryStoreSupport.scopeKey(""));
        assertEquals("", McpHistoryStoreSupport.scopeKey("   "));
        assertEquals("tenant-1", McpHistoryStoreSupport.scopeKey("tenant-1"));
    }

    @Test
    void snapshotCopiesScopedEntries() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z")));

        List<HistoryItem> snapshot = McpHistoryStoreSupport.snapshot(entriesByScope, "tenant-1");
        entriesByScope.get("tenant-1").removeFirst();

        assertEquals(2, snapshot.size());
        assertEquals(List.of(), McpHistoryStoreSupport.snapshot(entriesByScope, "missing"));
    }

    @Test
    void countHandlesMissingAndFilteredEntries() {
        ArrayDeque<HistoryItem> entries = deque(
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z"));

        assertEquals(0, McpHistoryStoreSupport.count(null, null));
        assertEquals(2, McpHistoryStoreSupport.count(entries, null));
        assertEquals(1, McpHistoryStoreSupport.count(
                entries,
                item -> item.finishedAt().equals(Instant.parse("2026-05-31T01:01:00Z"))));
    }

    @Test
    void countHandlesScopedEntries() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z")));

        assertEquals(2, McpHistoryStoreSupport.count(entriesByScope, "tenant-1", null));
        assertEquals(1, McpHistoryStoreSupport.count(
                entriesByScope,
                "tenant-1",
                item -> item.finishedAt().equals(Instant.parse("2026-05-31T01:01:00Z"))));
        assertEquals(0, McpHistoryStoreSupport.count(entriesByScope, "missing", null));
    }

    @Test
    void trimToMaxEntriesDropsOldestEntriesFromTail() {
        ArrayDeque<HistoryItem> entries = deque(
                item("2026-05-31T01:02:00Z"),
                item("2026-05-31T01:01:00Z"),
                item("2026-05-31T01:00:00Z"));

        int trimmed = McpHistoryStoreSupport.trimToMaxEntries(entries, 2);

        assertEquals(1, trimmed);
        assertEquals(2, entries.size());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), entries.getFirst().finishedAt());
        assertEquals(Instant.parse("2026-05-31T01:01:00Z"), entries.getLast().finishedAt());
    }

    @Test
    void appendNewestAddsToHeadPrunesExpiredAndTrimsTail() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:01:00Z"),
                item("2026-05-31T00:59:00Z")));

        McpHistoryStoreSupport.appendNewest(
                entriesByScope,
                "tenant-1",
                item("2026-05-31T01:02:00Z"),
                Instant.parse("2026-05-31T01:00:00Z"),
                HistoryItem::finishedAt,
                1);

        assertEquals(1, entriesByScope.get("tenant-1").size());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"),
                entriesByScope.get("tenant-1").getFirst().finishedAt());
    }

    @Test
    void clearScopeRemovesScopedEntriesAndReturnsCount() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z")));

        assertEquals(2, McpHistoryStoreSupport.clearScope(entriesByScope, "tenant-1"));
        assertEquals(false, entriesByScope.containsKey("tenant-1"));
        assertEquals(0, McpHistoryStoreSupport.clearScope(entriesByScope, "tenant-1"));
    }

    @Test
    void removeMatchingRemovesFilteredEntriesAndDropsEmptyScope() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z")));

        int removed = McpHistoryStoreSupport.removeMatching(
                entriesByScope,
                "tenant-1",
                item -> item.finishedAt().isBefore(Instant.parse("2026-05-31T01:02:00Z")));

        assertEquals(2, removed);
        assertEquals(false, entriesByScope.containsKey("tenant-1"));
        assertEquals(0, McpHistoryStoreSupport.removeMatching(entriesByScope, "missing", item -> true));
    }

    @Test
    void pruneExpiredScopePrunesOneScopeAndDropsItWhenEmpty() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("old-only", deque(
                item("2026-05-31T00:59:00Z")));
        entriesByScope.put("mixed", deque(
                item("2026-05-31T00:59:00Z"),
                item("2026-05-31T01:01:00Z")));

        int pruned = McpHistoryStoreSupport.pruneExpiredScope(
                entriesByScope,
                "mixed",
                Instant.parse("2026-05-31T01:00:00Z"),
                HistoryItem::finishedAt);

        assertEquals(1, pruned);
        assertEquals(1, entriesByScope.get("mixed").size());
        assertEquals(true, entriesByScope.containsKey("old-only"));
        assertEquals(1, McpHistoryStoreSupport.pruneExpiredScope(
                entriesByScope,
                "old-only",
                Instant.parse("2026-05-31T01:00:00Z"),
                HistoryItem::finishedAt));
        assertEquals(false, entriesByScope.containsKey("old-only"));
    }

    @Test
    void retentionPolicyNormalizesClockAndRetention() {
        McpHistoryStoreSupport.RetentionPolicy policy =
                new McpHistoryStoreSupport.RetentionPolicy(Duration.ZERO, -1, null);

        assertEquals(McpHistoryRetention.DEFAULT_RETENTION.toSeconds(), policy.retentionSeconds());
        assertEquals(0, policy.maxEntries());
        assertEquals(true, policy.now() != null);
    }

    @Test
    void retentionPolicyComputesCutoffFromClock() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-05-31T01:05:00Z"),
                java.time.ZoneOffset.UTC);
        McpHistoryStoreSupport.RetentionPolicy policy =
                new McpHistoryStoreSupport.RetentionPolicy(Duration.ofMinutes(5), 10, clock);

        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), policy.cutoff());
        assertEquals(300, policy.retentionSeconds());
        assertEquals(10, policy.maxEntries());
    }

    @Test
    void retentionPolicyFactoryNormalizesConfiguredMaxEntries() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-05-31T01:05:00Z"),
                java.time.ZoneOffset.UTC);

        McpHistoryStoreSupport.RetentionPolicy defaulted =
                McpHistoryStoreSupport.RetentionPolicy.of(Duration.ofMinutes(1), -1, 50, 100, clock);
        McpHistoryStoreSupport.RetentionPolicy capped =
                McpHistoryStoreSupport.RetentionPolicy.of(Duration.ofMinutes(1), 500, 50, 100, clock);

        assertEquals(50, defaulted.maxEntries());
        assertEquals(100, capped.maxEntries());
        assertEquals(60, defaulted.retentionSeconds());
        assertEquals(Instant.parse("2026-05-31T01:04:00Z"), defaulted.cutoff());
    }

    @Test
    void entryStatsSummarizesScopedEntries() {
        McpHistoryStoreSupport.EntryStats stats = McpHistoryStoreSupport.entryStats(
                deque(
                        item("2026-05-31T01:02:00Z"),
                        item("2026-05-31T01:00:00Z"),
                        item("2026-05-31T01:01:00Z")),
                HistoryItem::finishedAt);

        assertEquals(3, stats.entries());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), stats.oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), stats.newestEntryAt());
    }

    @Test
    void entryStatsSummarizesNamedScopeEntries() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:02:00Z"),
                item("2026-05-31T01:00:00Z")));

        McpHistoryStoreSupport.EntryStats stats =
                McpHistoryStoreSupport.entryStats(entriesByScope, "tenant-1", HistoryItem::finishedAt);
        McpHistoryStoreSupport.EntryStats missing =
                McpHistoryStoreSupport.entryStats(entriesByScope, "missing", HistoryItem::finishedAt);

        assertEquals(2, stats.entries());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), stats.oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:02:00Z"), stats.newestEntryAt());
        assertEquals(0, missing.entries());
        assertEquals(null, missing.oldestEntryAt());
        assertEquals(null, missing.newestEntryAt());
    }

    @Test
    void entryStatsSummarizesAggregateEntries() {
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("tenant-1", deque(
                item("2026-05-31T01:00:00Z")));
        entriesByScope.put("tenant-2", deque(
                item("2026-05-31T01:03:00Z"),
                item("2026-05-31T01:02:00Z")));

        McpHistoryStoreSupport.EntryStats stats =
                McpHistoryStoreSupport.entryStats(entriesByScope, HistoryItem::finishedAt);

        assertEquals(3, stats.entries());
        assertEquals(Instant.parse("2026-05-31T01:00:00Z"), stats.oldestEntryAt());
        assertEquals(Instant.parse("2026-05-31T01:03:00Z"), stats.newestEntryAt());
    }

    @Test
    void pruneExpiredRemovesExpiredEntriesAndEmptyScopes() {
        Instant cutoff = Instant.parse("2026-05-31T01:00:00Z");
        Map<String, ArrayDeque<HistoryItem>> entriesByScope = new LinkedHashMap<>();
        entriesByScope.put("old-only", deque(
                item("2026-05-31T00:59:00Z")));
        entriesByScope.put("mixed", deque(
                item("2026-05-31T00:58:00Z"),
                item("2026-05-31T01:00:00Z"),
                item("2026-05-31T01:01:00Z")));

        int pruned = McpHistoryStoreSupport.pruneExpired(entriesByScope, cutoff, HistoryItem::finishedAt);

        assertEquals(2, pruned);
        assertEquals(false, entriesByScope.containsKey("old-only"));
        assertEquals(2, entriesByScope.get("mixed").size());
    }

    @Test
    void minAndMaxIgnoreNullCandidate() {
        Instant first = Instant.parse("2026-05-31T01:00:00Z");
        Instant earlier = Instant.parse("2026-05-31T00:59:00Z");
        Instant later = Instant.parse("2026-05-31T01:01:00Z");

        assertEquals(first, McpHistoryStoreSupport.min(first, null));
        assertEquals(earlier, McpHistoryStoreSupport.min(first, earlier));
        assertEquals(first, McpHistoryStoreSupport.max(first, null));
        assertEquals(later, McpHistoryStoreSupport.max(first, later));
    }

    private static ArrayDeque<HistoryItem> deque(HistoryItem... items) {
        ArrayDeque<HistoryItem> deque = new ArrayDeque<>();
        for (HistoryItem item : items) {
            deque.add(item);
        }
        return deque;
    }

    private static HistoryItem item(String finishedAt) {
        return new HistoryItem(Instant.parse(finishedAt));
    }

    private record HistoryItem(Instant finishedAt) {
    }
}
