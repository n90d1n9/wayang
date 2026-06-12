package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpInMemoryScopedHistoryOperationsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-31T01:05:00Z"),
            ZoneOffset.UTC);

    @Test
    void delegatesScopedReadsCountsStatsAndClears() {
        McpInMemoryScopedHistoryOperations<HistoryItem, HistoryFilter> history = history(10);

        history.append("scope-1", item("drop", "2026-05-31T01:01:00Z")).await().indefinitely();
        history.append("scope-1", item("keep", "2026-05-31T01:02:00Z")).await().indefinitely();
        history.append("scope-2", item("other", "2026-05-31T01:03:00Z")).await().indefinitely();

        assertEquals(List.of("keep", "drop"), history.list("scope-1")
                .await().indefinitely()
                .stream()
                .map(HistoryItem::name)
                .toList());
        assertEquals(2, history.count("scope-1", null).await().indefinitely());
        assertEquals(1, history.count("scope-1", new HistoryFilter("keep")).await().indefinitely());
        assertEquals(2, history.stats().await().indefinitely().scopes());
        assertEquals(3, history.stats().await().indefinitely().entries());

        assertEquals(1, history.clear("scope-1", new HistoryFilter("keep")).await().indefinitely());
        assertEquals(List.of("drop"), history.list("scope-1")
                .await().indefinitely()
                .stream()
                .map(HistoryItem::name)
                .toList());
        assertEquals(1, history.clear("scope-1").await().indefinitely());
    }

    @Test
    void delegatesPruning() {
        McpInMemoryScopedHistoryOperations<HistoryItem, HistoryFilter> history = history(10);

        history.append("scope-1", item("expired", "2026-05-31T00:59:59Z")).await().indefinitely();
        history.append("scope-1", item("fresh", "2026-05-31T01:00:00Z")).await().indefinitely();

        assertEquals(List.of("fresh"), history.list("scope-1")
                .await().indefinitely()
                .stream()
                .map(HistoryItem::name)
                .toList());
        assertEquals(0, history.pruneExpired("scope-1").await().indefinitely());
        assertEquals(0, history.pruneExpired().await().indefinitely());
    }

    private static McpInMemoryScopedHistoryOperations<HistoryItem, HistoryFilter> history(int maxEntries) {
        return McpInMemoryScopedHistoryOperations.of(
                new McpHistoryStoreSupport.RetentionPolicy(Duration.ofMinutes(5), maxEntries, CLOCK),
                HistoryItem::finishedAt,
                filter -> item -> item.name().equals(filter.name()));
    }

    private static HistoryItem item(String name, String finishedAt) {
        return new HistoryItem(name, Instant.parse(finishedAt));
    }

    private record HistoryItem(
            String name,
            Instant finishedAt) {
    }

    private record HistoryFilter(String name) {
    }
}
