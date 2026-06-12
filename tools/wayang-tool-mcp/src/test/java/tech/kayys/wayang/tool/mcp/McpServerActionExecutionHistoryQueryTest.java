package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionExecutionHistoryQueryTest {

    @Test
    void createsListAndLatestFiltersFromSameCriteria() {
        McpServerActionExecutionHistoryQuery query = McpServerActionExecutionHistoryQuery.builder()
                .withActionId(" docs:run-sync ")
                .withServerName(" Docs ")
                .withActionCode("run-sync")
                .withStatus("executed")
                .withExecuted(true)
                .withExecutionMode("automatable")
                .withRiskLevel("low")
                .withWarnings(true)
                .withStartedAtFrom("2026-05-30T05:00:00Z")
                .withStartedAtTo("2026-05-30T05:05:00Z")
                .withFinishedAtFrom("2026-05-30T05:00:01Z")
                .withFinishedAtTo("2026-05-30T05:05:01Z")
                .withOffset(25)
                .withLimit(1000)
                .build();

        McpServerActionExecutionHistoryFilters filters = query.filters();
        McpServerActionExecutionHistoryFilters latestFilters = query.latestFilters();

        assertEquals("docs:RUN_SYNC", filters.actionId());
        assertEquals("docs", filters.serverName());
        assertEquals(McpServerActionCatalog.ACTION_RUN_SYNC, filters.actionCode());
        assertEquals(McpServerActionExecutionResult.STATUS_EXECUTED, filters.status());
        assertEquals(Boolean.TRUE, filters.executed());
        assertEquals(McpServerActionExecutionMode.AUTOMATABLE, filters.executionMode());
        assertEquals(McpServerActionRiskLevel.LOW, filters.riskLevel());
        assertEquals(Boolean.TRUE, filters.hasWarnings());
        assertEquals(Instant.parse("2026-05-30T05:00:00Z"), filters.startedAtFrom());
        assertEquals(Instant.parse("2026-05-30T05:05:00Z"), filters.startedAtTo());
        assertEquals(Instant.parse("2026-05-30T05:00:01Z"), filters.finishedAtFrom());
        assertEquals(Instant.parse("2026-05-30T05:05:01Z"), filters.finishedAtTo());
        assertEquals(25, filters.offset());
        assertEquals(200, filters.limit());
        assertEquals(226, filters.scanLimit());
        assertEquals(filters.limit(), latestFilters.limit());
        assertEquals(filters.offset(), latestFilters.offset());
        assertEquals(filters.actionId(), latestFilters.actionId());
        assertEquals(filters.startedAtFrom(), latestFilters.startedAtFrom());
        assertEquals(filters.finishedAtTo(), latestFilters.finishedAtTo());
        assertEquals(500, latestFilters.scanLimit());
    }
}
