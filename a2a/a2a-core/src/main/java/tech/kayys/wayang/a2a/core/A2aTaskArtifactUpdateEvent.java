package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Streaming or push event that reports a generated or updated task artifact.
 */
public record A2aTaskArtifactUpdateEvent(
        String taskId,
        String contextId,
        A2aArtifact artifact,
        Boolean append,
        Boolean lastChunk,
        Map<String, Object> metadata) {

    public A2aTaskArtifactUpdateEvent {
        taskId = A2aValues.required(taskId, "taskId");
        contextId = A2aValues.required(contextId, "contextId");
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        metadata = A2aValues.copyMap(metadata);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("contextId", contextId);
        payload.put("artifact", artifact.toMap());
        A2aValues.putOptional(payload, "append", append);
        A2aValues.putOptional(payload, "lastChunk", lastChunk);
        A2aValues.putOptional(payload, "metadata", metadata);
        return A2aValues.copyMap(payload);
    }

    public static A2aTaskArtifactUpdateEvent fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aTaskArtifactUpdateEvent(
                A2aValues.string(source, "taskId"),
                A2aValues.string(source, "contextId"),
                A2aArtifact.fromMap(A2aValues.objectRequired(source, "artifact")),
                A2aValues.optionalBoolean(source, "append"),
                A2aValues.optionalBoolean(source, "lastChunk"),
                A2aValues.objectOrEmpty(source, "metadata"));
    }
}
