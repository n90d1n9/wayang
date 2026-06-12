package tech.kayys.wayang.a2ui.core;

import java.util.Map;

/**
 * One A2UI server-to-client JSONL message.
 */
public sealed interface A2uiServerMessage permits
        A2uiSurfaceUpdate,
        A2uiDataModelUpdate,
        A2uiBeginRendering,
        A2uiDeleteSurface {

    String surfaceId();

    Map<String, Object> toPayload();
}
