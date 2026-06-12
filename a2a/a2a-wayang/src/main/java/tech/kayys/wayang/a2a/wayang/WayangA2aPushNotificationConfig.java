package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Store-neutral push notification config for one A2A task.
 */
public record WayangA2aPushNotificationConfig(
        String taskId,
        String configId,
        String url,
        Map<String, Object> auth,
        Map<String, Object> metadata) {

    public WayangA2aPushNotificationConfig {
        taskId = WayangA2aMaps.required(taskId, "taskId");
        configId = WayangA2aMaps.required(configId, "configId");
        url = WayangA2aMaps.required(url, "url");
        auth = WayangA2aMaps.copyMap(auth);
        metadata = WayangA2aMaps.copyMap(metadata);
    }

    public static WayangA2aPushNotificationConfig fromMap(String taskId, Map<?, ?> payload) {
        Map<String, Object> values = WayangA2aMaps.copyMap(payload);
        return new WayangA2aPushNotificationConfig(
                taskId,
                configId(values),
                WayangA2aMaps.required(WayangA2aMaps.optional(values.get("url")), "url"),
                object(values.get("auth")),
                object(values.get("metadata")));
    }

    static String createTaskId(Map<?, ?> payload) {
        return WayangA2aMaps.firstString(WayangA2aMaps.copyMap(payload), "taskId", "id")
                .orElseThrow(() -> new IllegalArgumentException("taskId is required"));
    }

    static String requiredTaskId(Map<?, ?> payload) {
        return WayangA2aMaps.firstString(WayangA2aMaps.copyMap(payload), "taskId")
                .orElseThrow(() -> new IllegalArgumentException("taskId is required"));
    }

    static String requiredConfigId(Map<?, ?> payload) {
        return WayangA2aMaps.firstString(WayangA2aMaps.copyMap(payload), "configId", "id")
                .orElseThrow(() -> new IllegalArgumentException("configId is required"));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("taskId", taskId);
        values.put("configId", configId);
        values.put("url", url);
        if (!auth.isEmpty()) {
            values.put("auth", auth);
        }
        if (!metadata.isEmpty()) {
            values.put("metadata", metadata);
        }
        return WayangA2aMaps.copyMap(values);
    }

    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? WayangA2aMaps.copyMap(map) : Map.of();
    }

    private static String configId(Map<String, Object> values) {
        return WayangA2aMaps.firstString(values, "configId", "id")
                .orElse("default");
    }
}
