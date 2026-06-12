package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;

/**
 * Maps inbound A2UI action context into SDK query and option objects.
 */
public final class ActionQueries {

    private ActionQueries() {
    }

    public static AgentRunHistoryQuery history(A2uiUserAction action) {
        return AgentRunHistoryQuery.of(
                ActionContextReader.text(action, "state"),
                ActionContextReader.integer(action, "limit"),
                ActionContextReader.firstText(action, "tenantId", "tenant"),
                ActionContextReader.firstText(action, "sessionId", "session"),
                ActionContextReader.firstText(action, "surfaceId", "surface"),
                ActionContextReader.integer(action, "offset"));
    }

    public static AgentRunEventsQuery events(A2uiUserAction action) {
        return AgentRunEventsQuery.of(
                ActionContextReader.text(action, "state"),
                ActionContextReader.text(action, "type"),
                ActionContextReader.longValue(action, "afterSequence"),
                ActionContextReader.integer(action, "limit"));
    }

    public static AgentRunWaitOptions waitOptions(A2uiUserAction action) {
        return AgentRunWaitOptions.of(
                ActionContextReader.integer(action, "timeoutSeconds"),
                ActionContextReader.integer(action, "pollMillis"));
    }

    public static String cancelReason(A2uiUserAction action) {
        return ActionContextReader.text(action, "reason");
    }
}
