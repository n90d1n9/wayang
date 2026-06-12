package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates SendMessage validation so request bodies are parsed once.
 */
final class WayangA2aSendMessagePreflight {

    private final WayangA2aSendMessageRequestGuard requestGuard;
    private final WayangA2aSendMessageConfigurationGuard configurationGuard;

    private WayangA2aSendMessagePreflight(A2aAgentCard agentCard) {
        A2aAgentCard resolved = Objects.requireNonNull(agentCard, "agentCard");
        this.requestGuard = WayangA2aSendMessageRequestGuard.fromAgentCard(resolved);
        this.configurationGuard = WayangA2aSendMessageConfigurationGuard.fromAgentCard(resolved);
    }

    static WayangA2aSendMessagePreflight fromAgentCard(A2aAgentCard agentCard) {
        return new WayangA2aSendMessagePreflight(agentCard);
    }

    WayangA2aHttpRequest enrichHttp(WayangA2aHttpRequest request, String operation) {
        if (!sendOperation(operation)) {
            return request;
        }
        if (request.attributes().get(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE) instanceof A2aSendMessageRequest) {
            return request;
        }
        try {
            return request.withAttribute(
                    WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE,
                    A2aSendMessageRequest.fromJson(request.body()));
        } catch (IllegalArgumentException ignored) {
            return request;
        }
    }

    HttpResult validateHttp(WayangA2aHttpRequest request, String operation) {
        if (!sendOperation(operation)) {
            return HttpResult.valid(request);
        }
        try {
            A2aSendMessageRequest sendRequest = request.sendMessageRequest();
            Optional<WayangA2aHttpResponse> error = requestGuard.validateHttp(sendRequest)
                    .or(() -> configurationGuard.validateHttp(sendRequest));
            return error
                    .map(response -> new HttpResult(request, Optional.of(response)))
                    .orElseGet(() -> HttpResult.valid(request.withAttribute(
                            WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE,
                            sendRequest)));
        } catch (IllegalArgumentException ignored) {
            return HttpResult.valid(request);
        }
    }

    JsonRpcResult validateJsonRpc(WayangA2aJsonRpcRequest request) {
        if (!sendOperation(request.method())) {
            return JsonRpcResult.empty();
        }
        try {
            A2aSendMessageRequest sendRequest = A2aSendMessageRequest.fromMap(request.params());
            Optional<WayangA2aJsonRpcError> error = requestGuard.validateJsonRpc(sendRequest)
                    .or(() -> configurationGuard.validateJsonRpc(sendRequest));
            return new JsonRpcResult(Optional.of(sendRequest), error);
        } catch (IllegalArgumentException ignored) {
            return JsonRpcResult.empty();
        }
    }

    private static boolean sendOperation(String methodOrOperation) {
        String operation = WayangA2aJsonRpcMethods.operation(methodOrOperation).orElse(methodOrOperation);
        return A2aProtocol.OPERATION_SEND_MESSAGE.equals(operation)
                || A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE.equals(operation);
    }

    record HttpResult(WayangA2aHttpRequest request, Optional<WayangA2aHttpResponse> error) {

        HttpResult {
            request = Objects.requireNonNull(request, "request");
            error = error == null ? Optional.empty() : error;
        }

        static HttpResult valid(WayangA2aHttpRequest request) {
            return new HttpResult(request, Optional.empty());
        }
    }

    record JsonRpcResult(Optional<A2aSendMessageRequest> sendRequest, Optional<WayangA2aJsonRpcError> error) {

        JsonRpcResult {
            sendRequest = sendRequest == null ? Optional.empty() : sendRequest;
            error = error == null ? Optional.empty() : error;
        }

        static JsonRpcResult empty() {
            return new JsonRpcResult(Optional.empty(), Optional.empty());
        }
    }
}
