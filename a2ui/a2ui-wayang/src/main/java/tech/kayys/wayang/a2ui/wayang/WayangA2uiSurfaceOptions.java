package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Presentation policy for generated Wayang A2UI surfaces.
 */
public record WayangA2uiSurfaceOptions(
        Set<String> visibleActions,
        Map<String, String> actionContext) {

    public WayangA2uiSurfaceOptions {
        visibleActions = visibleActions == null ? Set.of() : Set.copyOf(visibleActions);
        actionContext = copyContext(actionContext);
    }

    public static WayangA2uiSurfaceOptions inspectOnly() {
        return new WayangA2uiSurfaceOptions(Set.of(WayangA2uiActions.RUN_INSPECT), Map.of());
    }

    public static WayangA2uiSurfaceOptions readOnly() {
        return new WayangA2uiSurfaceOptions(Set.of(
                WayangA2uiActions.RUN_INSPECT,
                WayangA2uiActions.RUN_HISTORY,
                WayangA2uiActions.RUN_EVENTS), Map.of());
    }

    public static WayangA2uiSurfaceOptions runLifecycle() {
        return new WayangA2uiSurfaceOptions(Set.of(
                WayangA2uiActions.RUN_INSPECT,
                WayangA2uiActions.RUN_HISTORY,
                WayangA2uiActions.RUN_EVENTS,
                WayangA2uiActions.RUN_WAIT,
                WayangA2uiActions.RUN_CANCEL), Map.of());
    }

    public static WayangA2uiSurfaceOptions fromPolicy(WayangA2uiActionPolicy policy) {
        if (policy == null) {
            return inspectOnly();
        }
        return new WayangA2uiSurfaceOptions(policy.allowedActions(), policy.requiredContext());
    }

    public boolean shows(String actionName) {
        return visibleActions.contains(actionName);
    }

    public List<A2uiActionContextEntry> contextEntries(
            String runId,
            String surfaceId,
            A2uiActionContextEntry... entries) {
        List<A2uiActionContextEntry> context = new ArrayList<>();
        if (runId != null && !runId.isBlank()) {
            context.add(A2uiActionContextEntry.literalString("runId", runId));
        }
        if (surfaceId != null && !surfaceId.isBlank()) {
            context.add(A2uiActionContextEntry.literalString("surfaceId", surfaceId));
        }
        actionContext.forEach((key, value) -> {
            if (!"runId".equals(key) && !"surfaceId".equals(key)) {
                context.add(A2uiActionContextEntry.literalString(key, value));
            }
        });
        if (entries != null) {
            context.addAll(List.of(entries));
        }
        return List.copyOf(context);
    }

    private static Map<String, String> copyContext(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null && !key.isBlank()) {
                copy.put(key.trim(), value);
            }
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(copy));
    }
}
