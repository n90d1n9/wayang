package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

/**
 * Executes resolved JSON-RPC method handlers and normalizes handler failures.
 */
final class WayangA2aJsonRpcMethodHandlerExecutor {

    private WayangA2aJsonRpcMethodHandlerExecutor() {
    }

    static WayangA2aJsonRpcMethodHandlerExecutor create() {
        return new WayangA2aJsonRpcMethodHandlerExecutor();
    }

    WayangA2aHttpResponse execute(
            WayangA2aJsonRpcRequest request,
            WayangA2aJsonRpcMethodDispatchTable.Entry entry,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        WayangA2aJsonRpcRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2aJsonRpcMethodDispatchTable.Entry resolvedEntry = Objects.requireNonNull(entry, "entry");
        WayangA2aSendMessagePreflight.JsonRpcResult resolvedPreflight = preflight == null
                ? WayangA2aSendMessagePreflight.JsonRpcResult.empty()
                : preflight;
        try {
            return resolvedEntry.dispatch(resolvedRequest, resolvedPreflight);
        } catch (WayangA2aTaskLifecycleException e) {
            return error(resolvedRequest, WayangA2aJsonRpcError.unsupportedOperation(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return error(resolvedRequest, WayangA2aJsonRpcError.invalidParams(e.getMessage()));
        } catch (RuntimeException e) {
            return error(resolvedRequest, WayangA2aJsonRpcError.internalError(e.getMessage()));
        }
    }

    private static WayangA2aHttpResponse error(
            WayangA2aJsonRpcRequest request,
            WayangA2aJsonRpcError error) {
        return WayangA2aJsonRpcHttpResponses.jsonRpc(WayangA2aJsonRpcResponse.error(
                request.id(),
                error));
    }
}
