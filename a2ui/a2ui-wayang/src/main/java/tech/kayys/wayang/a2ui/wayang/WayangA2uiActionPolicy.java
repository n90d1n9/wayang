package tech.kayys.wayang.a2ui.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        allowedActions = allowedActions == null ? Set.of() : Set.copyOf(allowedActions);
        allowedRunIds = copyRunIds(allowedRunIds);
        requiredContext = copyContext(requiredContext);
    }

    public static WayangA2uiActionPolicy inspectOnly() {
        return new WayangA2uiActionPolicy(Set.of(WayangA2uiActions.RUN_INSPECT), Set.of(), Map.of());
    }

    public static WayangA2uiActionPolicy readOnly() {
        return new WayangA2uiActionPolicy(Set.of(
                WayangA2uiActions.RUN_INSPECT,
                WayangA2uiActions.RUN_HISTORY,
                WayangA2uiActions.RUN_EVENTS), Set.of(), Map.of());
    }

    public static WayangA2uiActionPolicy runLifecycle() {
        return new WayangA2uiActionPolicy(Set.of(
                WayangA2uiActions.RUN_INSPECT,
                WayangA2uiActions.RUN_HISTORY,
                WayangA2uiActions.RUN_EVENTS,
                WayangA2uiActions.RUN_WAIT,
                WayangA2uiActions.RUN_CANCEL), Set.of(), Map.of());
    }

    public Map<String, Object> toMap() {
        return WayangA2uiSessionProjection.actionPolicy(this);
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

    private static Set<String> copyRunIds(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(WayangA2uiActionPolicy::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<String, String> copyContext(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            if (!normalizedKey.isBlank() && value != null) {
                copy.put(normalizedKey, value);
            }
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(copy));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
