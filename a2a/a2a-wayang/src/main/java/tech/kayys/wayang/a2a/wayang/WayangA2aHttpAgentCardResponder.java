package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Serves built-in HTTP Agent Card operations.
 */
final class WayangA2aHttpAgentCardResponder {

    private final A2aAgentCard publicAgentCard;
    private final A2aAgentCard extendedAgentCard;
    private final WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer;

    private WayangA2aHttpAgentCardResponder(
            A2aAgentCard publicAgentCard,
            A2aAgentCard extendedAgentCard,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        this.publicAgentCard = Objects.requireNonNull(publicAgentCard, "publicAgentCard");
        this.extendedAgentCard = extendedAgentCard;
        this.extendedAgentCardAuthorizer = extendedAgentCardAuthorizer == null
                ? WayangA2aExtendedAgentCardAuthorizer.allowAll()
                : extendedAgentCardAuthorizer;
    }

    static WayangA2aHttpAgentCardResponder fromAgentCards(
            A2aAgentCard publicAgentCard,
            A2aAgentCard extendedAgentCard,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        return new WayangA2aHttpAgentCardResponder(
                publicAgentCard,
                extendedAgentCard,
                extendedAgentCardAuthorizer);
    }

    List<String> operations() {
        return List.of(
                A2aProtocol.OPERATION_DISCOVER_AGENT_CARD,
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD);
    }

    boolean supports(String operation) {
        return A2aProtocol.OPERATION_DISCOVER_AGENT_CARD.equals(operation)
                || A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation);
    }

    Optional<WayangA2aHttpResponse> respond(WayangA2aHttpRequest request, String operation) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        if (A2aProtocol.OPERATION_DISCOVER_AGENT_CARD.equals(operation)) {
            return Optional.of(agentCardResponse(resolvedRequest, publicAgentCard));
        }
        if (A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation)) {
            Optional<WayangA2aHttpResponse> unauthorized = extendedAgentCardAuthorizer.authorize(resolvedRequest);
            if (unauthorized.isPresent()) {
                return unauthorized;
            }
            return Optional.of(agentCardResponse(
                    resolvedRequest,
                    extendedAgentCard == null ? publicAgentCard : extendedAgentCard));
        }
        return Optional.empty();
    }

    private static WayangA2aHttpResponse agentCardResponse(WayangA2aHttpRequest request, A2aAgentCard card) {
        String etag = WayangA2aHttpResponse.agentCardEtag(card);
        return request.header(WayangA2aHttpResponse.HEADER_IF_NONE_MATCH)
                .filter(value -> etagMatches(value, etag))
                .map(ignored -> WayangA2aHttpResponse.agentCardNotModified(card))
                .orElseGet(() -> WayangA2aHttpResponse.agentCard(card));
    }

    private static boolean etagMatches(String header, String etag) {
        if (header == null || header.isBlank()) {
            return false;
        }
        for (String candidate : header.split(",")) {
            if (etag.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }
}
