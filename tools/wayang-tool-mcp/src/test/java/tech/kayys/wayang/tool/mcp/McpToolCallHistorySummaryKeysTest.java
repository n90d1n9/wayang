package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolCallHistorySummaryKeysTest {

    @Test
    void sortsMissingFinishedAtAtEpoch() {
        Instant finishedAt = Instant.parse("2026-05-30T05:00:00Z");

        assertEquals(Instant.EPOCH, McpToolCallHistorySummaryKeys.sortFinishedAt(null));
        assertEquals(Instant.EPOCH, McpToolCallHistorySummaryKeys.sortFinishedAt(entry("docs.list", null)));
        assertEquals(finishedAt, McpToolCallHistorySummaryKeys.sortFinishedAt(entry("docs.list", finishedAt)));
    }

    @Test
    void usesEmptyToolIdForIdentityAndSortKeys() {
        assertEquals("", McpToolCallHistorySummaryKeys.toolIdentityKey(null));
        assertEquals("", McpToolCallHistorySummaryKeys.toolIdentityKey(entry(null, Instant.EPOCH)));
        assertEquals("", McpToolCallHistorySummaryKeys.sortToolId(null));
        assertEquals("", McpToolCallHistorySummaryKeys.sortToolId(entry(null, Instant.EPOCH)));
        assertEquals("docs.list", McpToolCallHistorySummaryKeys.toolIdentityKey(entry("docs.list", Instant.EPOCH)));
    }

    @Test
    void usesUnknownForSummaryBucketKeys() {
        assertEquals("(unknown)", McpToolCallHistorySummaryKeys.defaultKey(null));
        assertEquals("(unknown)", McpToolCallHistorySummaryKeys.defaultKey("  "));
        assertEquals("failed", McpToolCallHistorySummaryKeys.defaultKey("failed"));
    }

    private static McpToolCallHistoryEntry entry(String toolId, Instant finishedAt) {
        Instant startedAt = Instant.parse("2026-05-30T04:59:59Z");
        return new McpToolCallHistoryEntry(
                "run-1",
                "node-1",
                1,
                toolId,
                McpToolOutputFields.STATUS_SUCCESS,
                true,
                null,
                null,
                startedAt,
                finishedAt,
                1000,
                25,
                Map.of());
    }
}
