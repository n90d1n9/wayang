package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A message exchanged between a client/user and an agent.
 */
public record A2aMessage(
        String messageId,
        String contextId,
        String taskId,
        A2aRole role,
        List<A2aPart> parts,
        Map<String, Object> metadata,
        List<String> extensions,
        List<String> referenceTaskIds) {

    public A2aMessage {
        messageId = A2aValues.required(messageId, "messageId");
        contextId = A2aValues.optional(contextId);
        taskId = A2aValues.optional(taskId);
        role = role == null ? A2aRole.ROLE_UNSPECIFIED : role;
        parts = A2aValues.copyRecords(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("A2A message requires at least one part");
        }
        metadata = A2aValues.copyMap(metadata);
        extensions = A2aValues.stringList(extensions, "extensions");
        referenceTaskIds = A2aValues.stringList(referenceTaskIds, "referenceTaskIds");
    }

    public static A2aMessage user(String messageId, List<A2aPart> parts) {
        return new A2aMessage(messageId, null, null, A2aRole.ROLE_USER, parts, Map.of(), List.of(), List.of());
    }

    public static A2aMessage agent(String messageId, List<A2aPart> parts) {
        return new A2aMessage(messageId, null, null, A2aRole.ROLE_AGENT, parts, Map.of(), List.of(), List.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", messageId);
        A2aValues.putOptional(payload, "contextId", contextId);
        A2aValues.putOptional(payload, "taskId", taskId);
        payload.put("role", role.value());
        payload.put("parts", parts.stream().map(A2aPart::toMap).toList());
        A2aValues.putOptional(payload, "metadata", metadata);
        A2aValues.putOptional(payload, "extensions", extensions);
        A2aValues.putOptional(payload, "referenceTaskIds", referenceTaskIds);
        return A2aValues.copyMap(payload);
    }

    public String toJson() {
        return A2aJson.write(toMap(), "message");
    }

    public static A2aMessage fromJson(String json) {
        return fromMap(A2aJson.read(json, "message"));
    }

    public static A2aMessage fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aMessage(
                A2aValues.string(source, "messageId"),
                A2aValues.optionalString(source, "contextId"),
                A2aValues.optionalString(source, "taskId"),
                A2aRole.fromValue(A2aValues.optionalString(source, "role")),
                A2aValues.objectList(source.get("parts"), "parts").stream()
                        .map(A2aPart::fromMap)
                        .toList(),
                A2aValues.objectOrEmpty(source, "metadata"),
                A2aValues.stringList(source.get("extensions"), "extensions"),
                A2aValues.stringList(source.get("referenceTaskIds"), "referenceTaskIds"));
    }
}
