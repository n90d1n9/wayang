package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcAgentCardMethodHandlersTest {

    private final WayangA2aJsonRpcAgentCardMethodHandlers handlers =
            WayangA2aJsonRpcAgentCardMethodHandlers.forAgentCard(card("Wayang Extended"));

    @Test
    void exposesAgentCardMethodHandler() {
        assertThat(handlers.handlers().keySet()).containsExactly(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD);
    }

    @Test
    void dispatchesExtendedAgentCard() {
        WayangA2aHttpResponse response = handlers.handlers()
                .get(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD)
                .dispatch(
                        WayangA2aJsonRpcRequest.of("card-1", WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD, Map.of()),
                        WayangA2aSendMessagePreflight.JsonRpcResult.empty());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(result(response))
                .containsEntry("name", "Wayang Extended")
                .containsEntry("version", "1.0.0");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(WayangA2aHttpResponse response) {
        return (Map<String, Object>) WayangA2aHttpJson.read(response.body()).get("result");
    }

    private static A2aAgentCard card(String name) {
        return A2aAgentCard.minimal(
                name,
                "A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
    }
}
