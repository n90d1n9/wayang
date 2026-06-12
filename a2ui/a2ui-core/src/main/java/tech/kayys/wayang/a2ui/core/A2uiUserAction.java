package tech.kayys.wayang.a2ui.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client event emitted when a user activates a component action.
 */
public record A2uiUserAction(
        String name,
        String surfaceId,
        String sourceComponentId,
        Instant timestamp,
        Map<String, Object> context)
        implements A2uiClientMessage {

    public A2uiUserAction {
        name = A2uiValues.required(name, "name");
        surfaceId = A2uiValues.required(surfaceId, "surfaceId");
        sourceComponentId = A2uiValues.required(sourceComponentId, "sourceComponentId");
        context = A2uiValues.copyMap(context);
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("surfaceId", surfaceId);
        body.put("sourceComponentId", sourceComponentId);
        body.put("timestamp", A2uiValues.timestamp(timestamp));
        body.put("context", context);
        return A2uiValues.payload("userAction", body);
    }
}
