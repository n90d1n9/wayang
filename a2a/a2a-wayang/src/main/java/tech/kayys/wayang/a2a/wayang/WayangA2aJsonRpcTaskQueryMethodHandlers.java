package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for task read and list methods.
 */
final class WayangA2aJsonRpcTaskQueryMethodHandlers {

    private final WayangA2aTaskStore store;
    private final WayangA2aJsonRpcTaskMethodSupport support;

    private WayangA2aJsonRpcTaskQueryMethodHandlers(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
        this.support = WayangA2aJsonRpcTaskMethodSupport.fromStore(this.store);
    }

    static WayangA2aJsonRpcTaskQueryMethodHandlers fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcTaskQueryMethodHandlers(store);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.GET_TASK, (request, preflight) -> getTask(request))
                .put(WayangA2aJsonRpcMethods.LIST_TASKS, (request, preflight) -> listTasks(request))
                .build();
    }

    private WayangA2aHttpResponse getTask(WayangA2aJsonRpcRequest request) {
        String id = support.requiredParam(request.params(), "id");
        return support.taskForRequest(request, id)
                .map(task -> WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                        request.id(),
                        task.toMap()))
                .orElseGet(() -> support.taskNotFound(request, id));
    }

    private WayangA2aHttpResponse listTasks(WayangA2aJsonRpcRequest request) {
        WayangA2aTaskQuery query = WayangA2aTaskQuery.fromAttributes(request.params());
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                WayangA2aTaskListView.fromStore(store, query).toJsonRpcMap());
    }
}
