package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpHistoryStatsTest {

    @Test
    void toolCallEmptyStatsUseZeroCountsAndTimestamp() {
        McpToolCallHistoryStats stats = McpToolCallHistoryStats.empty();

        assertEquals(0, stats.runs());
        assertEquals(0, stats.entries());
        assertEquals(0, stats.maxEntriesPerRun());
        assertEquals(0, stats.retentionSeconds());
        assertNull(stats.oldestEntryAt());
        assertNull(stats.newestEntryAt());
        assertNotNull(stats.inspectedAt());
    }

    @Test
    void actionExecutionEmptyStatsUseZeroCountsAndTimestamp() {
        McpServerActionExecutionHistoryStats stats = McpServerActionExecutionHistoryStats.empty();

        assertEquals(0, stats.requests());
        assertEquals(0, stats.entries());
        assertEquals(0, stats.maxEntriesPerRequest());
        assertEquals(0, stats.retentionSeconds());
        assertNull(stats.oldestEntryAt());
        assertNull(stats.newestEntryAt());
        assertNotNull(stats.inspectedAt());
    }
}
