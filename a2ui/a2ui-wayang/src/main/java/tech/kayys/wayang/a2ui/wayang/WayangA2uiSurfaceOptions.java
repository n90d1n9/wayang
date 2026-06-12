package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.support.StringMaps;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;

import java.util.ArrayList;
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
        visibleActions = RecordCollections.copySet(visibleActions);
        actionContext = StringMaps.copyStringValues(actionContext);
    }

    public static WayangA2uiSurfaceOptions inspectOnly() {
        return new WayangA2uiSurfaceOptions(WayangA2uiActions.inspectOnlyActionNames(), Map.of());
    }

    public static WayangA2uiSurfaceOptions readOnly() {
        return new WayangA2uiSurfaceOptions(WayangA2uiActions.readOnlyActionNames(), Map.of());
    }

    public static WayangA2uiSurfaceOptions runLifecycle() {
        return new WayangA2uiSurfaceOptions(WayangA2uiActions.runLifecycleActionNames(), Map.of());
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

}
