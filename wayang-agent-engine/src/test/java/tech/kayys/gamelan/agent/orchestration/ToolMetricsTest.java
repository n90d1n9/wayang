package tech.kayys.gamelan.agent.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ToolMetricsTest {

    private ToolMetrics metrics;

    @BeforeEach
    void setUp() { metrics = new ToolMetrics(); }

    @Test
    void recordsCallCount() {
        long start = System.currentTimeMillis() - 100;
        metrics.record("read_file", start, false);
        metrics.record("read_file", start, false);
        assertThat(metrics.stat("read_file").calls.get()).isEqualTo(2);
    }

    @Test
    void recordsErrorCount() {
        long start = System.currentTimeMillis() - 50;
        metrics.record("run_command", start, false);
        metrics.record("run_command", start, true);
        ToolMetrics.ToolStat stat = metrics.stat("run_command");
        assertThat(stat.calls.get()).isEqualTo(2);
        assertThat(stat.errors.get()).isEqualTo(1);
        assertThat(stat.errorRate()).isEqualTo(50.0);
    }

    @Test
    void tracksMinMaxLatency() {
        // Simulate different durations by adjusting startMs
        long now = System.currentTimeMillis();
        metrics.record("tool", now - 100, false); // ~100ms
        metrics.record("tool", now - 500, false); // ~500ms
        ToolMetrics.ToolStat stat = metrics.stat("tool");
        assertThat(stat.minMs.get()).isLessThanOrEqualTo(200);
        assertThat(stat.maxMs.get()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void mostUsedReturnsHighestCallCount() {
        long start = System.currentTimeMillis() - 10;
        metrics.record("rare_tool", start, false);
        metrics.record("common_tool", start, false);
        metrics.record("common_tool", start, false);
        metrics.record("common_tool", start, false);
        assertThat(metrics.mostUsed()).contains("common_tool");
    }

    @Test
    void resetClearsAllStats() {
        long start = System.currentTimeMillis() - 10;
        metrics.record("some_tool", start, false);
        metrics.reset();
        assertThat(metrics.toolNames()).isEmpty();
        assertThat(metrics.totalCalls()).isEqualTo(0);
    }

    @Test
    void summaryIsNonBlankAfterRecording() {
        long start = System.currentTimeMillis() - 200;
        metrics.record("read_file", start, false);
        String summary = metrics.summary();
        assertThat(summary).contains("read_file");
        assertThat(summary).contains("calls");
    }

    @Test
    void summaryReturnsNoneMessageWhenEmpty() {
        assertThat(metrics.summary()).contains("no tool calls");
    }

    @Test
    void toJsonContainsToolsField() {
        long start = System.currentTimeMillis() - 100;
        metrics.record("search_files", start, false);
        String json = metrics.toJson();
        assertThat(json).contains("\"tools\"");
        assertThat(json).contains("search_files");
        assertThat(json).contains("\"calls\"");
    }

    @Test
    void ringBufferCapsBeyond20() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 25; i++) {
            metrics.record("tool", start - 10, false);
        }
        // Ring buffer capped at 20
        assertThat(metrics.stat("tool").recentHistory()).hasSize(20);
    }

    @Test
    void totalCallsAndErrorsAggregateAcrossTools() {
        long start = System.currentTimeMillis() - 10;
        metrics.record("tool-a", start, false);
        metrics.record("tool-b", start, true);
        metrics.record("tool-b", start, false);
        assertThat(metrics.totalCalls()).isEqualTo(3);
        assertThat(metrics.totalErrors()).isEqualTo(1);
    }
}
