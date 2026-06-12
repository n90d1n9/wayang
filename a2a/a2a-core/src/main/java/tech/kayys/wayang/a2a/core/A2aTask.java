package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A task state, results, history, and metadata.
 */
public record A2aTask(
        String id,
        String contextId,
        A2aTaskStatus status,
        List<A2aArtifact> artifacts,
        List<A2aMessage> history,
        Map<String, Object> metadata) {

    public A2aTask {
        id = A2aValues.required(id, "id");
        contextId = A2aValues.optional(contextId);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        artifacts = A2aValues.copyRecords(artifacts);
        history = A2aValues.copyRecords(history);
        metadata = A2aValues.copyMap(metadata);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        A2aValues.putOptional(payload, "contextId", contextId);
        payload.put("status", status.toMap());
        if (!artifacts.isEmpty()) {
            payload.put("artifacts", artifacts.stream().map(A2aArtifact::toMap).toList());
        }
        if (!history.isEmpty()) {
            payload.put("history", history.stream().map(A2aMessage::toMap).toList());
        }
        A2aValues.putOptional(payload, "metadata", metadata);
        return A2aValues.copyMap(payload);
    }

    public String toJson() {
        return A2aJson.write(toMap(), "task");
    }

    public static A2aTask fromJson(String json) {
        return fromMap(A2aJson.read(json, "task"));
    }

    public static A2aTask fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aTask(
                A2aValues.string(source, "id"),
                A2aValues.optionalString(source, "contextId"),
                A2aTaskStatus.fromMap(A2aValues.objectRequired(source, "status")),
                A2aValues.objectList(source.get("artifacts"), "artifacts").stream()
                        .map(A2aArtifact::fromMap)
                        .toList(),
                A2aValues.objectList(source.get("history"), "history").stream()
                        .map(A2aMessage::fromMap)
                        .toList(),
                A2aValues.objectOrEmpty(source, "metadata"));
    }
}
