package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Streaming or push event that reports a task status transition.
 */
public record A2aTaskStatusUpdateEvent(
        String taskId,
        String contextId,
        A2aTaskStatus status,
        Map<String, Object> metadata) {

    public A2aTaskStatusUpdateEvent {
        taskId = A2aValues.required(taskId, "taskId");
        contextId = A2aValues.required(contextId, "contextId");
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        metadata = A2aValues.copyMap(metadata);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("contextId", contextId);
        payload.put("status", status.toMap());
        A2aValues.putOptional(payload, "metadata", metadata);
        return A2aValues.copyMap(payload);
    }

    public static A2aTaskStatusUpdateEvent fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aTaskStatusUpdateEvent(
                A2aValues.string(source, "taskId"),
                A2aValues.string(source, "contextId"),
                A2aTaskStatus.fromMap(A2aValues.objectRequired(source, "status")),
                A2aValues.objectOrEmpty(source, "metadata"));
    }
}
