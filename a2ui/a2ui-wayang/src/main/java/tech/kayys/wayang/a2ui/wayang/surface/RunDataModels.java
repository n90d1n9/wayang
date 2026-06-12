package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Objects;

/**
 * Builds top-level data-model entries for Wayang run status, events, and
 * history surfaces.
 */
public final class RunDataModels {

    private RunDataModels() {
    }

    public static A2uiDataEntry[] status(AgentRunStatus status) {
        AgentRunStatus resolved = Objects.requireNonNull(status, "status");
        return new A2uiDataEntry[] {
                A2uiDataEntry.string("runId", resolved.handle().runId()),
                A2uiDataEntry.string("state", resolved.handle().state().name()),
                A2uiDataEntry.string("strategy", resolved.handle().strategy()),
                A2uiDataEntry.string("message", resolved.message()),
                A2uiDataEntry.bool("known", resolved.known())
        };
    }

    public static A2uiDataEntry[] events(AgentRunEvents events) {
        AgentRunEvents resolved = Objects.requireNonNull(events, "events");
        return new A2uiDataEntry[] {
                A2uiDataEntry.string("runId", resolved.runId()),
                A2uiDataEntry.number("totalEvents", resolved.totalEvents()),
                A2uiDataEntry.number("returnedEvents", resolved.returnedEvents()),
                A2uiDataEntry.number("afterSequence", resolved.cursor().afterSequence()),
                A2uiDataEntry.number("nextAfterSequence", resolved.nextAfterSequence()),
                A2uiDataEntry.bool("truncated", resolved.truncated()),
                A2uiDataEntry.bool("empty", resolved.empty()),
                A2uiDataEntry.string("message", SurfaceText.eventsMessage(resolved)),
                A2uiDataEntry.map("summary", List.of(
                        A2uiDataEntry.number("totalEvents", resolved.summary().totalEvents()),
                        A2uiDataEntry.number("returnedEvents", resolved.summary().returnedEvents()),
                        A2uiDataEntry.map("stateCounts", SurfaceData.counts(resolved.stateCounts())),
                        A2uiDataEntry.map("typeCounts", SurfaceData.counts(resolved.typeCounts())))),
                A2uiDataEntry.map("events", SurfaceData.events(resolved.events()))
        };
    }

    public static A2uiDataEntry[] history(AgentRunHistory history) {
        AgentRunHistory resolved = Objects.requireNonNull(history, "history");
        return new A2uiDataEntry[] {
                A2uiDataEntry.number("totalRuns", resolved.totalRuns()),
                A2uiDataEntry.number("returnedRuns", resolved.returnedRuns()),
                A2uiDataEntry.number("offset", resolved.offset()),
                A2uiDataEntry.bool("hasMore", resolved.hasMore()),
                A2uiDataEntry.bool("empty", resolved.empty()),
                A2uiDataEntry.string("message", SurfaceText.historyMessage(resolved)),
                A2uiDataEntry.map("summary", List.of(
                        A2uiDataEntry.number("totalRuns", resolved.summary().totalRuns()),
                        A2uiDataEntry.number("returnedRuns", resolved.summary().returnedRuns()),
                        A2uiDataEntry.map("stateCounts", SurfaceData.counts(resolved.stateCounts())),
                        A2uiDataEntry.map("surfaceCounts", SurfaceData.counts(
                                resolved.surfaceCounts())),
                        A2uiDataEntry.map("strategyCounts", SurfaceData.counts(
                                resolved.strategyCounts())))),
                A2uiDataEntry.map("runs", SurfaceData.runs(resolved.runs()))
        };
    }
}
