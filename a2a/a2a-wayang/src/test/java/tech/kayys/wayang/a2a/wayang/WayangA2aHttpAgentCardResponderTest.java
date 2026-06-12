package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpAgentCardResponderTest {

    @Test
    void exposesBuiltInAgentCardOperations() {
        WayangA2aHttpAgentCardResponder responder =
                WayangA2aHttpAgentCardResponder.fromAgentCards(card("Wayang Public"), null, null);

        assertThat(responder.operations()).containsExactly(
                A2aProtocol.OPERATION_DISCOVER_AGENT_CARD,
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD);
        assertThat(responder.supports(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)).isTrue();
        assertThat(responder.supports(A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD)).isTrue();
        assertThat(responder.supports(A2aProtocol.OPERATION_SEND_MESSAGE)).isFalse();
        assertThat(responder.respond(
                WayangA2aHttpRequest.get("/message:send"),
                A2aProtocol.OPERATION_SEND_MESSAGE)).isEmpty();
    }

    @Test
    void servesPublicAgentCardWithConditionalEtag() {
        A2aAgentCard publicCard = card("Wayang Public");
        WayangA2aHttpAgentCardResponder responder =
                WayangA2aHttpAgentCardResponder.fromAgentCards(publicCard, null, null);

        WayangA2aHttpResponse response = responder.respond(
                WayangA2aHttpRequest.get(A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH),
                A2aProtocol.OPERATION_DISCOVER_AGENT_CARD).orElseThrow();
        String etag = String.valueOf(response.headers().get(WayangA2aHttpResponse.HEADER_ETAG));
        WayangA2aHttpResponse notModified = responder.respond(
                new WayangA2aHttpRequest(
                        "GET",
                        A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH,
                        "",
                        Map.of(WayangA2aHttpResponse.HEADER_IF_NONE_MATCH, "\"stale\", " + etag),
                        Map.of()),
                A2aProtocol.OPERATION_DISCOVER_AGENT_CARD).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(A2aAgentCard.fromJson(response.body()).name()).isEqualTo("Wayang Public");
        assertThat(etag).startsWith("\"").endsWith("\"");
        assertThat(notModified.statusCode()).isEqualTo(304);
        assertThat(notModified.body()).isBlank();
        assertThat(notModified.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ETAG, etag);
    }

    @Test
    void servesExtendedAgentCardThroughAuthorizer() {
        WayangA2aHttpAgentCardResponder responder = WayangA2aHttpAgentCardResponder.fromAgentCards(
                card("Wayang Public"),
                card("Wayang Extended"),
                WayangA2aExtendedAgentCardAuthorizer.requireBearerToken("secret-token"));

        WayangA2aHttpResponse unauthorized = responder.respond(
                WayangA2aHttpRequest.get("/extendedAgentCard"),
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD).orElseThrow();
        WayangA2aHttpResponse authorized = responder.respond(
                new WayangA2aHttpRequest(
                        "GET",
                        "/extendedAgentCard",
                        "",
                        Map.of(WayangA2aHttpResponse.HEADER_AUTHORIZATION, "Bearer secret-token"),
                        Map.of()),
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD).orElseThrow();

        assertThat(unauthorized.statusCode()).isEqualTo(401);
        assertThat(unauthorized.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                        "Bearer realm=\"a2a-extended-agent-card\"");
        assertThat(authorized.statusCode()).isEqualTo(200);
        assertThat(A2aAgentCard.fromJson(authorized.body()).name()).isEqualTo("Wayang Extended");
    }

    @Test
    void fallsBackToPublicCardWhenExtendedCardIsNotConfigured() {
        WayangA2aHttpAgentCardResponder responder =
                WayangA2aHttpAgentCardResponder.fromAgentCards(card("Wayang Public"), null, null);

        WayangA2aHttpResponse response = responder.respond(
                WayangA2aHttpRequest.get("/extendedAgentCard"),
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD).orElseThrow();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(A2aAgentCard.fromJson(response.body()).name()).isEqualTo("Wayang Public");
    }

    private static A2aAgentCard card(String name) {
        return A2aAgentCard.minimal(
                name,
                "A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
