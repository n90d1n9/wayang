package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.kayys.wayang.tool.mcp.McpHistoryPruneSchedulerTestHarness.actionExecutionHistoryCountingStore;
import static tech.kayys.wayang.tool.mcp.McpHistoryPruneSchedulerTestHarness.actionExecutionHistoryScheduler;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryResourceTestHarness.historyServiceWithStore;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.executedResult;
import static tech.kayys.wayang.tool.mcp.McpServerActionExecutionHistoryTestFixtures.record;

class McpServerActionExecutionHistoryPruneSchedulerTest {

    @Test
    void enabledSchedulerPrunesExpiredEntriesAcrossRequests() {
        Instant base = Instant.parse("2026-05-30T05:00:00Z");
        MutableClock clock = new MutableClock(base);
        McpServerActionExecutionHistoryStore store =
                new InMemoryMcpServerActionExecutionHistoryStore(Duration.ofHours(1), clock);
        McpServerActionExecutionHistoryService service = historyServiceWithStore(store);
        record(service, "tenant-1", executedResult("docs", base.minus(Duration.ofMinutes(30))));
        record(service, "tenant-1", executedResult("crm", base));
        record(service, "tenant-2", executedResult("files", base.minus(Duration.ofMinutes(30))));
        McpServerActionExecutionHistoryPruneScheduler scheduler =
                actionExecutionHistoryScheduler(service, true);

        clock.advance(Duration.ofMinutes(31));
        McpServerActionExecutionHistoryPruneResult result = scheduler
                .pruneExpiredActionExecutionHistory()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(2, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(List.of("crm"),
                service.list("tenant-1", null, null, null, 10)
                        .await().atMost(Duration.ofSeconds(3))
                        .stream()
                        .map(McpServerActionExecutionHistoryEntry::serverName)
                        .toList());
        assertEquals(List.of(),
                service.list("tenant-2", null, null, null, 10)
                        .await().atMost(Duration.ofSeconds(3)));
    }

    @Test
    void disabledSchedulerDoesNotPruneHistory() {
        McpHistoryPruneSchedulerTestHarness.ActionExecutionHistoryCountingStore store =
                actionExecutionHistoryCountingStore();
        McpServerActionExecutionHistoryService service = historyServiceWithStore(store);
        McpServerActionExecutionHistoryPruneScheduler scheduler =
                actionExecutionHistoryScheduler(service, false);

        McpServerActionExecutionHistoryPruneResult result = scheduler
                .pruneExpiredActionExecutionHistory()
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(0, result.pruned());
        assertNotNull(result.prunedAt());
        assertEquals(0, store.pruneExpiredCalls());
    }
}
