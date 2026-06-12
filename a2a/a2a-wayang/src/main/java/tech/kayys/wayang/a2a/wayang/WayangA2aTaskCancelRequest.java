package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transport-neutral cancellation request for A2A task stores.
 */
record WayangA2aTaskCancelRequest(String taskId, A2aMessage message) {

    private static final String DEFAULT_HTTP_REASON = "Task canceled by A2A client.";

    WayangA2aTaskCancelRequest {
        taskId = WayangA2aMaps.required(taskId, "taskId");
    }

    static WayangA2aTaskCancelRequest fromHttp(String taskId, Map<?, ?> payload) {
        String reason = reason(payload).orElse(DEFAULT_HTTP_REASON);
        return new WayangA2aTaskCancelRequest(taskId, message(taskId, reason));
    }

    static WayangA2aTaskCancelRequest fromJsonRpc(String taskId, Map<?, ?> params) {
        return new WayangA2aTaskCancelRequest(taskId, reason(params)
                .map(value -> message(taskId, value))
                .orElse(null));
    }

    A2aTask apply(WayangA2aTaskStore store) {
        return store.cancel(taskId, message);
    }

    private static Optional<String> reason(Map<?, ?> payload) {
        return WayangA2aMaps.firstString(WayangA2aMaps.copyMap(payload), "reason", "message");
    }

    private static A2aMessage message(String taskId, String reason) {
        return new A2aMessage(
                taskId + "-cancel",
                taskId,
                taskId,
                A2aRole.ROLE_AGENT,
                List.of(A2aPart.text(reason)),
                Map.of("source", "a2a.cancel"),
                List.of(),
                List.of());
    }
}
