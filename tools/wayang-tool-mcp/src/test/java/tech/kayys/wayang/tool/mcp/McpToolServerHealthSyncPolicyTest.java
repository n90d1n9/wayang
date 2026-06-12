package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.kayys.wayang.tool.mcp.McpToolServerHealthTestFixtures.scheduledServer;

class McpToolServerHealthSyncPolicyTest {

    private static final Instant NOW = Instant.parse("2026-06-03T04:00:00Z");

    @Test
    void blankScheduleIsNotDue() {
        McpToolServerHealthSyncPolicyStatus status =
                McpToolServerHealthSyncPolicy.from(scheduledServer(null, null, true), NOW);

        assertNull(status.nextSyncAt());
        assertEquals(false, status.syncDue());
        assertNull(status.error());
    }

    @Test
    void enabledServerWithoutLastSyncIsDue() {
        McpToolServerHealthSyncPolicyStatus status =
                McpToolServerHealthSyncPolicy.from(scheduledServer("PT5M", null, true), NOW);

        assertNull(status.nextSyncAt());
        assertEquals(true, status.syncDue());
        assertNull(status.error());
    }

    @Test
    void disabledServerWithoutLastSyncIsNotDue() {
        McpToolServerHealthSyncPolicyStatus status =
                McpToolServerHealthSyncPolicy.from(scheduledServer("PT5M", null, false), NOW);

        assertNull(status.nextSyncAt());
        assertEquals(false, status.syncDue());
        assertNull(status.error());
    }

    @Test
    void computesDueStateFromLastSyncAndInterval() {
        McpToolServerHealthSyncPolicyStatus due = McpToolServerHealthSyncPolicy.from(
                scheduledServer("PT5M", NOW.minusSeconds(300), true),
                NOW);
        McpToolServerHealthSyncPolicyStatus notDue = McpToolServerHealthSyncPolicy.from(
                scheduledServer("PT5M", NOW.minusSeconds(60), true),
                NOW);
        McpToolServerHealthSyncPolicyStatus disabled = McpToolServerHealthSyncPolicy.from(
                scheduledServer("PT5M", NOW.minusSeconds(300), false),
                NOW);

        assertEquals(NOW, due.nextSyncAt());
        assertEquals(true, due.syncDue());
        assertNull(due.error());
        assertEquals(NOW.plusSeconds(240), notDue.nextSyncAt());
        assertEquals(false, notDue.syncDue());
        assertEquals(NOW, disabled.nextSyncAt());
        assertEquals(false, disabled.syncDue());
    }

    @Test
    void invalidScheduleReturnsError() {
        McpToolServerHealthSyncPolicyStatus status =
                McpToolServerHealthSyncPolicy.from(
                        scheduledServer("definitely-not-an-interval", NOW, true),
                        NOW);

        assertNull(status.nextSyncAt());
        assertEquals(false, status.syncDue());
        assertNotNull(status.error());
    }

    @Test
    void nullServerReturnsEmptyStatus() {
        McpToolServerHealthSyncPolicyStatus status = McpToolServerHealthSyncPolicy.from(null, NOW);

        assertNull(status.nextSyncAt());
        assertEquals(false, status.syncDue());
        assertNull(status.error());
    }
}
