package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Streaming response envelope for task, message, status update, or artifact update events.
 */
public record A2aStreamResponse(
        A2aTask task,
        A2aMessage message,
        A2aTaskStatusUpdateEvent statusUpdate,
        A2aTaskArtifactUpdateEvent artifactUpdate) {

    public A2aStreamResponse {
        int present = (task == null ? 0 : 1)
                + (message == null ? 0 : 1)
                + (statusUpdate == null ? 0 : 1)
                + (artifactUpdate == null ? 0 : 1);
        if (present != 1) {
            throw new IllegalArgumentException("A2A stream response requires exactly one event");
        }
    }

    public static A2aStreamResponse task(A2aTask task) {
        return new A2aStreamResponse(task, null, null, null);
    }

    public static A2aStreamResponse message(A2aMessage message) {
        return new A2aStreamResponse(null, message, null, null);
    }

    public static A2aStreamResponse statusUpdate(A2aTaskStatusUpdateEvent event) {
        return new A2aStreamResponse(null, null, event, null);
    }

    public static A2aStreamResponse artifactUpdate(A2aTaskArtifactUpdateEvent event) {
        return new A2aStreamResponse(null, null, null, event);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (task != null) {
            payload.put("task", task.toMap());
        }
        if (message != null) {
            payload.put("message", message.toMap());
        }
        if (statusUpdate != null) {
            payload.put("statusUpdate", statusUpdate.toMap());
        }
        if (artifactUpdate != null) {
            payload.put("artifactUpdate", artifactUpdate.toMap());
        }
        return A2aValues.copyMap(payload);
    }

    public String toJson() {
        return A2aJson.write(toMap(), "stream response");
    }

    public static A2aStreamResponse fromJson(String json) {
        return fromMap(A2aJson.read(json, "stream response"));
    }

    public static A2aStreamResponse fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        A2aTask task = source.get("task") instanceof Map<?, ?> taskMap ? A2aTask.fromMap(taskMap) : null;
        A2aMessage message = source.get("message") instanceof Map<?, ?> messageMap
                ? A2aMessage.fromMap(messageMap)
                : null;
        A2aTaskStatusUpdateEvent statusUpdate = source.get("statusUpdate") instanceof Map<?, ?> statusMap
                ? A2aTaskStatusUpdateEvent.fromMap(statusMap)
                : null;
        A2aTaskArtifactUpdateEvent artifactUpdate = source.get("artifactUpdate") instanceof Map<?, ?> artifactMap
                ? A2aTaskArtifactUpdateEvent.fromMap(artifactMap)
                : null;
        return new A2aStreamResponse(task, message, statusUpdate, artifactUpdate);
    }
}
