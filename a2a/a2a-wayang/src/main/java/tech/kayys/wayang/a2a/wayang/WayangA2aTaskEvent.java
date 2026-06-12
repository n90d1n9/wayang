package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskArtifactUpdateEvent;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;
import tech.kayys.wayang.a2a.core.A2aTaskStatusUpdateEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Append-only task event used for stream replay and audit-friendly stores.
 */
public record WayangA2aTaskEvent(
        long sequence,
        String taskId,
        String contextId,
        String type,
        Map<String, Object> payload,
        String timestamp) {

    public static final String TYPE_TASK_CREATED = "task.created";
    public static final String TYPE_STATUS_UPDATED = "task.status.updated";
    public static final String TYPE_MESSAGE_APPENDED = "task.message.appended";
    public static final String TYPE_ARTIFACT_APPENDED = "task.artifact.appended";
    public static final String TYPE_TASK_CANCELED = "task.canceled";

    public WayangA2aTaskEvent {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        taskId = WayangA2aMaps.required(taskId, "taskId");
        contextId = WayangA2aMaps.required(contextId, "contextId");
        type = WayangA2aMaps.required(type, "type");
        payload = WayangA2aMaps.copyMap(payload);
        timestamp = timestamp == null || timestamp.isBlank() ? Instant.now().toString() : timestamp.trim();
    }

    public static WayangA2aTaskEvent taskCreated(long sequence, A2aTask task) {
        return new WayangA2aTaskEvent(
                sequence,
                task.id(),
                contextId(task),
                TYPE_TASK_CREATED,
                eventPayload("task", task.toMap()),
                Instant.now().toString());
    }

    public static WayangA2aTaskEvent status(
            long sequence,
            A2aTask task,
            A2aTaskStatusUpdateEvent event,
            boolean canceled) {
        return new WayangA2aTaskEvent(
                sequence,
                task.id(),
                contextId(task),
                canceled ? TYPE_TASK_CANCELED : TYPE_STATUS_UPDATED,
                eventPayload("statusUpdate", event.toMap()),
                Instant.now().toString());
    }

    public static WayangA2aTaskEvent status(
            long sequence,
            A2aTask task,
            A2aTaskStatus status,
            boolean canceled) {
        A2aTaskStatusUpdateEvent event = new A2aTaskStatusUpdateEvent(
                task.id(),
                contextId(task),
                status,
                Map.of());
        return status(sequence, task, event, canceled);
    }

    public static WayangA2aTaskEvent message(long sequence, A2aTask task, A2aMessage message) {
        return new WayangA2aTaskEvent(
                sequence,
                task.id(),
                contextId(task),
                TYPE_MESSAGE_APPENDED,
                eventPayload("message", message.toMap()),
                Instant.now().toString());
    }

    public static WayangA2aTaskEvent artifact(long sequence, A2aTask task, A2aArtifact artifact) {
        A2aTaskArtifactUpdateEvent event = new A2aTaskArtifactUpdateEvent(
                task.id(),
                contextId(task),
                artifact,
                true,
                true,
                Map.of());
        return new WayangA2aTaskEvent(
                sequence,
                task.id(),
                contextId(task),
                TYPE_ARTIFACT_APPENDED,
                eventPayload("artifactUpdate", event.toMap()),
                Instant.now().toString());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sequence", sequence);
        values.put("taskId", taskId);
        values.put("contextId", contextId);
        values.put("type", type);
        values.put("payload", payload);
        values.put("timestamp", timestamp);
        return WayangA2aMaps.copyMap(values);
    }

    static String contextId(A2aTask task) {
        String contextId = task == null ? null : WayangA2aMaps.optional(task.contextId());
        return contextId == null && task != null ? task.id() : contextId;
    }

    private static Map<String, Object> eventPayload(String key, Map<String, Object> value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(key, value);
        return WayangA2aMaps.copyMap(payload);
    }
}
