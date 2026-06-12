package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Objects;

/**
 * Adds reusable inspect, events, wait, and cancel controls to Wayang run
 * surfaces.
 */
public final class RunActionControls {

    private static final Labels STATUS_LABELS = new Labels(
            "Inspect run",
            "View events",
            "Wait for completion",
            "Cancel run");
    private static final Labels HISTORY_ROW_LABELS = new Labels(
            "Inspect",
            "Events",
            "Wait",
            "Cancel");

    private RunActionControls() {
    }

    public static void addStatusActions(
            List<A2uiComponent> components,
            List<String> children,
            String prefix,
            AgentRunStatus status,
            String surfaceId,
            WayangA2uiSurfaceOptions options) {
        AgentRunStatus resolved = Objects.requireNonNull(status, "status");
        addRunActions(
                components,
                children,
                prefix,
                resolved.handle().runId(),
                surfaceId,
                resolved.handle().terminal(),
                SurfaceActions.options(options),
                STATUS_LABELS);
    }

    public static void addHistoryRowActions(
            List<A2uiComponent> components,
            List<String> children,
            String prefix,
            AgentRunStatus status,
            String surfaceId,
            WayangA2uiSurfaceOptions options) {
        AgentRunStatus resolved = Objects.requireNonNull(status, "status");
        addRunActions(
                components,
                children,
                prefix,
                resolved.handle().runId(),
                surfaceId,
                resolved.handle().terminal(),
                SurfaceActions.options(options),
                HISTORY_ROW_LABELS);
    }

    private static void addRunActions(
            List<A2uiComponent> components,
            List<String> children,
            String prefix,
            String runId,
            String surfaceId,
            boolean terminal,
            WayangA2uiSurfaceOptions options,
            Labels labels) {
        if (options.shows(WayangA2uiActions.RUN_INSPECT)) {
            addButton(components, children, prefix, "inspect", labels.inspect(), WayangA2uiActions.RUN_INSPECT,
                    options, runId, surfaceId);
        }
        if (options.shows(WayangA2uiActions.RUN_EVENTS)) {
            addButton(components, children, prefix, "events", labels.events(), WayangA2uiActions.RUN_EVENTS,
                    options, runId, surfaceId);
        }
        if (!terminal && options.shows(WayangA2uiActions.RUN_WAIT)) {
            addButton(components, children, prefix, "wait", labels.waitLabel(), WayangA2uiActions.RUN_WAIT,
                    options, runId, surfaceId);
        }
        if (!terminal && options.shows(WayangA2uiActions.RUN_CANCEL)) {
            addButton(components, children, prefix, "cancel", labels.cancelLabel(), WayangA2uiActions.RUN_CANCEL,
                    options, runId, surfaceId);
        }
    }

    private static void addButton(
            List<A2uiComponent> components,
            List<String> children,
            String prefix,
            String suffix,
            String label,
            String actionName,
            WayangA2uiSurfaceOptions options,
            String runId,
            String surfaceId) {
        SurfaceActions.addButton(
                components,
                children,
                prefix + "-" + suffix,
                prefix + "-" + suffix + "-label",
                label,
                actionName,
                options,
                runId,
                surfaceId);
    }

    private record Labels(
            String inspect,
            String events,
            String waitLabel,
            String cancelLabel) {
    }
}
