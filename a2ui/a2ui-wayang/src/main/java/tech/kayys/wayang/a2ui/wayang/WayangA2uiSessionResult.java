package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encoded A2UI responses and routing metadata for one inbound session turn.
 */
public record WayangA2uiSessionResult(
        List<WayangA2uiActionResult> actionResults,
        List<A2uiServerMessage> responseMessages,
        String responseJsonl,
        List<Map<String, Object>> responseDataParts) {

    public WayangA2uiSessionResult {
        actionResults = actionResults == null ? List.of() : List.copyOf(actionResults);
        responseMessages = responseMessages == null ? List.of() : List.copyOf(responseMessages);
        responseJsonl = responseJsonl == null ? "" : responseJsonl;
        responseDataParts = responseDataParts == null
                ? List.of()
                : responseDataParts.stream()
                        .filter(Objects::nonNull)
                        .map(Map::copyOf)
                        .toList();
    }

    public static WayangA2uiSessionResult of(
            List<WayangA2uiActionResult> actionResults,
            A2uiJsonlCodec codec) {
        return of(actionResults, codec, null);
    }

    public static WayangA2uiSessionResult of(
            List<WayangA2uiActionResult> actionResults,
            A2uiJsonlCodec codec,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        A2uiJsonlCodec resolvedCodec = codec == null ? new A2uiJsonlCodec() : codec;
        List<WayangA2uiActionResult> results = actionResults == null ? List.of() : List.copyOf(actionResults);
        List<A2uiServerMessage> messages = messages(results, surfaceRegistry);
        List<Map<String, Object>> dataParts = messages.stream()
                .map(WayangA2ui::dataPart)
                .toList();
        return new WayangA2uiSessionResult(
                results,
                messages,
                resolvedCodec.stream(messages),
                dataParts);
    }

    private static List<A2uiServerMessage> messages(
            List<WayangA2uiActionResult> results,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        if (results.isEmpty()) {
            return List.of();
        }
        WayangA2uiSurfaceRegistry resolvedRegistry = surfaceRegistry == null
                ? WayangA2uiSurfaceRegistry.readOnly()
                : surfaceRegistry;
        List<A2uiServerMessage> messages = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            WayangA2uiActionResult result = results.get(index);
            if (result.responseMessages().isEmpty()) {
                messages.addAll(resolvedRegistry.renderActionFeedback(result, index + 1));
            } else {
                messages.addAll(result.responseMessages());
            }
        }
        return List.copyOf(messages);
    }

    public long handledCount() {
        return actionResults.stream()
                .filter(WayangA2uiActionResult::handled)
                .count();
    }

    public long rejectedCount() {
        return actionResults.size() - handledCount();
    }
}
