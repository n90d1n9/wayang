package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceTextTest {

    @Test
    void formatsHistorySummaryMessageAndRunLines() {
        AgentRunStatus running = runningStatus();
        AgentRunHistory history = singleRunningHistory(4);
        AgentRunHistory empty = emptyHistory();
        AgentRunHistory loaded = loadedHistoryWithoutMessage();

        assertThat(SurfaceText.historySummary(history))
                .isEqualTo("Showing 1-1 of 4 runs");
        assertThat(SurfaceText.historyMessage(history))
                .isEqualTo("Loaded run history.");
        assertThat(SurfaceText.historyMessage(empty))
                .isEqualTo("No run statuses are recorded.");
        assertThat(SurfaceText.historyMessage(loaded))
                .isEqualTo("Run history loaded.");
        assertThat(SurfaceText.runLine(running))
                .isEqualTo("run-1 - RUNNING - Run is running.");
    }

    @Test
    void formatsEventSummaryMessageAndLines() {
        AgentRunEvent event = runningEvent(2);
        AgentRunEvents events = singleRunningEventPageAfterOne();
        AgentRunEvents empty = emptyEvents();
        AgentRunEvents loaded = loadedEventsWithoutMessage();

        assertThat(SurfaceText.eventsSummary(events))
                .isEqualTo("1 of 3 events, next sequence 2");
        assertThat(SurfaceText.eventsMessage(events))
                .isEqualTo("Loaded run events.");
        assertThat(SurfaceText.eventsMessage(empty))
                .isEqualTo("No run events are recorded.");
        assertThat(SurfaceText.eventsMessage(loaded))
                .isEqualTo("Run events loaded.");
        assertThat(SurfaceText.eventLine(event))
                .isEqualTo("#2 - run.running - RUNNING - Run is running.");
    }

    private static AgentRunHistory singleRunningHistory(int totalRuns) {
        return new AgentRunHistory(
                AgentRunHistoryQuery.of("", 2, "", "", "", 0),
                List.of(runningStatus()),
                totalRuns,
                "Loaded run history.");
    }

    private static AgentRunHistory emptyHistory() {
        return new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(),
                0,
                "");
    }

    private static AgentRunHistory loadedHistoryWithoutMessage() {
        return new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(runningStatus()),
                1,
                "");
    }

    private static AgentRunEvents singleRunningEventPageAfterOne() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of("", "", 1L, 10),
                List.of(runningEvent(2)),
                3,
                "Loaded run events.");
    }

    private static AgentRunEvents emptyEvents() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(),
                0,
                "");
    }

    private static AgentRunEvents loadedEventsWithoutMessage() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(runningEvent(2)),
                1,
                "");
    }

    private static AgentRunEvent runningEvent(long sequence) {
        return new AgentRunEvent(
                "run-1",
                sequence,
                "run.running",
                AgentRunState.RUNNING,
                "Run is running.",
                Map.of());
    }

    private static AgentRunStatus runningStatus() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of());
    }
}
