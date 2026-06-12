package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Store-backed push notification config commands shared by A2A transports.
 */
final class WayangA2aPushNotificationConfigCommands {

    private final WayangA2aTaskStore store;

    private WayangA2aPushNotificationConfigCommands(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    static WayangA2aPushNotificationConfigCommands fromStore(WayangA2aTaskStore store) {
        return new WayangA2aPushNotificationConfigCommands(store);
    }

    WayangA2aPushNotificationConfig create(String taskId, Map<?, ?> payload) {
        return store.putPushNotificationConfig(WayangA2aPushNotificationConfig.fromMap(taskId, payload));
    }

    Optional<WayangA2aPushNotificationConfig> get(String taskId, String configId) {
        return store.getPushNotificationConfig(taskId, configId);
    }

    WayangA2aPushNotificationConfigList list(String taskId) {
        return new WayangA2aPushNotificationConfigList(store.listPushNotificationConfigs(taskId));
    }

    WayangA2aPushNotificationConfigDeleteResult delete(String taskId, String configId) {
        return new WayangA2aPushNotificationConfigDeleteResult(
                taskId,
                configId,
                store.deletePushNotificationConfig(taskId, configId));
    }
}

record WayangA2aPushNotificationConfigList(List<WayangA2aPushNotificationConfig> configs) {

    WayangA2aPushNotificationConfigList {
        configs = configs == null
                ? List.of()
                : configs.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    Map<String, Object> toHttpMap() {
        List<Map<String, Object>> values = configMaps();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("configCount", values.size());
        payload.put("configs", values);
        return WayangA2aMaps.copyMap(payload);
    }

    Map<String, Object> toJsonRpcMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("configs", configMaps());
        payload.put("nextPageToken", "");
        return WayangA2aMaps.copyMap(payload);
    }

    private List<Map<String, Object>> configMaps() {
        return configs.stream()
                .map(WayangA2aPushNotificationConfig::toMap)
                .toList();
    }
}

record WayangA2aPushNotificationConfigDeleteResult(String taskId, String configId, boolean deleted) {

    WayangA2aPushNotificationConfigDeleteResult {
        taskId = WayangA2aMaps.required(taskId, "taskId");
        configId = WayangA2aMaps.required(configId, "configId");
    }

    Map<String, Object> toHttpMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("configId", configId);
        payload.put("deleted", deleted);
        return WayangA2aMaps.copyMap(payload);
    }

    Map<String, Object> toJsonRpcMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deleted", deleted);
        return WayangA2aMaps.copyMap(payload);
    }
}
