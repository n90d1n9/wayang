package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurfaceDataTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildsRunDataEntries() {
        AgentRunStatus status = runningStatus();

        List<A2uiDataEntry> entries = SurfaceData.runs(List.of(status));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).toPayload()).containsEntry("key", "run0");
        List<Map<String, Object>> runValues = (List<Map<String, Object>>) entries.get(0).toPayload().get("valueMap");
        assertThat(runValues)
                .anySatisfy(value -> assertThat(value).containsEntry("key", "runId")
                        .containsEntry("valueString", "run-1"))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "state")
                        .containsEntry("valueString", "RUNNING"))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "stateWireName")
                        .containsEntry("valueString", "running"))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "terminal")
                        .containsEntry("valueBoolean", false));
        assertThatThrownBy(() -> entries.add(A2uiDataEntry.string("run1", "run-2")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(SurfaceData.runs(null)).isEmpty();
    }

    @Test
    void skipsNullRunDataEntries() {
        List<AgentRunStatus> statuses = new ArrayList<>();
        statuses.add(null);
        statuses.add(runningStatus());

        List<A2uiDataEntry> entries = SurfaceData.runs(statuses);

        assertThat(entries)
                .extracting(A2uiDataEntry::key)
                .containsExactly("run0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsEventDataEntries() {
        AgentRunEvent event = runningEvent(2);

        List<A2uiDataEntry> entries = SurfaceData.events(List.of(event));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).toPayload()).containsEntry("key", "event2");
        List<Map<String, Object>> eventValues =
                (List<Map<String, Object>>) entries.get(0).toPayload().get("valueMap");
        assertThat(eventValues)
                .anySatisfy(value -> assertThat(value).containsEntry("key", "sequence")
                        .containsEntry("valueNumber", 2L))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "runId")
                        .containsEntry("valueString", "run-1"))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "type")
                        .containsEntry("valueString", "run.running"))
                .anySatisfy(value -> assertThat(value).containsEntry("key", "stateWireName")
                        .containsEntry("valueString", "running"));
        assertThat(SurfaceData.events(null)).isEmpty();
    }

    @Test
    void skipsNullEventDataEntries() {
        List<AgentRunEvent> events = new ArrayList<>();
        events.add(null);
        events.add(runningEvent(2));

        List<A2uiDataEntry> entries = SurfaceData.events(events);

        assertThat(entries)
                .extracting(A2uiDataEntry::key)
                .containsExactly("event2");
    }

    @Test
    void buildsCountDataEntriesInInputOrder() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("RUNNING", 2);
        counts.put("COMPLETED", 1);

        List<A2uiDataEntry> entries = SurfaceData.counts(counts);

        assertThat(entries)
                .extracting(A2uiDataEntry::key)
                .containsExactly("RUNNING", "COMPLETED");
        assertThat(entries)
                .extracting(A2uiDataEntry::value)
                .containsExactly(2, 1);
        assertThat(SurfaceData.counts(null)).isEmpty();
        assertThat(SurfaceData.counts(Map.of())).isEmpty();
    }

    private static AgentRunStatus runningStatus() {
        return new AgentRunStatus(
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"),
                true,
                "Run is running.",
                Map.of());
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
