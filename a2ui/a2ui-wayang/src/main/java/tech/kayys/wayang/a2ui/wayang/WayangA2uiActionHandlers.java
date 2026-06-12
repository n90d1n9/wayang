package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.action.ActionQueries;
import tech.kayys.wayang.a2ui.wayang.action.ActionResponses;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of A2UI action handlers for Wayang lifecycle operations.
 */
public final class WayangA2uiActionHandlers {

    private final Map<String, WayangA2uiActionHandler> handlers;

    private WayangA2uiActionHandlers(Map<String, WayangA2uiActionHandler> handlers) {
        this.handlers = Collections.unmodifiableMap(new LinkedHashMap<>(handlers));
    }

    public static WayangA2uiActionHandlers standard(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        return standardBuilder(sdk, surfaceRegistry).build();
    }

    public static Builder standardBuilder(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        WayangGollekSdk resolvedSdk = Objects.requireNonNull(sdk, "sdk");
        WayangA2uiSurfaceRegistry resolvedRegistry = Objects.requireNonNull(surfaceRegistry, "surfaceRegistry");
        return builder()
                .register(WayangA2uiActions.RUN_INSPECT, (action, runId) -> inspect(
                        resolvedSdk,
                        resolvedRegistry,
                        runId))
                .register(WayangA2uiActions.RUN_HISTORY, (action, runId) -> history(
                        resolvedSdk,
                        resolvedRegistry,
                        action))
                .register(WayangA2uiActions.RUN_EVENTS, (action, runId) -> events(
                        resolvedSdk,
                        resolvedRegistry,
                        action,
                        runId))
                .register(WayangA2uiActions.RUN_WAIT, (action, runId) -> waitForRun(
                        resolvedSdk,
                        resolvedRegistry,
                        action,
                        runId))
                .register(WayangA2uiActions.RUN_CANCEL, (action, runId) -> cancel(
                        resolvedSdk,
                        resolvedRegistry,
                        action,
                        runId));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().extend(this);
    }

    public WayangA2uiActionResult route(A2uiUserAction action, String runId) {
        Objects.requireNonNull(action, "action");
        String resolvedRunId = runId == null ? "" : runId;
        String actionName = text(action.name());
        WayangA2uiActionHandler handler = actionName.isBlank() ? null : handlers.get(actionName);
        if (handler == null) {
            return WayangA2uiActionResult.rejected(action.name(), resolvedRunId, "Unsupported A2UI action.");
        }
        return handler.handle(action, resolvedRunId);
    }

    public boolean supports(String actionName) {
        String normalized = text(actionName);
        return !normalized.isBlank() && handlers.containsKey(normalized);
    }

    public List<String> actionNames() {
        return List.copyOf(handlers.keySet());
    }

    public WayangA2uiActionBindingReport bindingReport(WayangA2uiActionPolicy policy) {
        return WayangA2uiActionBindingReport.of(policy, this);
    }

    private static WayangA2uiActionResult inspect(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            String runId) {
        return ActionResponses.inspection(surfaceRegistry, runId, sdk.inspectRun(runId));
    }

    private static WayangA2uiActionResult history(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            A2uiUserAction action) {
        return ActionResponses.history(
                surfaceRegistry,
                sdk.runHistory(ActionQueries.history(action)));
    }

    private static WayangA2uiActionResult events(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            A2uiUserAction action,
            String runId) {
        return ActionResponses.events(
                surfaceRegistry,
                sdk.runEvents(runId, ActionQueries.events(action)));
    }

    private static WayangA2uiActionResult waitForRun(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            A2uiUserAction action,
            String runId) {
        return ActionResponses.waitResult(
                surfaceRegistry,
                sdk.waitForRun(runId, ActionQueries.waitOptions(action)));
    }

    private static WayangA2uiActionResult cancel(
            WayangGollekSdk sdk,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            A2uiUserAction action,
            String runId) {
        return ActionResponses.cancelResult(
                surfaceRegistry,
                sdk.cancelRun(runId, ActionQueries.cancelReason(action)));
    }

    private static String normalizeActionName(String actionName) {
        String normalized = text(actionName);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("A2UI action handler name must not be blank");
        }
        return normalized;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {

        private final Map<String, WayangA2uiActionHandler> handlers = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder extend(WayangA2uiActionHandlers registry) {
            if (registry != null) {
                handlers.putAll(registry.handlers);
            }
            return this;
        }

        public Builder register(String actionName, WayangA2uiActionHandler handler) {
            handlers.put(normalizeActionName(actionName), Objects.requireNonNull(handler, "handler"));
            return this;
        }

        public Builder replace(String actionName, WayangA2uiActionHandler handler) {
            return register(actionName, handler);
        }

        public Builder without(String actionName) {
            handlers.remove(normalizeActionName(actionName));
            return this;
        }

        public WayangA2uiActionHandlers build() {
            return new WayangA2uiActionHandlers(handlers);
        }
    }
}
