package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiClientMessage;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.util.Map;
import java.util.Objects;

/**
 * Routes inbound A2UI user actions into Wayang SDK lifecycle operations.
 */
public final class WayangA2uiActionRouter {

    private final WayangGollekSdk sdk;
    private final WayangA2uiActionPolicy policy;
    private final WayangA2uiSurfaceRegistry surfaceRegistry;

    public WayangA2uiActionRouter(WayangGollekSdk sdk) {
        this(sdk, WayangA2uiActionPolicy.inspectOnly());
    }

    public WayangA2uiActionRouter(WayangGollekSdk sdk, WayangA2uiActionPolicy policy) {
        this(sdk, policy, null);
    }

    public WayangA2uiActionRouter(
            WayangGollekSdk sdk,
            WayangA2uiActionPolicy policy,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
        this.policy = policy == null ? WayangA2uiActionPolicy.inspectOnly() : policy;
        this.surfaceRegistry = surfaceRegistry == null
                ? WayangA2uiSurfaceRegistry.fromPolicy(this.policy)
                : surfaceRegistry;
    }

    public WayangA2uiSurfaceRegistry surfaceRegistry() {
        return surfaceRegistry;
    }

    public WayangA2uiActionResult route(A2uiClientMessage message) {
        if (message instanceof A2uiUserAction action) {
            return route(action);
        }
        return WayangA2uiActionResult.rejected("", "", "Only A2UI userAction messages can be routed.");
    }

    public WayangA2uiActionResult route(A2uiUserAction action) {
        Objects.requireNonNull(action, "action");
        String runId = contextString(action, "runId");
        if (!policy.allowsAction(action.name())) {
            return WayangA2uiActionResult.rejected(action.name(), runId, "A2UI action is not allowed.");
        }
        if (requiresRunId(action.name()) && runId.isBlank()) {
            return WayangA2uiActionResult.rejected(action.name(), runId, "A2UI run action requires context.runId.");
        }
        if (!runId.isBlank() && !policy.allowsRun(runId)) {
            return WayangA2uiActionResult.rejected(action.name(), runId, "A2UI action is not allowed for this run.");
        }
        if (!policy.matchesContext(action.context())) {
            return WayangA2uiActionResult.rejected(action.name(), runId, "A2UI action context does not match policy.");
        }
        return switch (action.name()) {
            case WayangA2uiActions.RUN_INSPECT -> inspect(runId);
            case WayangA2uiActions.RUN_HISTORY -> history(action);
            case WayangA2uiActions.RUN_EVENTS -> events(action, runId);
            case WayangA2uiActions.RUN_WAIT -> waitForRun(action, runId);
            case WayangA2uiActions.RUN_CANCEL -> cancel(action, runId);
            default -> WayangA2uiActionResult.rejected(action.name(), runId, "Unsupported A2UI action.");
        };
    }

    private WayangA2uiActionResult inspect(String runId) {
        AgentRunInspection inspection = sdk.inspectRun(runId);
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_INSPECT,
                runId,
                inspection.message(),
                surfaceRegistry.renderRequired(inspection),
                WayangA2uiActionMetadata.inspection(inspection));
    }

    private WayangA2uiActionResult history(A2uiUserAction action) {
        AgentRunHistory history = sdk.runHistory(AgentRunHistoryQuery.of(
                contextString(action, "state"),
                contextInteger(action, "limit"),
                contextStringAny(action, "tenantId", "tenant"),
                contextStringAny(action, "sessionId", "session"),
                contextStringAny(action, "surfaceId", "surface"),
                contextInteger(action, "offset")));
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_HISTORY,
                "",
                history.message(),
                surfaceRegistry.renderRequired(history),
                WayangA2uiActionMetadata.history(history));
    }

    private WayangA2uiActionResult events(A2uiUserAction action, String runId) {
        AgentRunEvents events = sdk.runEvents(runId, AgentRunEventsQuery.of(
                contextString(action, "state"),
                contextString(action, "type"),
                contextLong(action, "afterSequence"),
                contextInteger(action, "limit")));
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_EVENTS,
                events.runId(),
                events.message(),
                surfaceRegistry.renderRequired(events),
                WayangA2uiActionMetadata.events(events));
    }

    private WayangA2uiActionResult waitForRun(A2uiUserAction action, String runId) {
        AgentRunWaitResult result = sdk.waitForRun(
                runId,
                AgentRunWaitOptions.of(contextInteger(action, "timeoutSeconds"), contextInteger(action, "pollMillis")));
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_WAIT,
                result.runId(),
                result.message(),
                surfaceRegistry.renderRequired(result.status()),
                WayangA2uiActionMetadata.waitResult(result));
    }

    private WayangA2uiActionResult cancel(A2uiUserAction action, String runId) {
        AgentRunCancelResult result = sdk.cancelRun(runId, contextString(action, "reason"));
        AgentRunStatus status = new AgentRunStatus(result.handle(), true, result.message(), result.metadata());
        return WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_CANCEL,
                result.runId(),
                result.message(),
                surfaceRegistry.renderRequired(status),
                WayangA2uiActionMetadata.cancelResult(result));
    }

    private static String contextString(A2uiUserAction action, String key) {
        Object value = action.context().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String contextStringAny(A2uiUserAction action, String... keys) {
        for (String key : keys) {
            String value = contextString(action, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Integer contextInteger(A2uiUserAction action, String key) {
        Object value = action.context().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return null;
    }

    private static Long contextLong(A2uiUserAction action, String key) {
        Object value = action.context().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    private static boolean requiresRunId(String actionName) {
        return WayangA2uiActions.RUN_INSPECT.equals(actionName)
                || WayangA2uiActions.RUN_EVENTS.equals(actionName)
                || WayangA2uiActions.RUN_WAIT.equals(actionName)
                || WayangA2uiActions.RUN_CANCEL.equals(actionName);
    }
}
