package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceRegistry;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;

import java.util.Objects;

/**
 * Builds handled A2UI action results from SDK lifecycle responses.
 */
public final class ActionResponses {

    private ActionResponses() {
    }

    public static WayangA2uiActionResult inspection(
            WayangA2uiSurfaceRegistry surfaceRegistry,
            String runId,
            AgentRunInspection inspection) {
        AgentRunInspection resolved = Objects.requireNonNull(inspection, "inspection");
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_INSPECT,
                runId,
                resolved.message(),
                registry(surfaceRegistry).renderRequired(resolved),
                ActionMetadata.inspection(resolved));
    }

    public static WayangA2uiActionResult history(
            WayangA2uiSurfaceRegistry surfaceRegistry,
            AgentRunHistory history) {
        AgentRunHistory resolved = Objects.requireNonNull(history, "history");
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_HISTORY,
                "",
                resolved.message(),
                registry(surfaceRegistry).renderRequired(resolved),
                ActionMetadata.history(resolved));
    }

    public static WayangA2uiActionResult events(
            WayangA2uiSurfaceRegistry surfaceRegistry,
            AgentRunEvents events) {
        AgentRunEvents resolved = Objects.requireNonNull(events, "events");
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_EVENTS,
                resolved.runId(),
                resolved.message(),
                registry(surfaceRegistry).renderRequired(resolved),
                ActionMetadata.events(resolved));
    }

    public static WayangA2uiActionResult waitResult(
            WayangA2uiSurfaceRegistry surfaceRegistry,
            AgentRunWaitResult result) {
        AgentRunWaitResult resolved = Objects.requireNonNull(result, "result");
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_WAIT,
                resolved.runId(),
                resolved.message(),
                registry(surfaceRegistry).renderRequired(resolved.status()),
                ActionMetadata.waitResult(resolved));
    }

    public static WayangA2uiActionResult cancelResult(
            WayangA2uiSurfaceRegistry surfaceRegistry,
            AgentRunCancelResult result) {
        AgentRunCancelResult resolved = Objects.requireNonNull(result, "result");
        AgentRunStatus status = new AgentRunStatus(
                resolved.handle(),
                true,
                resolved.message(),
                resolved.metadata());
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_CANCEL,
                resolved.runId(),
                resolved.message(),
                registry(surfaceRegistry).renderRequired(status),
                ActionMetadata.cancelResult(resolved));
    }

    private static WayangA2uiSurfaceRegistry registry(WayangA2uiSurfaceRegistry surfaceRegistry) {
        return Objects.requireNonNull(surfaceRegistry, "surfaceRegistry");
    }
}
