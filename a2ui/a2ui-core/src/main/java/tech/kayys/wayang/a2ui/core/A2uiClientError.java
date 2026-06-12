package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side A2UI rendering or data-binding error.
 */
public record A2uiClientError(String surfaceId, String message, Map<String, Object> details)
        implements A2uiClientMessage {

    public A2uiClientError {
        surfaceId = A2uiValues.optional(surfaceId);
        message = A2uiValues.required(message, "message");
        details = A2uiValues.copyMap(details);
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        A2uiValues.putOptional(body, "surfaceId", surfaceId);
        body.put("message", message);
        A2uiValues.putOptional(body, "details", details);
        return A2uiValues.payload("error", body);
    }
}
