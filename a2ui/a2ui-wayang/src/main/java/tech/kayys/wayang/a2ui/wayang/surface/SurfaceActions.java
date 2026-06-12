package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiAction;
import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;

import java.util.List;

/**
 * Builds A2UI surface action controls with consistent option fallback,
 * button assembly, and run/surface action context wiring.
 */
public final class SurfaceActions {

    private SurfaceActions() {
    }

    public static WayangA2uiSurfaceOptions options(WayangA2uiSurfaceOptions options) {
        return options == null ? WayangA2uiSurfaceOptions.readOnly() : options;
    }

    public static void addButton(
            List<A2uiComponent> components,
            List<String> children,
            String buttonId,
            String labelId,
            String label,
            String actionName,
            WayangA2uiSurfaceOptions options,
            String runId,
            String surfaceId,
            A2uiActionContextEntry... entries) {
        children.add(buttonId);
        components.add(A2uiComponents.text(labelId, label));
        components.add(A2uiComponents.button(buttonId, labelId, action(actionName, options, runId, surfaceId, entries)));
    }

    public static A2uiAction action(
            String actionName,
            WayangA2uiSurfaceOptions options,
            String runId,
            String surfaceId,
            A2uiActionContextEntry... entries) {
        return new A2uiAction(actionName, options(options).contextEntries(runId, surfaceId, entries));
    }
}
