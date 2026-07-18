package tech.kayys.wayang.agent.core.observability;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTelemetryContractTest {

    @Test
    void recordsMemoryOperationStatsByTierAndOperation() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());

        telemetry.recordMemoryOperation(" read ", " short-term ", Duration.ofMillis(10));
        telemetry.recordMemoryOperation("read", "short-term", Duration.ofMillis(30), false);
        telemetry.recordMemoryOperation("write", "long-term", Duration.ofMillis(20), true);

        Map<String, AgentTelemetry.MemoryOperationStats> stats = telemetry.getMemoryOperationStats();
        AgentTelemetry.MemoryOperationStats readStats = stats.get("short-term:read");
        AgentTelemetry.MemoryOperationStats writeStats = stats.get("long-term:write");

        assertEquals(2, readStats.count());
        assertEquals(1, readStats.failureCount());
        assertEquals(40, readStats.totalDurationMs());
        assertEquals(30, readStats.maxDurationMs());
        assertEquals(20.0, readStats.averageDurationMs());
        assertEquals(0.5, readStats.failureRate());
        assertEquals(1, writeStats.count());
        assertThrows(UnsupportedOperationException.class,
                () -> stats.put("new", new AgentTelemetry.MemoryOperationStats("x", "y", 1, 0, 1, 1)));
    }

    @Test
    void normalizesBlankMemoryValuesAndNegativeDuration() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());

        telemetry.recordMemoryOperation(" ", null, Duration.ofMillis(-5), false);

        AgentTelemetry.MemoryOperationStats stats = telemetry.getMemoryOperationStats()
                .get("unknown:unknown");

        assertEquals("unknown", stats.operation());
        assertEquals("unknown", stats.tier());
        assertEquals(1, stats.count());
        assertEquals(1, stats.failureCount());
        assertEquals(0, stats.totalDurationMs());
        assertEquals(0.0, stats.averageDurationMs());
    }

    @Test
    void recordersAreNullSafe() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());

        assertDoesNotThrow(() -> telemetry.recordInference(null, null, null, false));
        assertDoesNotThrow(() -> telemetry.recordToolExecution(null, null, false));
        assertDoesNotThrow(() -> telemetry.recordMemoryOperation(null, null, null, false));
    }

    @Test
    void tracksAndClearsActiveSpans() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());

        String spanId = telemetry.startSpan(" ");
        telemetry.addEvent(spanId, null);
        telemetry.setAttribute(spanId, " key ", null);

        assertEquals(1, telemetry.activeSpanCount());
        telemetry.endSpan(spanId);
        assertEquals(0, telemetry.activeSpanCount());

        telemetry.endSpan("missing");
        telemetry.addEvent("missing", "ignored");
        telemetry.setAttribute("missing", "key", "value");
        assertEquals(0, telemetry.activeSpanCount());
    }

    @Test
    void executeAgentRecordsSuccessAndRethrowsFailures() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());

        String result = telemetry.executeAgent(null, null, () -> "done");

        assertEquals("done", result);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> telemetry.executeAgent("react", "agent-a", () -> {
                    throw new IllegalStateException("boom");
                }));
        assertEquals("boom", error.getMessage());
    }

    @Test
    void shutdownClearsActiveSpans() {
        AgentTelemetry telemetry = AgentTelemetry.initialize(OpenTelemetry.noop());
        telemetry.startSpan("span-a");
        telemetry.startSpan("span-b");

        telemetry.shutdown();

        assertEquals(0, telemetry.activeSpanCount());
        assertTrue(telemetry.getMemoryOperationStats().isEmpty());
    }

    @Test
    void requiresOpenTelemetryInstance() {
        assertThrows(NullPointerException.class, () -> AgentTelemetry.initialize(null));
    }
}
