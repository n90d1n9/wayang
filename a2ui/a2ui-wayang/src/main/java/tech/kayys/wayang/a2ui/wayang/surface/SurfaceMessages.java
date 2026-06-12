package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiBeginRendering;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.core.A2uiDataModelUpdate;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.core.A2uiSurfaceUpdate;

import java.util.List;

/**
 * Shared assembly for standard Wayang A2UI surface message streams.
 */
public final class SurfaceMessages {

    private SurfaceMessages() {
    }

    public static List<A2uiServerMessage> standard(
            String surfaceId,
            String rootId,
            List<A2uiComponent> components,
            A2uiDataEntry... contents) {
        return List.of(
                A2uiDataModelUpdate.root(surfaceId, contents),
                new A2uiSurfaceUpdate(surfaceId, components),
                A2uiBeginRendering.standard(surfaceId, rootId));
    }
}
