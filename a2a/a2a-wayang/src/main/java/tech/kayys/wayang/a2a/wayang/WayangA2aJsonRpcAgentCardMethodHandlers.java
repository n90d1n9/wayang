package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for Agent Card methods.
 */
final class WayangA2aJsonRpcAgentCardMethodHandlers implements WayangA2aJsonRpcMethodHandlerProvider {

    private final A2aAgentCard agentCard;

    private WayangA2aJsonRpcAgentCardMethodHandlers(A2aAgentCard agentCard) {
        this.agentCard = Objects.requireNonNull(agentCard, "agentCard");
    }

    static WayangA2aJsonRpcAgentCardMethodHandlers forAgentCard(A2aAgentCard agentCard) {
        return new WayangA2aJsonRpcAgentCardMethodHandlers(agentCard);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .put(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD, this::extendedAgentCard)
                .build();
    }

    @Override
    public WayangA2aJsonRpcMethodHandlerGroup group() {
        return WayangA2aJsonRpcMethodHandlerGroup.of(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_AGENT_CARD,
                handlers(),
                WayangA2aJsonRpcCoreMethodHandlerContributions.agentCard());
    }

    private WayangA2aHttpResponse extendedAgentCard(
            WayangA2aJsonRpcRequest request,
            WayangA2aSendMessagePreflight.JsonRpcResult preflight) {
        return WayangA2aJsonRpcHttpResponses.jsonRpcResult(
                request.id(),
                agentCard.toMap());
    }
}
