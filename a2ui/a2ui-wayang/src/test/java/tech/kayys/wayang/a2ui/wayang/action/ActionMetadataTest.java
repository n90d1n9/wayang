package tech.kayys.wayang.a2ui.wayang.action;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionMetadataTest {

    @Test
    void projectsOrderedInspectMetadata() {
        Map<String, Object> values = ActionMetadata.inspection(new AgentRunInspection(
                "run-1",
                status("run-1", AgentRunState.RUNNING, "Run is running."),
                null,
                "Inspected run."));

        assertThat(values.keySet()).containsExactly("known", "empty");
        assertThat(values)
                .containsEntry("known", true)
                .containsEntry("empty", false);
    }

    @Test
    void projectsOrderedHistoryMetadata() {
        Map<String, Object> values = ActionMetadata.history(new AgentRunHistory(
                AgentRunHistoryQuery.of("", 1, "tenant-a", "", "", 1),
                List.of(
                        status("run-1", AgentRunState.RUNNING, "Run is running."),
                        status("run-2", AgentRunState.COMPLETED, "Run completed.")),
                3,
                "Loaded run history."));

        assertThat(values.keySet()).containsExactly("totalRuns", "returnedRuns", "offset", "hasMore", "filtered");
        assertThat(values)
                .containsEntry("totalRuns", 3)
                .containsEntry("returnedRuns", 2)
                .containsEntry("offset", 1)
                .containsEntry("hasMore", false)
                .containsEntry("filtered", true);
    }

    @Test
    void projectsOrderedEventsMetadata() {
        Map<String, Object> values = ActionMetadata.events(new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of("completed", "run.completed", 1L, 10),
                List.of(
                        new AgentRunEvent(
                                "run-1",
                                1,
                                "run.running",
                                AgentRunState.RUNNING,
                                "Run is running.",
                                Map.of()),
                        new AgentRunEvent(
                                "run-1",
                                2,
                                "run.completed",
                                AgentRunState.COMPLETED,
                                "Run completed.",
                                Map.of())),
                2,
                "Loaded run events."));

        assertThat(values.keySet()).containsExactly(
                "totalEvents",
                "returnedEvents",
                "afterSequence",
                "nextAfterSequence",
                "filtered",
                "truncated");
        assertThat(values)
                .containsEntry("totalEvents", 2)
                .containsEntry("returnedEvents", 2)
                .containsEntry("afterSequence", 1L)
                .containsEntry("nextAfterSequence", 2L)
                .containsEntry("filtered", true)
                .containsEntry("truncated", false);
    }

    @Test
    void projectsOrderedWaitAndCancelMetadata() {
        Map<String, Object> wait = ActionMetadata.waitResult(new AgentRunWaitResult(
                "run-1",
                status("run-1", AgentRunState.COMPLETED, "Run completed."),
                true,
                false,
                1,
                0,
                "Run completed.",
                Map.of()));
        Map<String, Object> cancel = ActionMetadata.cancelResult(AgentRunCancelResult.cancelled(
                status("run-1", AgentRunState.CANCELLED, "Run was cancelled.")));

        assertThat(wait.keySet()).containsExactly("terminal", "timedOut", "attempts", "elapsedMillis");
        assertThat(wait)
                .containsEntry("terminal", true)
                .containsEntry("timedOut", false)
                .containsEntry("attempts", 1)
                .containsEntry("elapsedMillis", 0L);
        assertThat(cancel.keySet()).containsExactly("cancelled");
        assertThat(cancel).containsEntry("cancelled", true);
    }

    private static AgentRunStatus status(String runId, AgentRunState state, String message) {
        return new AgentRunStatus(new AgentRunHandle(runId, state, "react"), true, message, Map.of());
    }
}
