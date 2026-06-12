package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiUserAction;

/**
 * Handles one policy-admitted A2UI user action.
 */
@FunctionalInterface
public interface WayangA2uiActionHandler {

    WayangA2uiActionResult handle(A2uiUserAction action, String runId);
}
