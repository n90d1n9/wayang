package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.kayys.wayang.tool.mcp.McpHistoryPruneSchedulerTestHarness.toolCallHistoryCountingStore;
import static tech.kayys.wayang.tool.mcp.McpHistoryPruneSchedulerTestHarness.toolCallHistoryScheduler;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.append;
import static tech.kayys.wayang.tool.mcp.McpToolCallHistoryTestFixtures.entry;

class McpToolCallHistoryPruneSchedulerTest {

    @Test
    void enabledSchedulerPrunesExpiredEntriesAcrossRuns() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T01:05:00Z"));
        McpToolCallHistoryStore store = new InMemoryMcpToolCallHistoryStore(Duration.ofMinutes(5), clock);
        append(store, entry("run-1", "docs:old", Instant.parse("2026-05-31T01:00:00Z")));
        append(store, entry("run-1", "docs:fresh", Instant.parse("2026-05-31T01:04:00Z")));
        append(store, entry("run-2", "crm:old", Instant.parse("2026-05-31T01:00:30Z")));
        McpToolCallHistoryPruneScheduler scheduler = toolCallHistoryScheduler(store, true);

        clock.setInstant(Instant.parse("2026-05-31T01:06:00Z"));
        McpToolCallHistoryPruneResult result = scheduler.pruneExpiredToolCallHistory()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(List.of("docs:fresh"),
                new McpToolCallHistoryService(store).list("run-1")
                        .await().atMost(Duration.ofSeconds(3))
                        .stream()
                        .map(McpToolCallHistoryEntry::toolId)
                        .toList());
        assertEquals(List.of(),
                new McpToolCallHistoryService(store).list("run-2")
                        .await().atMost(Duration.ofSeconds(3)));
    }

    @Test
    void disabledSchedulerDoesNotPruneHistory() {
        McpHistoryPruneSchedulerTestHarness.ToolCallHistoryCountingStore store =
                toolCallHistoryCountingStore();
        McpToolCallHistoryPruneScheduler scheduler = toolCallHistoryScheduler(store, false);

        McpToolCallHistoryPruneResult result = scheduler.pruneExpiredToolCallHistory()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(0, store.pruneExpiredCalls());
    }
}
