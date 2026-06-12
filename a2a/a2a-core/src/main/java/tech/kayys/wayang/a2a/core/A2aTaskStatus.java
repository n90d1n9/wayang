package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Current status state and optional status message for an A2A task.
 */
public record A2aTaskStatus(A2aTaskState state, A2aMessage message, String timestamp) {

    public A2aTaskStatus {
        state = state == null ? A2aTaskState.TASK_STATE_UNSPECIFIED : state;
        timestamp = A2aValues.optional(timestamp);
    }

    public static A2aTaskStatus of(A2aTaskState state) {
        return new A2aTaskStatus(state, null, null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("state", state.value());
        if (message != null) {
            payload.put("message", message.toMap());
        }
        A2aValues.putOptional(payload, "timestamp", timestamp);
        return A2aValues.copyMap(payload);
    }

    public static A2aTaskStatus fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        A2aMessage statusMessage = source.get("message") instanceof Map<?, ?> messageMap
                ? A2aMessage.fromMap(messageMap)
                : null;
        return new A2aTaskStatus(
                A2aTaskState.fromValue(A2aValues.optionalString(source, "state")),
                statusMessage,
                A2aValues.optionalString(source, "timestamp"));
    }
}
