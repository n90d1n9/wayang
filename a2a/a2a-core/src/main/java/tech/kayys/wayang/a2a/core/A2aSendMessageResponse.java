package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Non-streaming send response. Exactly one of task or message is present.
 */
public record A2aSendMessageResponse(A2aTask task, A2aMessage message) {

    public A2aSendMessageResponse {
        int present = (task == null ? 0 : 1) + (message == null ? 0 : 1);
        if (present != 1) {
            throw new IllegalArgumentException("A2A send message response requires exactly one result");
        }
    }

    public static A2aSendMessageResponse task(A2aTask task) {
        return new A2aSendMessageResponse(task, null);
    }

    public static A2aSendMessageResponse message(A2aMessage message) {
        return new A2aSendMessageResponse(null, message);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (task != null) {
            payload.put("task", task.toMap());
        }
        if (message != null) {
            payload.put("message", message.toMap());
        }
        return A2aValues.copyMap(payload);
    }

    public static A2aSendMessageResponse fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        A2aTask task = source.get("task") instanceof Map<?, ?> taskMap ? A2aTask.fromMap(taskMap) : null;
        A2aMessage message = source.get("message") instanceof Map<?, ?> messageMap ? A2aMessage.fromMap(messageMap) : null;
        return new A2aSendMessageResponse(task, message);
    }
}
