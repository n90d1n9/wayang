package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reusable HTTP handlers backed by a task store.
 */
public final class WayangA2aTaskHttpHandlers {

    private final WayangA2aTaskStore store;
    private final WayangA2aPushNotificationConfigCommands pushConfigs;

    public WayangA2aTaskHttpHandlers(WayangA2aTaskStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
        this.pushConfigs = WayangA2aPushNotificationConfigCommands.fromStore(store);
    }

    public static Map<String, WayangA2aHttpOperationHandler> forStore(WayangA2aTaskStore store) {
        return new WayangA2aTaskHttpHandlers(store).handlers();
    }

    public Map<String, WayangA2aHttpOperationHandler> handlers() {
        return WayangA2aHttpHandlerMaps.builder()
                .put(A2aProtocol.OPERATION_GET_TASK, this::getTask)
                .put(A2aProtocol.OPERATION_LIST_TASKS, this::listTasks)
                .put(A2aProtocol.OPERATION_CANCEL_TASK, this::cancelTask)
                .put(A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK, this::subscribeToTask)
                .put(A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG, this::createPushConfig)
                .put(A2aProtocol.OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG, this::getPushConfig)
                .put(A2aProtocol.OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS, this::listPushConfigs)
                .put(A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG, this::deletePushConfig)
                .build();
    }

    private WayangA2aHttpResponse getTask(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        return taskForRequest(request, taskId(match))
                .map(task -> WayangA2aHttpResponse.object(200, task.toMap()))
                .orElseGet(() -> notFound(taskId(match)));
    }

    private WayangA2aHttpResponse listTasks(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        WayangA2aTaskListView view = WayangA2aTaskListView.fromStore(
                store,
                WayangA2aTaskQuery.fromAttributes(request.attributes()));
        return WayangA2aHttpResponse.object(200, view.toHttpMap());
    }

    private WayangA2aHttpResponse cancelTask(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        WayangA2aTaskCancelRequest cancelRequest = WayangA2aTaskCancelRequest.fromHttp(
                taskId,
                WayangA2aHttpJson.read(request.body()));
        return WayangA2aHttpResponse.object(200, cancelRequest.apply(store).toMap());
    }

    private WayangA2aHttpResponse subscribeToTask(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        WayangA2aTaskSubscriptionRequest subscription =
                WayangA2aTaskSubscriptionRequest.fromHttp(taskId, request.attributes());
        String body = WayangA2aTaskEventStreams.http(subscription.events(store));
        return WayangA2aHttpResponse.eventStream(200, body);
    }

    private WayangA2aHttpResponse createPushConfig(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        return WayangA2aHttpResponse.object(
                200,
                pushConfigs.create(taskId, WayangA2aHttpJson.read(request.body())).toMap());
    }

    private WayangA2aHttpResponse getPushConfig(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        String configId = configId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        return pushConfigs.get(taskId, configId)
                .map(config -> WayangA2aHttpResponse.object(200, config.toMap()))
                .orElseGet(() -> WayangA2aHttpResponse.error(
                        404,
                        "push_notification_config_not_found",
                        "A2A push notification config not found: " + configId));
    }

    private WayangA2aHttpResponse listPushConfigs(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        return WayangA2aHttpResponse.object(200, pushConfigs.list(taskId).toHttpMap());
    }

    private WayangA2aHttpResponse deletePushConfig(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        String taskId = taskId(match);
        String configId = configId(match);
        if (taskForRequest(request, taskId).isEmpty()) {
            return notFound(taskId);
        }
        WayangA2aPushNotificationConfigDeleteResult result = pushConfigs.delete(taskId, configId);
        return WayangA2aHttpResponse.object(result.deleted() ? 200 : 404, result.toHttpMap());
    }

    private Optional<A2aTask> taskForRequest(WayangA2aHttpRequest request, String taskId) {
        return WayangA2aTaskAccess.getForHttp(store, request, taskId);
    }

    private static WayangA2aHttpResponse notFound(String taskId) {
        return WayangA2aHttpResponse.error(
                404,
                "task_not_found",
                "A2A task not found: " + taskId);
    }

    private static String taskId(WayangA2aHttpRouteMatch match) {
        return match.pathParameter("id").orElseThrow(() -> new IllegalArgumentException("Missing task id path parameter"));
    }

    private static String configId(WayangA2aHttpRouteMatch match) {
        return match.pathParameter("configId")
                .orElseThrow(() -> new IllegalArgumentException("Missing config id path parameter"));
    }

}
