package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordMaps;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

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
        actionName = RecordValues.text(actionName);
        runId = RecordValues.text(runId);
        message = RecordValues.text(message);
        responseMessages = RecordCollections.copyList(responseMessages);
        metadata = RecordMaps.nullableValues(metadata);
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
}
