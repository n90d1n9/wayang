package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionPolicy;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;

import java.util.Objects;

/**
 * Applies A2UI action policy checks before SDK dispatch.
 */
public final class ActionGate {

    private ActionGate() {
    }

    public static Decision evaluate(WayangA2uiActionPolicy policy, A2uiUserAction action) {
        Objects.requireNonNull(action, "action");
        WayangA2uiActionPolicy resolved = policy == null ? WayangA2uiActionPolicy.defaultPolicy() : policy;
        String runId = ActionContextReader.text(action, "runId");
        if (!resolved.allowsAction(action.name())) {
            return Decision.rejected(action.name(), runId, "A2UI action is not allowed.");
        }
        if (WayangA2uiActions.requiresRunId(action.name()) && runId.isBlank()) {
            return Decision.rejected(action.name(), runId, "A2UI run action requires context.runId.");
        }
        if (!runId.isBlank() && !resolved.allowsRun(runId)) {
            return Decision.rejected(action.name(), runId, "A2UI action is not allowed for this run.");
        }
        if (!resolved.matchesContext(action.context())) {
            return Decision.rejected(action.name(), runId, "A2UI action context does not match policy.");
        }
        return Decision.accepted(runId);
    }

    public record Decision(
            String runId,
            WayangA2uiActionResult rejection) {

        static Decision accepted(String runId) {
            return new Decision(runId, null);
        }

        static Decision rejected(String actionName, String runId, String message) {
            return new Decision(runId, WayangA2uiActionResult.rejected(actionName, runId, message));
        }

        public boolean accepted() {
            return rejection == null;
        }
    }
}
