package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small helpers for assembling root-column based A2UI surface layouts.
 */
public final class SurfaceLayouts {

    private SurfaceLayouts() {
    }

    public static List<A2uiComponent> components() {
        return new ArrayList<>();
    }

    public static List<String> children(String... childIds) {
        if (childIds == null || childIds.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(childIds));
    }

    public static void prependRootColumn(List<A2uiComponent> components, String rootId, List<String> children) {
        components.add(0, A2uiComponents.column(rootId, children));
    }

    public static void addRootColumn(List<A2uiComponent> components, String rootId, List<String> children) {
        components.add(A2uiComponents.column(rootId, children));
    }
}
