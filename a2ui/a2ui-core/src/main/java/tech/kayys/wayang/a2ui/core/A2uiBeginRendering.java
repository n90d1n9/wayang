package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Signals that a surface can render from the supplied root component.
 */
public record A2uiBeginRendering(
        String surfaceId,
        String root,
        String catalogId,
        Map<String, Object> styles)
        implements A2uiServerMessage {

    public A2uiBeginRendering {
        surfaceId = A2uiValues.optional(surfaceId);
        root = A2uiValues.required(root, "root");
        catalogId = A2uiValues.optional(catalogId);
        styles = A2uiValues.copyMap(styles);
    }

    public static A2uiBeginRendering standard(String surfaceId, String root) {
        return new A2uiBeginRendering(surfaceId, root, A2uiProtocol.STANDARD_CATALOG_ID, Map.of());
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        A2uiValues.putOptional(body, "surfaceId", surfaceId);
        A2uiValues.putOptional(body, "catalogId", catalogId);
        body.put("root", root);
        A2uiValues.putOptional(body, "styles", styles);
        return A2uiValues.payload("beginRendering", body);
    }
}
