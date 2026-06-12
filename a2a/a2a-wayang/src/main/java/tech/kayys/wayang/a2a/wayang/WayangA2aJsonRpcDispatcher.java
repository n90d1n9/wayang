package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC-over-HTTP dispatcher for A2A methods.
 */
public final class WayangA2aJsonRpcDispatcher {

    public static final String MEDIA_TYPE_JSON = "application/json";

    private final A2aAgentCard extendedAgentCard;
    private final WayangA2aJsonRpcRequestDispatcher requestDispatcher;
    private final WayangA2aJsonRpcRequestDecoder requestDecoder;

    public WayangA2aJsonRpcDispatcher(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService) {
        this(extendedAgentCard, store, sendMessageService, List.of());
    }

    WayangA2aJsonRpcDispatcher(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService,
            List<? extends WayangA2aJsonRpcMethodHandlerProvider> extraProviders) {
        this.extendedAgentCard = Objects.requireNonNull(extendedAgentCard, "extendedAgentCard");
        WayangA2aTaskStore resolvedStore = Objects.requireNonNull(store, "store");
        WayangA2aSendMessageService resolvedService =
                Objects.requireNonNull(sendMessageService, "sendMessageService");
        this.requestDispatcher = WayangA2aJsonRpcRequestDispatcher.fromAgentCard(
                this.extendedAgentCard,
                resolvedStore,
                resolvedService,
                extraProviders);
        this.requestDecoder = WayangA2aJsonRpcRequestDecoder.create();
    }

    public static WayangA2aJsonRpcDispatcher forExecution(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aAgentExecutor executor) {
        return new WayangA2aJsonRpcDispatcher(
                extendedAgentCard,
                store,
                new WayangA2aSendMessageService(store, executor));
    }

    public A2aAgentCard agentCard() {
        return extendedAgentCard;
    }

    public List<String> dispatchMethods() {
        return requestDispatcher.methods();
    }

    public Map<String, Object> dispatchCoverage() {
        return methodDispatchCoverage().toMap();
    }

    WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage() {
        return requestDispatcher.coverage();
    }

    WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot() {
        return requestDispatcher.methodHandlerRegistrySnapshot();
    }

    public WayangA2aHttpResponse dispatchJson(String body) {
        WayangA2aJsonRpcRequestDecoder.Result decoded = requestDecoder.decode(body);
        return decoded.error()
                .orElseGet(() -> dispatch(decoded.request().orElseThrow()));
    }

    public WayangA2aHttpResponse dispatch(WayangA2aJsonRpcRequest request) {
        return requestDispatcher.dispatch(request);
    }

}
