package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for task event subscription methods.
 */
final class WayangA2aJsonRpcTaskSubscriptionMethodHandlers {

    private final WayangA2aTaskStore store;
    private final WayangA2aJsonRpcTaskMethodSupport support;

    private WayangA2aJsonRpcTaskSubscriptionMethodHandlers(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
        this.support = WayangA2aJsonRpcTaskMethodSupport.fromStore(this.store);
    }

    static WayangA2aJsonRpcTaskSubscriptionMethodHandlers fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcTaskSubscriptionMethodHandlers(store);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK, (request, preflight) -> subscribeToTask(request))
                .build();
    }

    private WayangA2aHttpResponse subscribeToTask(WayangA2aJsonRpcRequest request) {
        String id = support.requiredParam(request.params(), "id");
        WayangA2aTaskSubscriptionRequest subscription =
                WayangA2aTaskSubscriptionRequest.fromJsonRpc(id, request.params());
        A2aTask task = support.taskForRequest(request, id).orElse(null);
        if (task == null) {
            return support.taskNotFound(request, id);
        }
        if (subscription.terminalUnsupported(task)) {
            return WayangA2aJsonRpcHttpResponses.jsonRpc(
                    WayangA2aJsonRpcResponse.error(
                            request.id(),
                            WayangA2aJsonRpcError.unsupportedOperation("Cannot subscribe to terminal task: " + id)));
        }
        String body = WayangA2aTaskEventStreams.jsonRpc(
                request.id(),
                subscription.events(store));
        return WayangA2aJsonRpcHttpResponses.eventStream(body);
    }
}
