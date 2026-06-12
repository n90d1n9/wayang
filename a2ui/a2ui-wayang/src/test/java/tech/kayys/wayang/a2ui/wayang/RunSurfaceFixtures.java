package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Map;

/**
 * Shared run-surface samples for A2UI renderer and contract tests.
 */
final class RunSurfaceFixtures {

    static final String RUNNING_MESSAGE = "Run is running.";
    static final String COMPLETED_MESSAGE = "Run completed.";
    static final String EVENTS_MESSAGE = "Loaded run events.";
    static final String HISTORY_MESSAGE = "Loaded run history.";
    static final String INSPECTION_MESSAGE = "Inspected run.";

    private RunSurfaceFixtures() {
    }

    static AgentRunStatus runningStatus() {
        return runningStatus(Map.of());
    }

    static AgentRunStatus runningStatusWithSurface() {
        return runningStatus(Map.of("surface", "coding-agent"));
    }

    static AgentRunStatus completedStatus() {
        return completedStatus("run-2", Map.of());
    }

    static AgentRunStatus completedStatusWithSurface() {
        return completedStatus("run-2", Map.of("surface", "coding-agent"));
    }

    static AgentRunStatus completedStatus(String runId) {
        return completedStatus(runId, Map.of());
    }

    static AgentRunEvents runEventsPage() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of("", "", 1L, 10),
                List.of(
                        runningEvent(2),
                        completedEvent(3)),
                3,
                EVENTS_MESSAGE);
    }

    static AgentRunEvents singleRunningEventPage() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(runningEvent(1)),
                1,
                EVENTS_MESSAGE);
    }

    static AgentRunEvents singleRunningEventPageAfterOne() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of("", "", 1L, 10),
                List.of(runningEvent(2)),
                3,
                EVENTS_MESSAGE);
    }

    static AgentRunEvents emptyEvents() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(),
                0,
                "");
    }

    static AgentRunEvents loadedEventsWithoutMessage() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.all(),
                List.of(runningEvent(2)),
                1,
                "");
    }

    static AgentRunHistory runHistory(int totalRuns) {
        return new AgentRunHistory(
                AgentRunHistoryQuery.of("", 2, "", "", "", 0),
                List.of(runningStatusWithSurface(), completedStatusWithSurface()),
                totalRuns,
                HISTORY_MESSAGE);
    }

    static AgentRunHistory singleRunningHistory(int totalRuns) {
        return new AgentRunHistory(
                AgentRunHistoryQuery.of("", 2, "", "", "", 0),
                List.of(runningStatus()),
                totalRuns,
                HISTORY_MESSAGE);
    }

    static AgentRunHistory emptyHistory() {
        return new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(),
                0,
                "");
    }

    static AgentRunHistory loadedHistoryWithoutMessage() {
        return new AgentRunHistory(
                AgentRunHistoryQuery.all(),
                List.of(runningStatus()),
                1,
                "");
    }

    static AgentRunInspection runInspection() {
        return runInspection(singleRunningEventPage());
    }

    static AgentRunInspection runInspection(AgentRunEvents events) {
        return new AgentRunInspection("run-1", runningStatus(), events, INSPECTION_MESSAGE);
    }

    static AgentRunEvent runningEvent(long sequence) {
        return new AgentRunEvent(
                "run-1",
                sequence,
                "run.running",
                AgentRunState.RUNNING,
                RUNNING_MESSAGE,
                Map.of());
    }

    private static AgentRunStatus runningStatus(Map<String, Object> metadata) {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                RUNNING_MESSAGE,
                metadata);
    }

    private static AgentRunStatus completedStatus(String runId, Map<String, Object> metadata) {
        return new AgentRunStatus(
                AgentRunHandle.completed(runId, "react"),
                true,
                COMPLETED_MESSAGE,
                metadata);
    }

    private static AgentRunEvent completedEvent(long sequence) {
        return new AgentRunEvent(
                "run-1",
                sequence,
                "run.completed",
                AgentRunState.COMPLETED,
                COMPLETED_MESSAGE,
                Map.of());
    }
}
