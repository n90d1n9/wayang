package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiClientMessage;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.action.ActionGate;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.util.Objects;

/**
 * Routes inbound A2UI user actions into Wayang SDK lifecycle operations.
 */
public final class WayangA2uiActionRouter {

    private final WayangA2uiActionPolicy policy;
    private final WayangA2uiSurfaceRegistry surfaceRegistry;
    private final WayangA2uiActionHandlers actionHandlers;

    public WayangA2uiActionRouter(WayangGollekSdk sdk) {
        this(sdk, WayangA2uiActionPolicy.defaultPolicy());
    }

    public WayangA2uiActionRouter(WayangGollekSdk sdk, WayangA2uiActionPolicy policy) {
        this(sdk, policy, null);
    }

    public WayangA2uiActionRouter(
            WayangGollekSdk sdk,
            WayangA2uiActionPolicy policy,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        WayangGollekSdk resolvedSdk = Objects.requireNonNull(sdk, "sdk");
        this.policy = policy == null ? WayangA2uiActionPolicy.defaultPolicy() : policy;
        this.surfaceRegistry = surfaceRegistry == null
                ? WayangA2uiSurfaceRegistry.fromPolicy(this.policy)
                : surfaceRegistry;
        this.actionHandlers = WayangA2uiActionHandlers.standard(resolvedSdk, this.surfaceRegistry);
    }

    public WayangA2uiActionRouter(
            WayangA2uiActionPolicy policy,
            WayangA2uiSurfaceRegistry surfaceRegistry,
            WayangA2uiActionHandlers actionHandlers) {
        this.policy = policy == null ? WayangA2uiActionPolicy.defaultPolicy() : policy;
        this.surfaceRegistry = surfaceRegistry == null
                ? WayangA2uiSurfaceRegistry.fromPolicy(this.policy)
                : surfaceRegistry;
        this.actionHandlers = Objects.requireNonNull(actionHandlers, "actionHandlers");
    }

    public WayangA2uiSurfaceRegistry surfaceRegistry() {
        return surfaceRegistry;
    }

    public WayangA2uiActionBindingReport actionBindingReport() {
        return actionHandlers.bindingReport(policy);
    }

    public WayangA2uiActionResult route(A2uiClientMessage message) {
        if (message instanceof A2uiUserAction action) {
            return route(action);
        }
        return WayangA2uiActionResult.rejected("", "", "Only A2UI userAction messages can be routed.");
    }

    public WayangA2uiActionResult route(A2uiUserAction action) {
        Objects.requireNonNull(action, "action");
        ActionGate.Decision decision = ActionGate.evaluate(policy, action);
        if (!decision.accepted()) {
            return decision.rejection();
        }
        return actionHandlers.route(action, decision.runId());
    }
}
