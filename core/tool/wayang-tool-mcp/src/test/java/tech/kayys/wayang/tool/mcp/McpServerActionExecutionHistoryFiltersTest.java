package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerActionExecutionHistoryFiltersTest {

    @Test
    void normalizesFiltersAndBoundsLimits() {
        McpServerActionExecutionHistoryFilters filters =
                McpServerActionExecutionHistoryFilters.latest(
                        " docs:run-sync ",
                        " Docs ",
                        "run-sync",
                        "executed",
                        true,
                        "automatable",
                        "low",
                        true,
                        1000);

        assertEquals("docs:RUN_SYNC", filters.actionId());
        assertEquals("docs", filters.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, filters.actionCode());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, filters.status());
        assertEquals(Boolean.TRUE, filters.executed());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, filters.executionMode());
        assertEquals(McpServerActionRiskLevel.LOW, filters.riskLevel());
        assertEquals(Boolean.TRUE, filters.hasWarnings());
        assertEquals(null, filters.startedAtFrom());
        assertEquals(null, filters.finishedAtTo());
        assertEquals(0, filters.offset());
        assertEquals(200, filters.limit());
        assertEquals(500, filters.scanLimit());
        assertTrue(filters.matches(entry(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "DOCS",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of("warning"))));
        assertTrue(filters.matches(entry(
                "docs:run-sync",
                "DOCS",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of("warning"))));
        assertTrue(filters.matches(entry(
                null,
                "DOCS",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of("warning"))));
        assertFalse(filters.matches(entry(
                "crm:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "crm",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of("warning"))));
        assertFalse(filters.matches(entry(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of())));
    }

    @Test
    void listFiltersUseDefaultLimitWithLookaheadScanLimit() {
        McpServerActionExecutionHistoryFilters filters =
                McpServerActionExecutionHistoryFilters.of(null, null, null, -1);

        assertEquals(null, filters.actionId());
        assertEquals(0, filters.offset());
        assertEquals(50, filters.limit());
        assertEquals(51, filters.scanLimit());
    }

    @Test
    void listFiltersScanThroughRequestedOffset() {
        McpServerActionExecutionHistoryFilters filters = McpServerActionExecutionHistoryFilters.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                25,
                10);

        assertEquals(25, filters.offset());
        assertEquals(10, filters.limit());
        assertEquals(36, filters.scanLimit());
    }

    @Test
    void filtersByStartedAndFinishedTimeWindows() {
        Instant startedAt = Instant.parse("2026-05-30T05:00:00Z");
        Instant finishedAt = Instant.parse("2026-05-30T05:00:05Z");
        McpServerActionExecutionHistoryFilters filters = McpServerActionExecutionHistoryFilters.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                startedAt.minusSeconds(1).toString(),
                startedAt.plusSeconds(1).toString(),
                finishedAt.minusSeconds(1).toString(),
                finishedAt.plusSeconds(1).toString(),
                10);

        assertEquals(startedAt.minusSeconds(1), filters.startedAtFrom());
        assertEquals(finishedAt.plusSeconds(1), filters.finishedAtTo());
        assertTrue(filters.matches(entry(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of(),
                startedAt,
                finishedAt)));
        assertFalse(filters.matches(entry(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of(),
                startedAt.minusSeconds(2),
                finishedAt)));
        assertFalse(filters.matches(entry(
                "docs:" + McpServerActionCatalog.ACTION_RUN_SYNC,
                "docs",
                McpServerActionCatalog.ACTION_RUN_SYNC,
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                List.of(),
                startedAt,
                finishedAt.plusSeconds(2))));
    }

    @Test
    void derivesStableActionIdentityAndSortTime() {
        Instant startedAt = Instant.parse("2026-05-30T05:00:00Z");
        McpServerActionExecutionHistoryEntry entry = new McpServerActionExecutionHistoryEntry(
                "Docs:run-sync",
                McpServerActionExecutionResult.STATUS_EXECUTED,
                true,
                "done",
                "Docs",
                "run-sync",
                McpServerActionExecutionMode.AUTOMATABLE,
                McpServerActionRiskLevel.LOW,
                startedAt,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                List.of());

        assertEquals("docs|RUN_SYNC", McpServerActionExecutionHistorySummaryKeys.actionIdentityKey(entry));
        assertEquals(startedAt, McpServerActionExecutionHistorySummaryKeys.sortFinishedAt(entry));
    }

    private static McpServerActionExecutionHistoryEntry entry(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            boolean executed,
            String executionMode,
            String riskLevel,
            List<String> warnings) {
        Instant now = Instant.now();
        return entry(
                actionId,
                serverName,
                actionCode,
                status,
                executed,
                executionMode,
                riskLevel,
                warnings,
                now,
                now);
    }

    private static McpServerActionExecutionHistoryEntry entry(
            String actionId,
            String serverName,
            String actionCode,
            String status,
            boolean executed,
            String executionMode,
            String riskLevel,
            List<String> warnings,
            Instant startedAt,
            Instant finishedAt) {
        return new McpServerActionExecutionHistoryEntry(
                actionId,
                status,
                executed,
                status,
                serverName,
                actionCode,
                executionMode,
                riskLevel,
                startedAt,
                finishedAt,
                0,
                null,
                null,
                null,
                null,
                null,
                warnings);
    }
}
