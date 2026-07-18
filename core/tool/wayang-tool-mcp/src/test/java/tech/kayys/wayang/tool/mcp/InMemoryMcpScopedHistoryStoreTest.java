package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryMcpScopedHistoryStoreTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-31T01:05:00Z"), ZoneOffset.UTC);

    @Test
    void appendsListsCountsAndTrimsScopedEntries() {
        InMemoryMcpScopedHistoryStore<HistoryItem> store = store(2);

        store.append("scope-1", item("old", "2026-05-31T01:01:00Z"));
        store.append("scope-1", item("middle", "2026-05-31T01:02:00Z"));
        store.append("scope-1", item("new", "2026-05-31T01:03:00Z"));
        store.append("scope-2", item("other", "2026-05-31T01:04:00Z"));

        assertEquals(List.of(item("new", "2026-05-31T01:03:00Z"), item("middle", "2026-05-31T01:02:00Z")),
                store.list("scope-1"));
        assertEquals(2, store.count("scope-1", null));
        assertEquals(1, store.count("scope-1", item -> item.name().equals("new")));
        assertEquals(2, store.stats().scopes());
        assertEquals(3, store.stats().entries());
        assertEquals(1, store.stats("scope-2").scopes());
    }

    @Test
    void prunesExpiredEntriesBeforeReadsAndClearsByPredicate() {
        InMemoryMcpScopedHistoryStore<HistoryItem> store = store(10);

        store.append("scope-1", item("expired", "2026-05-31T00:59:59Z"));
        store.append("scope-1", item("kept", "2026-05-31T01:00:00Z"));
        store.append("scope-1", item("removed", "2026-05-31T01:01:00Z"));

        assertEquals(2, store.list("scope-1").size());
        assertEquals(1, store.clear("scope-1", item -> item.name().equals("removed")));
        assertEquals(List.of(item("kept", "2026-05-31T01:00:00Z")), store.list("scope-1"));
        assertEquals(1, store.clear("scope-1"));
        assertEquals(0, store.stats("scope-1").entries());
    }

    private static InMemoryMcpScopedHistoryStore<HistoryItem> store(int maxEntries) {
        return new InMemoryMcpScopedHistoryStore<>(
                new McpHistoryStoreSupport.RetentionPolicy(Duration.ofMinutes(5), maxEntries, CLOCK),
                HistoryItem::finishedAt);
    }

    private static HistoryItem item(String name, String finishedAt) {
        return new HistoryItem(name, Instant.parse(finishedAt));
    }

    private record HistoryItem(
            String name,
            Instant finishedAt) {
    }
}
