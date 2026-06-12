package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Removes a previously created surface from the client UI.
 */
public record A2uiDeleteSurface(String surfaceId) implements A2uiServerMessage {

    public A2uiDeleteSurface {
        surfaceId = A2uiValues.required(surfaceId, "surfaceId");
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("surfaceId", surfaceId);
        return A2uiValues.payload("deleteSurface", body);
    }
}
