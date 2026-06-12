package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.support.StringMaps;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.Map;
import java.util.Set;

/**
 * Allowlist and context policy for inbound A2UI actions.
 */
public record WayangA2uiActionPolicy(
        Set<String> allowedActions,
        Set<String> allowedRunIds,
        Map<String, String> requiredContext) {

    public WayangA2uiActionPolicy {
        allowedActions = RecordCollections.copySet(allowedActions);
        allowedRunIds = RecordCollections.trimmedNonBlankStringSet(allowedRunIds);
        requiredContext = StringMaps.copyStringValues(requiredContext);
    }

    public static WayangA2uiActionPolicy inspectOnly() {
        return new WayangA2uiActionPolicy(WayangA2uiActions.inspectOnlyActionNames(), Set.of(), Map.of());
    }

    public static WayangA2uiActionPolicy defaultPolicy() {
        return inspectOnly();
    }

    public static WayangA2uiActionPolicy readOnly() {
        return new WayangA2uiActionPolicy(WayangA2uiActions.readOnlyActionNames(), Set.of(), Map.of());
    }

    public static WayangA2uiActionPolicy runLifecycle() {
        return new WayangA2uiActionPolicy(WayangA2uiActions.runLifecycleActionNames(), Set.of(), Map.of());
    }

    public Map<String, Object> toMap() {
        return SessionProjection.actionPolicy(this);
    }

    public boolean allowsAction(String actionName) {
        return allowedActions.contains(actionName);
    }

    public boolean allowsRun(String runId) {
        return allowedRunIds.isEmpty() || allowedRunIds.contains(normalize(runId));
    }

    public boolean matchesContext(Map<String, Object> context) {
        if (requiredContext.isEmpty()) {
            return true;
        }
        Map<String, Object> resolved = context == null ? Map.of() : context;
        return requiredContext.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(String.valueOf(resolved.get(entry.getKey()))));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
