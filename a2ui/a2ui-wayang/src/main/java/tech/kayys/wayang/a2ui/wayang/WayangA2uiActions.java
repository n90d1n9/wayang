package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Set;

/**
 * Stable A2UI action names and action groups understood by the Wayang adapter.
 */
public final class WayangA2uiActions {

    public static final String RUN_INSPECT = "wayang.run.inspect";
    public static final String RUN_HISTORY = "wayang.run.history";
    public static final String RUN_EVENTS = "wayang.run.events";
    public static final String RUN_CANCEL = "wayang.run.cancel";
    public static final String RUN_WAIT = "wayang.run.wait";

    private static final List<String> RUN_LIFECYCLE_ACTION_ORDER = List.of(
            RUN_INSPECT,
            RUN_HISTORY,
            RUN_EVENTS,
            RUN_WAIT,
            RUN_CANCEL);

    private static final Set<String> INSPECT_ONLY_ACTION_NAMES = Set.of(RUN_INSPECT);

    private static final Set<String> READ_ONLY_ACTION_NAMES = Set.of(
            RUN_INSPECT,
            RUN_HISTORY,
            RUN_EVENTS);

    private static final Set<String> RUN_LIFECYCLE_ACTION_NAMES = Set.copyOf(RUN_LIFECYCLE_ACTION_ORDER);

    private static final Set<String> RUN_ID_REQUIRED_ACTION_NAMES = Set.of(
            RUN_INSPECT,
            RUN_EVENTS,
            RUN_WAIT,
            RUN_CANCEL);

    private WayangA2uiActions() {
    }

    public static Set<String> inspectOnlyActionNames() {
        return INSPECT_ONLY_ACTION_NAMES;
    }

    public static Set<String> readOnlyActionNames() {
        return READ_ONLY_ACTION_NAMES;
    }

    public static Set<String> runLifecycleActionNames() {
        return RUN_LIFECYCLE_ACTION_NAMES;
    }

    public static List<String> runLifecycleActionOrder() {
        return RUN_LIFECYCLE_ACTION_ORDER;
    }

    public static boolean requiresRunId(String actionName) {
        return actionName != null && RUN_ID_REQUIRED_ACTION_NAMES.contains(actionName);
    }
}
