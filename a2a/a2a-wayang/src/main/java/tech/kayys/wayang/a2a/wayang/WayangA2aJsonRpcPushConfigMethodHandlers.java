package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for task-scoped push notification configuration methods.
 */
final class WayangA2aJsonRpcPushConfigMethodHandlers {

    private final WayangA2aTaskStore store;
    private final WayangA2aJsonRpcTaskMethodSupport support;
    private final WayangA2aPushNotificationConfigCommands pushConfigs;

    private WayangA2aJsonRpcPushConfigMethodHandlers(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
        this.support = WayangA2aJsonRpcTaskMethodSupport.fromStore(this.store);
        this.pushConfigs = WayangA2aPushNotificationConfigCommands.fromStore(this.store);
    }

    static WayangA2aJsonRpcPushConfigMethodHandlers fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcPushConfigMethodHandlers(store);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        (request, preflight) -> createPushConfig(request))
                .put(WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        (request, preflight) -> getPushConfig(request))
                .put(WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        (request, preflight) -> listPushConfigs(request))
                .put(WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG,
                        (request, preflight) -> deletePushConfig(request))
                .build();
    }

    private WayangA2aHttpResponse createPushConfig(WayangA2aJsonRpcRequest request) {
        String taskId = WayangA2aPushNotificationConfig.createTaskId(request.params());
        if (support.taskForRequest(request, taskId).isEmpty()) {
            return support.taskNotFound(request, taskId);
        }
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                pushConfigs.create(taskId, request.params()).toMap());
    }

    private WayangA2aHttpResponse getPushConfig(WayangA2aJsonRpcRequest request) {
        String taskId = WayangA2aPushNotificationConfig.requiredTaskId(request.params());
        String id = WayangA2aPushNotificationConfig.requiredConfigId(request.params());
        if (support.taskForRequest(request, taskId).isEmpty()) {
            return support.taskNotFound(request, taskId);
        }
        return pushConfigs.get(taskId, id)
                .map(config -> WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                        request.id(),
                        config.toMap()))
                .orElseGet(() -> support.taskNotFound(request, taskId));
    }

    private WayangA2aHttpResponse listPushConfigs(WayangA2aJsonRpcRequest request) {
        String taskId = WayangA2aPushNotificationConfig.requiredTaskId(request.params());
        if (support.taskForRequest(request, taskId).isEmpty()) {
            return support.taskNotFound(request, taskId);
        }
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                pushConfigs.list(taskId).toJsonRpcMap());
    }

    private WayangA2aHttpResponse deletePushConfig(WayangA2aJsonRpcRequest request) {
        String taskId = WayangA2aPushNotificationConfig.requiredTaskId(request.params());
        String id = WayangA2aPushNotificationConfig.requiredConfigId(request.params());
        if (support.taskForRequest(request, taskId).isEmpty()) {
            return support.taskNotFound(request, taskId);
        }
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                pushConfigs.delete(taskId, id).toJsonRpcMap());
    }
}
