package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.entity.McpServerRegistry;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolDiscoverySyncScheduleTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void skipsNullDisabledAndUnscheduledServers() {
        assertFalse(McpToolDiscoverySyncSchedule.dueDecision(null, NOW).due());

        McpServerRegistry disabled = server("docs", "PT5M", null);
        disabled.setEnabled(false);
        assertFalse(McpToolDiscoverySyncSchedule.dueDecision(disabled, NOW).due());

        McpServerRegistry unscheduled = server("docs", null, null);
        assertFalse(McpToolDiscoverySyncSchedule.dueDecision(unscheduled, NOW).due());
    }

    @Test
    void marksNeverSyncedOrExpiredServersAsDue() {
        McpToolDiscoverySyncDueDecision neverSynced = McpToolDiscoverySyncSchedule.dueDecision(
                server("docs", "PT5M", null),
                NOW);
        McpToolDiscoverySyncDueDecision expired = McpToolDiscoverySyncSchedule.dueDecision(
                server("docs", "PT5M", NOW.minusSeconds(301)),
                NOW);

        assertTrue(neverSynced.due());
        assertNull(neverSynced.warning());
        assertTrue(expired.due());
        assertNull(expired.warning());
    }

    @Test
    void skipsServersStillInsideScheduleInterval() {
        McpToolDiscoverySyncDueDecision decision = McpToolDiscoverySyncSchedule.dueDecision(
                server("docs", "PT5M", NOW.minusSeconds(299)),
                NOW);

        assertFalse(decision.due());
        assertNull(decision.warning());
    }

    @Test
    void convertsInvalidSchedulesIntoWarnings() {
        McpToolDiscoverySyncDueDecision decision = McpToolDiscoverySyncSchedule.dueDecision(
                server("docs", "definitely-not-an-interval", null),
                NOW);

        assertFalse(decision.due());
        assertEquals(
                "Live MCP tools sync skipped for server docs: Invalid interval format: "
                        + "definitely-not-an-interval. Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m).",
                decision.warning());
    }

    private McpServerRegistry server(String name, String schedule, Instant lastSyncAt) {
        McpServerRegistry server = new McpServerRegistry();
        server.setRequestId("tenant-1");
        server.setName(name);
        server.setTransport(McpServerTransports.HTTP);
        server.setUrl("http://docs.local/mcp");
        server.setEnabled(true);
        server.setSyncSchedule(schedule);
        server.setLastSyncAt(lastSyncAt);
        server.setCreatedAt(NOW.minusSeconds(600));
        server.setUpdatedAt(NOW.minusSeconds(300));
        return server;
    }
}
