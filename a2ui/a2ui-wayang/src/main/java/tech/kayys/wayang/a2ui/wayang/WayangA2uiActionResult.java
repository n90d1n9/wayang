package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of routing one inbound A2UI action into Wayang.
 */
public record WayangA2uiActionResult(
        String actionName,
        String runId,
        boolean handled,
        String message,
        List<A2uiServerMessage> responseMessages,
        Map<String, Object> metadata) {

    public WayangA2uiActionResult {
        actionName = normalize(actionName);
        runId = normalize(runId);
        message = normalize(message);
        responseMessages = responseMessages == null ? List.of() : List.copyOf(responseMessages);
        metadata = copy(metadata);
    }

    public static WayangA2uiActionResult handled(
            String actionName,
            String runId,
            String message,
            List<A2uiServerMessage> responseMessages,
            Map<String, Object> metadata) {
        return new WayangA2uiActionResult(actionName, runId, true, message, responseMessages, metadata);
    }

    public static WayangA2uiActionResult rejected(String actionName, String runId, String message) {
        return new WayangA2uiActionResult(actionName, runId, false, message, List.of(), Map.of());
    }

    private static Map<String, Object> copy(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                copy.put(key, value);
            }
        });
        return WayangA2uiTransportMaps.freeze(copy);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
