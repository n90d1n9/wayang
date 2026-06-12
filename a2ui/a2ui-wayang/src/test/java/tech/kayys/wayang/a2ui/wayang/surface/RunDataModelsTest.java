package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunDataModelsTest {

    @Test
    void buildsStatusEntriesInRendererOrder() {
        A2uiDataEntry[] entries = RunDataModels.status(status());

        assertThat(Arrays.stream(entries).map(A2uiDataEntry::key))
                .containsExactly("runId", "state", "strategy", "message", "known");
        assertThat(entries[0].toPayload()).containsEntry("valueString", "run-1");
        assertThat(entries[1].toPayload()).containsEntry("valueString", "RUNNING");
        assertThat(entries[4].toPayload()).containsEntry("valueBoolean", true);
    }

    @Test
    void buildsEventEntriesWithSummaryAndEventMap() {
        A2uiDataEntry[] entries = RunDataModels.events(events());

        assertThat(Arrays.stream(entries).map(A2uiDataEntry::key))
                .containsExactly(
                        "runId",
                        "totalEvents",
                        "returnedEvents",
                        "afterSequence",
                        "nextAfterSequence",
                        "truncated",
                        "empty",
                        "message",
                        "summary",
                        "events");
        assertThat(entries[4].toPayload()).containsEntry("valueNumber", 2L);
        assertThat(entries[8].toString()).contains("stateCounts").contains("typeCounts");
        assertThat(entries[9].toString()).contains("event2").contains("run.running");
    }

    @Test
    void buildsHistoryEntriesWithSummaryAndRunsMap() {
        A2uiDataEntry[] entries = RunDataModels.history(history());

        assertThat(Arrays.stream(entries).map(A2uiDataEntry::key))
                .containsExactly(
                        "totalRuns",
                        "returnedRuns",
                        "offset",
                        "hasMore",
                        "empty",
                        "message",
                        "summary",
                        "runs");
        assertThat(entries[3].toPayload()).containsEntry("valueBoolean", true);
        assertThat(entries[6].toString()).contains("stateCounts").contains("surfaceCounts").contains("strategyCounts");
        assertThat(entries[7].toString()).contains("run0").contains("run-1");
    }

    private static AgentRunStatus status() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of("surface", "coding-agent"));
    }

    private static AgentRunEvents events() {
        return new AgentRunEvents(
                "run-1",
                AgentRunEventsQuery.of("", "", 1L, 10),
                List.of(runningEvent(2)),
                3,
                "Loaded run events.");
    }

    private static AgentRunHistory history() {
        return new AgentRunHistory(
                AgentRunHistoryQuery.of("", 2, "", "", "", 0),
                List.of(status()),
                4,
                "Loaded run history.");
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
}
