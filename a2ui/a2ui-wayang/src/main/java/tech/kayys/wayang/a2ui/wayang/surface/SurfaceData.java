package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunStates;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds reusable data-model entries for Wayang run and event surfaces.
 */
public final class SurfaceData {

    private SurfaceData() {
    }

    public static List<A2uiDataEntry> runs(List<AgentRunStatus> runs) {
        List<AgentRunStatus> resolved = RecordCollections.nonNullList(runs);
        List<A2uiDataEntry> entries = new ArrayList<>();
        for (int index = 0; index < resolved.size(); index++) {
            AgentRunStatus status = resolved.get(index);
            entries.add(A2uiDataEntry.map("run" + index, List.of(
                    A2uiDataEntry.string("runId", status.handle().runId()),
                    A2uiDataEntry.string("state", status.handle().state().name()),
                    A2uiDataEntry.string("stateWireName", AgentRunStates.wireName(status.handle().state())),
                    A2uiDataEntry.string("strategy", status.handle().strategy()),
                    A2uiDataEntry.string("message", status.message()),
                    A2uiDataEntry.bool("known", status.known()),
                    A2uiDataEntry.bool("terminal", status.handle().terminal()))));
        }
        return List.copyOf(entries);
    }

    public static List<A2uiDataEntry> events(List<AgentRunEvent> events) {
        List<AgentRunEvent> resolved = RecordCollections.nonNullList(events);
        List<A2uiDataEntry> entries = new ArrayList<>();
        for (AgentRunEvent event : resolved) {
            entries.add(A2uiDataEntry.map("event" + event.sequence(), List.of(
                    A2uiDataEntry.number("sequence", event.sequence()),
                    A2uiDataEntry.string("runId", event.runId()),
                    A2uiDataEntry.string("type", event.type()),
                    A2uiDataEntry.string("state", event.state().name()),
                    A2uiDataEntry.string("stateWireName", AgentRunStates.wireName(event.state())),
                    A2uiDataEntry.string("message", event.message()))));
        }
        return List.copyOf(entries);
    }

    public static List<A2uiDataEntry> counts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
                .map(entry -> A2uiDataEntry.number(entry.getKey(), entry.getValue()))
                .toList();
    }
}
