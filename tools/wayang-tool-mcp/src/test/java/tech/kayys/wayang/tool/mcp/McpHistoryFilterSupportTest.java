package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHistoryFilterSupportTest {

    @Test
    void blankToNullTrimsValuesAndDropsBlankInput() {
        assertNull(McpHistoryFilterSupport.blankToNull(null));
        assertNull(McpHistoryFilterSupport.blankToNull("   "));
        assertEquals("value", McpHistoryFilterSupport.blankToNull(" value "));
    }

    @Test
    void parseInstantHandlesTrimmedInvalidAndMissingValues() {
        Instant parsed = Instant.parse("2026-05-31T01:00:00Z");

        assertEquals(parsed, McpHistoryFilterSupport.parseInstant(" 2026-05-31T01:00:00Z "));
        assertNull(McpHistoryFilterSupport.parseInstant("not-an-instant"));
        assertNull(McpHistoryFilterSupport.parseInstant(null));
    }

    @Test
    void matchesInstantRangeTreatsBoundsAsInclusive() {
        Instant from = Instant.parse("2026-05-31T01:00:00Z");
        Instant to = Instant.parse("2026-05-31T01:05:00Z");

        assertTrue(McpHistoryFilterSupport.matchesInstantRange(from, from, to));
        assertTrue(McpHistoryFilterSupport.matchesInstantRange(to, from, to));
        assertFalse(McpHistoryFilterSupport.matchesInstantRange(from.minusMillis(1), from, to));
        assertFalse(McpHistoryFilterSupport.matchesInstantRange(to.plusMillis(1), from, to));
        assertFalse(McpHistoryFilterSupport.matchesInstantRange(null, from, to));
        assertTrue(McpHistoryFilterSupport.matchesInstantRange(null, null, null));
    }

    @Test
    void boundedPaginationDefaultsAndCapsValues() {
        assertEquals(50, McpHistoryFilterSupport.boundedPageLimit(0, 50, 200));
        assertEquals(50, McpHistoryFilterSupport.boundedPageLimit(-1, 50, 200));
        assertEquals(100, McpHistoryFilterSupport.boundedPageLimit(100, 50, 200));
        assertEquals(200, McpHistoryFilterSupport.boundedPageLimit(500, 50, 200));

        assertEquals(0, McpHistoryFilterSupport.boundedPageOffset(null, 0, 500));
        assertEquals(0, McpHistoryFilterSupport.boundedPageOffset(-1, 0, 500));
        assertEquals(25, McpHistoryFilterSupport.boundedPageOffset(25, 0, 500));
        assertEquals(500, McpHistoryFilterSupport.boundedPageOffset(1_000, 0, 500));
    }
}
