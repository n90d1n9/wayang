package tech.kayys.wayang.tool.mcp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

final class McpToolCallHistoryServiceTestHarness {
    private static final Duration DEFAULT_RETENTION = Duration.ofDays(7);
    private static final Clock HISTORY_CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T00:00:00Z"),
            ZoneOffset.UTC);

    private McpToolCallHistoryServiceTestHarness() {
    }

    static History newHistory() {
        return historyWithStore(new InMemoryMcpToolCallHistoryStore(DEFAULT_RETENTION, HISTORY_CLOCK));
    }

    static History historyWithMaxEntries(int maxEntriesPerRun) {
        return historyWithStore(new InMemoryMcpToolCallHistoryStore(
                DEFAULT_RETENTION,
                maxEntriesPerRun,
                HISTORY_CLOCK));
    }

    static History historyWithRetention(Duration retention, Clock clock) {
        return historyWithStore(new InMemoryMcpToolCallHistoryStore(retention, clock));
    }

    static History historyWithStore(McpToolCallHistoryStore store) {
        return new History(store, new McpToolCallHistoryService(store));
    }

    record History(
            McpToolCallHistoryStore store,
            McpToolCallHistoryService service) {

        History append(McpToolCallHistoryEntry entry) {
            McpToolCallHistoryTestFixtures.append(store, entry);
            return this;
        }
    }
}
