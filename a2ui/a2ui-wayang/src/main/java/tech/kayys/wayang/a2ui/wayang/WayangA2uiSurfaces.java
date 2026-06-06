package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiAction;
import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;
import tech.kayys.wayang.a2ui.core.A2uiBeginRendering;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.core.A2uiDataModelUpdate;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.core.A2uiSurfaceUpdate;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStates;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maps common Wayang SDK models to A2UI surface message streams.
 */
public final class WayangA2uiSurfaces {

    private WayangA2uiSurfaces() {
    }

    public static List<A2uiServerMessage> runStatus(AgentRunStatus status) {
        return runStatus(status, WayangA2uiSurfaceOptions.readOnly());
    }

    public static List<A2uiServerMessage> runStatus(AgentRunStatus status, WayangA2uiSurfaceOptions options) {
        AgentRunStatus resolved = Objects.requireNonNull(status, "status");
        WayangA2uiSurfaceOptions resolvedOptions = resolveOptions(options);
        String surfaceId = "wayang-run-" + safeId(resolved.handle().runId());
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String stateId = surfaceId + "-state";
        String messageId = surfaceId + "-message";

        List<A2uiComponent> components = new ArrayList<>();
        List<String> rootChildren = new ArrayList<>();
        rootChildren.add(titleId);
        rootChildren.add(stateId);
        rootChildren.add(messageId);

        components.add(A2uiComponents.text(titleId, "Run " + resolved.handle().runId()));
        components.add(A2uiComponents.text(stateId, "State: " + resolved.handle().state().name()));
        components.add(A2uiComponents.text(messageId, resolved.message()));

        if (resolvedOptions.shows(WayangA2uiActions.RUN_INSPECT)) {
            addActionButton(
                    components,
                    rootChildren,
                    surfaceId + "-inspect",
                    surfaceId + "-inspect-label",
                    "Inspect run",
                    WayangA2uiActions.RUN_INSPECT,
                    resolvedOptions,
                    resolved.handle().runId(),
                    surfaceId);
        }
        if (resolvedOptions.shows(WayangA2uiActions.RUN_EVENTS)) {
            addActionButton(
                    components,
                    rootChildren,
                    surfaceId + "-events",
                    surfaceId + "-events-label",
                    "View events",
                    WayangA2uiActions.RUN_EVENTS,
                    resolvedOptions,
                    resolved.handle().runId(),
                    surfaceId);
        }
        if (!resolved.handle().terminal() && resolvedOptions.shows(WayangA2uiActions.RUN_WAIT)) {
            addActionButton(
                    components,
                    rootChildren,
                    surfaceId + "-wait",
                    surfaceId + "-wait-label",
                    "Wait for completion",
                    WayangA2uiActions.RUN_WAIT,
                    resolvedOptions,
                    resolved.handle().runId(),
                    surfaceId);
        }
        if (!resolved.handle().terminal() && resolvedOptions.shows(WayangA2uiActions.RUN_CANCEL)) {
            addActionButton(
                    components,
                    rootChildren,
                    surfaceId + "-cancel",
                    surfaceId + "-cancel-label",
                    "Cancel run",
                    WayangA2uiActions.RUN_CANCEL,
                    resolvedOptions,
                    resolved.handle().runId(),
                    surfaceId);
        }
        components.add(0, A2uiComponents.column(rootId, rootChildren));

        return List.of(
                A2uiDataModelUpdate.root(
                        surfaceId,
                        A2uiDataEntry.string("runId", resolved.handle().runId()),
                        A2uiDataEntry.string("state", resolved.handle().state().name()),
                        A2uiDataEntry.string("strategy", resolved.handle().strategy()),
                        A2uiDataEntry.string("message", resolved.message()),
                        A2uiDataEntry.bool("known", resolved.known())),
                new A2uiSurfaceUpdate(surfaceId, components),
                A2uiBeginRendering.standard(surfaceId, rootId));
    }

    public static List<A2uiServerMessage> runInspection(AgentRunInspection inspection) {
        return runInspection(inspection, WayangA2uiSurfaceOptions.readOnly());
    }

    public static List<A2uiServerMessage> runInspection(
            AgentRunInspection inspection,
            WayangA2uiSurfaceOptions options) {
        AgentRunInspection resolved = Objects.requireNonNull(inspection, "inspection");
        WayangA2uiSurfaceOptions resolvedOptions = resolveOptions(options);
        List<A2uiServerMessage> messages = new ArrayList<>(runStatus(resolved.status(), resolvedOptions));
        if (resolvedOptions.shows(WayangA2uiActions.RUN_EVENTS) && !resolved.events().empty()) {
            messages.addAll(runEvents(resolved.events(), resolvedOptions));
        }
        return List.copyOf(messages);
    }

    public static List<A2uiServerMessage> runEvents(AgentRunEvents events) {
        return runEvents(events, WayangA2uiSurfaceOptions.readOnly());
    }

    public static List<A2uiServerMessage> runEvents(AgentRunEvents events, WayangA2uiSurfaceOptions options) {
        AgentRunEvents resolved = Objects.requireNonNull(events, "events");
        WayangA2uiSurfaceOptions resolvedOptions = resolveOptions(options);
        String surfaceId = "wayang-run-events-" + safeId(resolved.runId());
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String summaryId = surfaceId + "-summary";
        String messageId = surfaceId + "-message";
        String refreshLabelId = surfaceId + "-refresh-label";
        String refreshButtonId = surfaceId + "-refresh";

        List<A2uiComponent> components = new ArrayList<>();
        List<String> rootChildren = new ArrayList<>();
        rootChildren.add(titleId);
        rootChildren.add(summaryId);
        rootChildren.add(messageId);

        components.add(A2uiComponents.text(titleId, "Run events " + resolved.runId()));
        components.add(A2uiComponents.text(summaryId, eventsSummary(resolved)));
        components.add(A2uiComponents.text(messageId, eventsMessageOrDefault(resolved)));

        for (AgentRunEvent event : resolved.events()) {
            String eventId = surfaceId + "-event-" + event.sequence();
            rootChildren.add(eventId);
            components.add(A2uiComponents.text(eventId, eventLine(event)));
        }

        if (resolvedOptions.shows(WayangA2uiActions.RUN_EVENTS)) {
            rootChildren.add(refreshButtonId);
            components.add(A2uiComponents.text(refreshLabelId, "Refresh events"));
            components.add(A2uiComponents.button(
                    refreshButtonId,
                    refreshLabelId,
                    action(
                            WayangA2uiActions.RUN_EVENTS,
                            resolvedOptions,
                            resolved.runId(),
                            surfaceId,
                            A2uiActionContextEntry.literalNumber("afterSequence", resolved.nextAfterSequence()),
                            A2uiActionContextEntry.literalNumber("limit", resolved.query().limit()))));
        }
        components.add(0, A2uiComponents.column(rootId, rootChildren));

        return List.of(
                A2uiDataModelUpdate.root(
                        surfaceId,
                        A2uiDataEntry.string("runId", resolved.runId()),
                        A2uiDataEntry.number("totalEvents", resolved.totalEvents()),
                        A2uiDataEntry.number("returnedEvents", resolved.returnedEvents()),
                        A2uiDataEntry.number("afterSequence", resolved.cursor().afterSequence()),
                        A2uiDataEntry.number("nextAfterSequence", resolved.nextAfterSequence()),
                        A2uiDataEntry.bool("truncated", resolved.truncated()),
                        A2uiDataEntry.bool("empty", resolved.empty()),
                        A2uiDataEntry.string("message", eventsMessageOrDefault(resolved)),
                        A2uiDataEntry.map("summary", List.of(
                                A2uiDataEntry.number("totalEvents", resolved.summary().totalEvents()),
                                A2uiDataEntry.number("returnedEvents", resolved.summary().returnedEvents()),
                                A2uiDataEntry.map("stateCounts", countEntries(resolved.stateCounts())),
                                A2uiDataEntry.map("typeCounts", countEntries(resolved.typeCounts())))),
                        A2uiDataEntry.map("events", eventEntries(resolved.events()))),
                new A2uiSurfaceUpdate(surfaceId, components),
                A2uiBeginRendering.standard(surfaceId, rootId));
    }

    public static List<A2uiServerMessage> runHistory(AgentRunHistory history) {
        return runHistory(history, WayangA2uiSurfaceOptions.readOnly());
    }

    public static List<A2uiServerMessage> runHistory(AgentRunHistory history, WayangA2uiSurfaceOptions options) {
        AgentRunHistory resolved = Objects.requireNonNull(history, "history");
        WayangA2uiSurfaceOptions resolvedOptions = resolveOptions(options);
        String surfaceId = "wayang-run-history";
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String summaryId = surfaceId + "-summary";
        String messageId = surfaceId + "-message";

        List<A2uiComponent> components = new ArrayList<>();
        List<String> rootChildren = new ArrayList<>();
        rootChildren.add(titleId);
        rootChildren.add(summaryId);
        rootChildren.add(messageId);

        components.add(A2uiComponents.text(titleId, "Wayang runs"));
        components.add(A2uiComponents.text(summaryId, historySummary(resolved)));
        components.add(A2uiComponents.text(messageId, messageOrDefault(resolved)));

        for (int index = 0; index < resolved.runs().size(); index++) {
            AgentRunStatus status = resolved.runs().get(index);
            String prefix = surfaceId + "-run-" + safeId(status.handle().runId());
            String rowId = prefix + "-row";
            String textId = prefix + "-text";
            List<String> rowChildren = new ArrayList<>();
            rootChildren.add(rowId);
            rowChildren.add(textId);
            components.add(A2uiComponents.text(textId, runLine(status)));
            if (resolvedOptions.shows(WayangA2uiActions.RUN_INSPECT)) {
                addActionButton(
                        components,
                        rowChildren,
                        prefix + "-inspect",
                        prefix + "-inspect-label",
                        "Inspect",
                        WayangA2uiActions.RUN_INSPECT,
                        resolvedOptions,
                        status.handle().runId(),
                        surfaceId);
            }
            if (resolvedOptions.shows(WayangA2uiActions.RUN_EVENTS)) {
                addActionButton(
                        components,
                        rowChildren,
                        prefix + "-events",
                        prefix + "-events-label",
                        "Events",
                        WayangA2uiActions.RUN_EVENTS,
                        resolvedOptions,
                        status.handle().runId(),
                        surfaceId);
            }
            if (!status.handle().terminal() && resolvedOptions.shows(WayangA2uiActions.RUN_WAIT)) {
                addActionButton(
                        components,
                        rowChildren,
                        prefix + "-wait",
                        prefix + "-wait-label",
                        "Wait",
                        WayangA2uiActions.RUN_WAIT,
                        resolvedOptions,
                        status.handle().runId(),
                        surfaceId);
            }
            if (!status.handle().terminal() && resolvedOptions.shows(WayangA2uiActions.RUN_CANCEL)) {
                addActionButton(
                        components,
                        rowChildren,
                        prefix + "-cancel",
                        prefix + "-cancel-label",
                        "Cancel",
                        WayangA2uiActions.RUN_CANCEL,
                        resolvedOptions,
                        status.handle().runId(),
                        surfaceId);
            }
            components.add(A2uiComponents.row(rowId, rowChildren));
        }
        components.add(0, A2uiComponents.column(rootId, rootChildren));

        return List.of(
                A2uiDataModelUpdate.root(
                        surfaceId,
                        A2uiDataEntry.number("totalRuns", resolved.totalRuns()),
                        A2uiDataEntry.number("returnedRuns", resolved.returnedRuns()),
                        A2uiDataEntry.number("offset", resolved.offset()),
                        A2uiDataEntry.bool("hasMore", resolved.hasMore()),
                        A2uiDataEntry.bool("empty", resolved.empty()),
                        A2uiDataEntry.string("message", messageOrDefault(resolved)),
                        A2uiDataEntry.map("summary", List.of(
                                A2uiDataEntry.number("totalRuns", resolved.summary().totalRuns()),
                                A2uiDataEntry.number("returnedRuns", resolved.summary().returnedRuns()),
                                A2uiDataEntry.map("stateCounts", countEntries(resolved.stateCounts())),
                                A2uiDataEntry.map("surfaceCounts", countEntries(resolved.surfaceCounts())),
                                A2uiDataEntry.map("strategyCounts", countEntries(resolved.strategyCounts())))),
                        A2uiDataEntry.map("runs", runEntries(resolved.runs()))),
                new A2uiSurfaceUpdate(surfaceId, components),
                A2uiBeginRendering.standard(surfaceId, rootId));
    }

    private static WayangA2uiSurfaceOptions resolveOptions(WayangA2uiSurfaceOptions options) {
        return options == null ? WayangA2uiSurfaceOptions.readOnly() : options;
    }

    private static void addActionButton(
            List<A2uiComponent> components,
            List<String> children,
            String buttonId,
            String labelId,
            String label,
            String actionName,
            WayangA2uiSurfaceOptions options,
            String runId,
            String surfaceId,
            A2uiActionContextEntry... entries) {
        children.add(buttonId);
        components.add(A2uiComponents.text(labelId, label));
        components.add(A2uiComponents.button(buttonId, labelId, action(actionName, options, runId, surfaceId, entries)));
    }

    private static A2uiAction action(
            String actionName,
            WayangA2uiSurfaceOptions options,
            String runId,
            String surfaceId,
            A2uiActionContextEntry... entries) {
        return new A2uiAction(actionName, resolveOptions(options).contextEntries(runId, surfaceId, entries));
    }

    private static String safeId(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            return "unknown";
        }
        String safe = normalized.replaceAll("[^a-z0-9_-]+", "-");
        return safe.isBlank() ? "unknown" : safe;
    }

    private static List<A2uiDataEntry> runEntries(List<AgentRunStatus> runs) {
        List<A2uiDataEntry> entries = new ArrayList<>();
        for (int index = 0; index < runs.size(); index++) {
            AgentRunStatus status = runs.get(index);
            entries.add(A2uiDataEntry.map("run" + index, List.of(
                    A2uiDataEntry.string("runId", status.handle().runId()),
                    A2uiDataEntry.string("state", status.handle().state().name()),
                    A2uiDataEntry.string("stateWireName", AgentRunStates.wireName(status.handle().state())),
                    A2uiDataEntry.string("strategy", status.handle().strategy()),
                    A2uiDataEntry.string("message", status.message()),
                    A2uiDataEntry.bool("known", status.known()),
                    A2uiDataEntry.bool("terminal", status.handle().terminal()))));
        }
        return entries;
    }

    private static List<A2uiDataEntry> eventEntries(List<AgentRunEvent> events) {
        List<A2uiDataEntry> entries = new ArrayList<>();
        for (AgentRunEvent event : events) {
            entries.add(A2uiDataEntry.map("event" + event.sequence(), List.of(
                    A2uiDataEntry.number("sequence", event.sequence()),
                    A2uiDataEntry.string("runId", event.runId()),
                    A2uiDataEntry.string("type", event.type()),
                    A2uiDataEntry.string("state", event.state().name()),
                    A2uiDataEntry.string("stateWireName", AgentRunStates.wireName(event.state())),
                    A2uiDataEntry.string("message", event.message()))));
        }
        return entries;
    }

    private static List<A2uiDataEntry> countEntries(java.util.Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
                .map(entry -> A2uiDataEntry.number(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static String historySummary(AgentRunHistory history) {
        return "Showing " + history.windowStart() + "-" + history.windowEnd()
                + " of " + history.totalRuns() + " runs";
    }

    private static String messageOrDefault(AgentRunHistory history) {
        if (history.message() == null || history.message().isBlank()) {
            return history.empty() ? "No run statuses are recorded." : "Run history loaded.";
        }
        return history.message();
    }

    private static String runLine(AgentRunStatus status) {
        return status.handle().runId()
                + " - "
                + status.handle().state().name()
                + " - "
                + status.message();
    }

    private static String eventsSummary(AgentRunEvents events) {
        return events.returnedEvents()
                + " of "
                + events.totalEvents()
                + " events, next sequence "
                + events.nextAfterSequence();
    }

    private static String eventsMessageOrDefault(AgentRunEvents events) {
        if (events.message() == null || events.message().isBlank()) {
            return events.empty() ? "No run events are recorded." : "Run events loaded.";
        }
        return events.message();
    }

    private static String eventLine(AgentRunEvent event) {
        return "#"
                + event.sequence()
                + " - "
                + event.type()
                + " - "
                + event.state().name()
                + " - "
                + event.message();
    }
}
