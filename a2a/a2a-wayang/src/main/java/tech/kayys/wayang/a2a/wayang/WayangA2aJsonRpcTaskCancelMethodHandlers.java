package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for task cancellation methods.
 */
final class WayangA2aJsonRpcTaskCancelMethodHandlers {

    private final WayangA2aTaskStore store;
    private final WayangA2aJsonRpcTaskMethodSupport support;

    private WayangA2aJsonRpcTaskCancelMethodHandlers(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
        this.support = WayangA2aJsonRpcTaskMethodSupport.fromStore(this.store);
    }

    static WayangA2aJsonRpcTaskCancelMethodHandlers fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcTaskCancelMethodHandlers(store);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.CANCEL_TASK, (request, preflight) -> cancelTask(request))
                .build();
    }

    private WayangA2aHttpResponse cancelTask(WayangA2aJsonRpcRequest request) {
        String id = support.requiredParam(request.params(), "id");
        if (support.taskForRequest(request, id).isEmpty()) {
            return support.taskNotFound(request, id);
        }
        try {
            WayangA2aTaskCancelRequest cancelRequest =
                    WayangA2aTaskCancelRequest.fromJsonRpc(id, request.params());
            return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                    request.id(),
                    cancelRequest.apply(store).toMap());
        } catch (WayangA2aTaskLifecycleException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return support.taskNotFound(request, id);
        }
    }
}
