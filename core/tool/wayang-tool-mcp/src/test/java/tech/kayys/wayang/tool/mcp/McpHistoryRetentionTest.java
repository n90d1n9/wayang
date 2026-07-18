package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHistoryRetentionTest {

    @Test
    void retentionFromConfigParsesIsoAndSimpleDurations() {
        assertEquals(Duration.ofDays(2), McpHistoryRetention.retentionFromConfig("P2D"));
        assertEquals(Duration.ofMillis(250), McpHistoryRetention.retentionFromConfig("250ms"));
        assertEquals(Duration.ofSeconds(30), McpHistoryRetention.retentionFromConfig("30s"));
        assertEquals(Duration.ofMinutes(15), McpHistoryRetention.retentionFromConfig("15m"));
        assertEquals(Duration.ofHours(3), McpHistoryRetention.retentionFromConfig("3h"));
        assertEquals(Duration.ofDays(4), McpHistoryRetention.retentionFromConfig("4d"));
        assertEquals(Duration.ofSeconds(90), McpHistoryRetention.retentionFromConfig("90"));
    }

    @Test
    void retentionFromConfigFallsBackForBlankInvalidAndNonPositiveValues() {
        Duration fallback = Duration.ofHours(12);

        assertEquals(fallback, McpHistoryRetention.retentionFromConfig(null, fallback));
        assertEquals(fallback, McpHistoryRetention.retentionFromConfig("   ", fallback));
        assertEquals(fallback, McpHistoryRetention.retentionFromConfig("never", fallback));
        assertEquals(fallback, McpHistoryRetention.retentionFromConfig("PT0S", fallback));
        assertEquals(fallback, McpHistoryRetention.retentionFromConfig("-1h", fallback));
    }

    @Test
    void normalizeMaxEntriesDefaultsAndCapsConfiguredValues() {
        assertEquals(500, McpHistoryRetention.normalizeMaxEntries(0));
        assertEquals(500, McpHistoryRetention.normalizeMaxEntries(-10));
        assertEquals(100, McpHistoryRetention.normalizeMaxEntries(100));
        assertEquals(10_000, McpHistoryRetention.normalizeMaxEntries(20_000));
        assertEquals(50, McpHistoryRetention.normalizeMaxEntries(-1, 50, 100));
        assertEquals(100, McpHistoryRetention.normalizeMaxEntries(500, 50, 100));
    }
}
