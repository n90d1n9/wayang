package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.List;
import java.util.Objects;

/**
 * Assembles the complete JSON-RPC method dispatch table from handler groups.
 */
final class WayangA2aJsonRpcMethodDispatchTableFactory {

    private WayangA2aJsonRpcMethodDispatchTableFactory() {
    }

    static WayangA2aJsonRpcMethodDispatchTable create(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService) {
        return create(extendedAgentCard, store, sendMessageService, List.of());
    }

    static WayangA2aJsonRpcMethodDispatchTable create(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService,
            List<? extends WayangA2aJsonRpcMethodHandlerProvider> extraProviders) {
        A2aAgentCard resolvedAgentCard = Objects.requireNonNull(extendedAgentCard, "extendedAgentCard");
        WayangA2aTaskStore resolvedStore = Objects.requireNonNull(store, "store");
        WayangA2aSendMessageService resolvedService =
                Objects.requireNonNull(sendMessageService, "sendMessageService");
        return create(coreHandlerRegistry(
                resolvedAgentCard,
                resolvedStore,
                resolvedService,
                extraProviders));
    }

    static WayangA2aJsonRpcMethodDispatchTable create(WayangA2aJsonRpcMethodHandlerRegistry registry) {
        return WayangA2aJsonRpcMethodDispatchTable.requireComplete(
                Objects.requireNonNull(registry, "registry").handlers());
    }

    static WayangA2aJsonRpcMethodHandlerRegistry coreHandlerRegistry(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService) {
        return coreHandlerRegistry(extendedAgentCard, store, sendMessageService, List.of());
    }

    static WayangA2aJsonRpcMethodHandlerRegistry coreHandlerRegistry(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService,
            List<? extends WayangA2aJsonRpcMethodHandlerProvider> extraProviders) {
        A2aAgentCard resolvedAgentCard = Objects.requireNonNull(extendedAgentCard, "extendedAgentCard");
        WayangA2aTaskStore resolvedStore = Objects.requireNonNull(store, "store");
        WayangA2aSendMessageService resolvedService =
                Objects.requireNonNull(sendMessageService, "sendMessageService");
        return WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .addProvider(WayangA2aJsonRpcSendMessageMethodHandlers.forService(resolvedService))
                .addProvider(WayangA2aJsonRpcTaskMethodHandlers.fromStore(resolvedStore))
                .addProvider(WayangA2aJsonRpcAgentCardMethodHandlers.forAgentCard(resolvedAgentCard))
                .addProviders(extraProviders)
                .build();
    }
}
