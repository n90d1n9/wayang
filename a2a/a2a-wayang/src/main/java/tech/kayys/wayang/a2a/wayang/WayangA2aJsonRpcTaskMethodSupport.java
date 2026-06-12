package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared task method support for A2A JSON-RPC task-scoped handlers.
 */
final class WayangA2aJsonRpcTaskMethodSupport {

    private final WayangA2aTaskStore store;

    private WayangA2aJsonRpcTaskMethodSupport(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    static WayangA2aJsonRpcTaskMethodSupport fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcTaskMethodSupport(store);
    }

    Optional<A2aTask> taskForRequest(WayangA2aJsonRpcRequest request, String taskId) {
        return WayangA2aTaskAccess.getForJsonRpc(store, request, taskId);
    }

    WayangA2aHttpResponse taskNotFound(WayangA2aJsonRpcRequest request, String taskId) {
        return WayangA2aJsonRpcHttpResponses.jsonRpc(
                WayangA2aJsonRpcResponse.error(request.id(), WayangA2aJsonRpcError.taskNotFound(taskId)));
    }

    String requiredParam(Map<String, Object> params, String key) {
        return WayangA2aMaps.firstString(params, key)
                .orElseThrow(() -> new IllegalArgumentException(key + " is required"));
    }
}
