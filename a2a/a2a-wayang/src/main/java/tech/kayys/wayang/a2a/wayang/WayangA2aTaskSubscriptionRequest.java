package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.List;
import java.util.Map;

/**
 * Transport-neutral task event subscription request.
 */
record WayangA2aTaskSubscriptionRequest(
        String taskId,
        WayangA2aTaskEventCursor cursor,
        boolean terminalTasksAllowed) {

    WayangA2aTaskSubscriptionRequest {
        taskId = WayangA2aMaps.required(taskId, "taskId");
        cursor = cursor == null ? WayangA2aTaskEventCursor.of(0, WayangA2aTaskQuery.DEFAULT_LIMIT) : cursor;
    }

    static WayangA2aTaskSubscriptionRequest fromHttp(String taskId, Map<String, Object> attributes) {
        return new WayangA2aTaskSubscriptionRequest(
                taskId,
                WayangA2aTaskEventCursor.fromHttpAttributes(attributes),
                true);
    }

    static WayangA2aTaskSubscriptionRequest fromJsonRpc(String taskId, Map<String, Object> params) {
        return new WayangA2aTaskSubscriptionRequest(
                taskId,
                WayangA2aTaskEventCursor.fromJsonRpcParams(params),
                false);
    }

    boolean terminalUnsupported(A2aTask task) {
        return task != null && !terminalTasksAllowed && task.status().state().terminal();
    }

    List<WayangA2aTaskEvent> events(WayangA2aTaskStore store) {
        return store.events(taskId, cursor.afterSequence(), cursor.limit());
    }
}
