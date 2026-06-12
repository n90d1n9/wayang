package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aSendMessageResponse;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for SendMessage methods.
 */
final class WayangA2aJsonRpcSendMessageMethodHandlers implements WayangA2aJsonRpcMethodHandlerProvider {

    private final WayangA2aSendMessageService service;

    private WayangA2aJsonRpcSendMessageMethodHandlers(WayangA2aSendMessageService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    static WayangA2aJsonRpcSendMessageMethodHandlers forService(WayangA2aSendMessageService service) {
        return new WayangA2aJsonRpcSendMessageMethodHandlers(service);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.SEND_MESSAGE, this::sendMessage)
                .put(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE, this::sendStreamingMessage)
                .build();
    }

    @Override
    public WayangA2aJsonRpcMethodHandlerGroup group() {
        return WayangA2aJsonRpcMethodHandlerGroup.of(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                handlers(),
                WayangA2aJsonRpcCoreMethodHandlerContributions.sendMessage());
    }

    private WayangA2aHttpResponse sendMessage(
            WayangA2aJsonRpcRequest request,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                A2aSendMessageResponse.task(service.send(sendRequest(request, preflight)).responseTask()).toMap());
    }

    private WayangA2aHttpResponse sendStreamingMessage(
            WayangA2aJsonRpcRequest request,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        WayangA2aSendMessageResult result = service.stream(sendRequest(request, preflight));
        return WayangA2aJsonRpcHttpResponses.eventStream(WayangA2aJsonRpcResponse.result(
                request.id(),
                Map.of("task", result.responseTask().toMap())).toEvent());
    }

    private static A2aSendMessageRequest sendRequest(
            WayangA2aJsonRpcRequest request,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        WayangA2aSendMessagePreflight.JsonRpcResult resolvedPreflight = preflight == null
                ? WayangA2aSendMessagePreflight.JsonRpcResult.empty()
                : preflight;
        return resolvedPreflight.sendRequest()
                .orElseGet(() -> A2aSendMessageRequest.fromMap(request.params()));
    }
}
