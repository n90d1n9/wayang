package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transport-specific event-stream rendering for task event replay.
 */
final class WayangA2aTaskEventStreams {

    private WayangA2aTaskEventStreams() {
    }

    static String http(List<WayangA2aTaskEvent> events) {
        return copy(events).stream()
                .map(WayangA2aTaskEvent::toMap)
                .map(WayangA2aHttpJson::write)
                .map(json -> "data: " + json + "\n\n")
                .collect(Collectors.joining());
    }

    static String jsonRpc(Object requestId, List<WayangA2aTaskEvent> events) {
        return copy(events).stream()
                .map(event -> WayangA2aJsonRpcResponse.result(requestId, event.payload()).toEvent())
                .collect(Collectors.joining());
    }

    private static List<WayangA2aTaskEvent> copy(List<WayangA2aTaskEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .filter(event -> event != null)
                .toList();
    }
}
