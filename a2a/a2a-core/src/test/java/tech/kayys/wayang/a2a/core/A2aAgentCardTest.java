package tech.kayys.wayang.a2a.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2aAgentCardTest {

    @Test
    void roundTripsA2aOnePointZeroAgentCard() {
        A2aAgentCard card = new A2aAgentCard(
                "Wayang Agent",
                "Agentic core endpoint",
                List.of(
                        A2aAgentInterface.httpJson("https://wayang.test/a2a"),
                        new A2aAgentInterface("https://wayang.test/a2a/jsonrpc", A2aProtocol.BINDING_JSONRPC,
                                "tenant-a", A2aProtocol.VERSION)),
                new A2aAgentProvider("https://kayys.tech", "Kayys"),
                "1.2.3",
                "https://wayang.test/docs",
                new A2aAgentCapabilities(true, false,
                        List.of(new A2aAgentExtension("https://a2ui.org/a2a-extension/a2ui/v0.8",
                                "A2UI surfaces", false, Map.of())), true),
                Map.of("bearer", Map.of("httpAuthSecurityScheme", Map.of("scheme", "Bearer"))),
                List.of(Map.of("bearer", List.of())),
                List.of("text/plain"),
                List.of("text/plain", "application/json"),
                List.of(A2aAgentSkill.of("rag", "RAG", "Answer with retrieved context", List.of("rag", "search"))),
                List.of(),
                "https://wayang.test/icon.png");

        A2aAgentCard decoded = A2aAgentCard.fromJson(card.toJson());

        assertThat(decoded.name()).isEqualTo("Wayang Agent");
        assertThat(decoded.preferredInterface().protocolBinding()).isEqualTo(A2aProtocol.BINDING_HTTP_JSON);
        assertThat(decoded.supportedInterfaces()).hasSize(2);
        assertThat(decoded.capabilities().streaming()).isTrue();
        assertThat(decoded.capabilities().extendedAgentCard()).isTrue();
        assertThat(decoded.securitySchemes()).containsKey("bearer");
        assertThat(decoded.skills()).singleElement().extracting(A2aAgentSkill::id).isEqualTo("rag");
    }

    @Test
    void createsMinimalHttpJsonAgentCard() {
        A2aAgentCard card = A2aAgentCard.minimal(
                "Wayang",
                "Default Wayang A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General agent chat", List.of("chat"))));

        Map<String, Object> payload = card.toMap();

        assertThat(payload).containsKeys("supportedInterfaces", "capabilities", "defaultInputModes", "skills");
        assertThat(card.preferredInterface().protocolVersion()).isEqualTo(A2aProtocol.VERSION);
        assertThat(card.defaultInputModes()).containsExactly("text/plain");
    }
}
