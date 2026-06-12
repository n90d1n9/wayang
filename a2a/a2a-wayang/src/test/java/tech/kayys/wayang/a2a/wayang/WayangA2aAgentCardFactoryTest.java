package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aAgentCardFactoryTest {

    @Test
    void buildsAgentCardFromWayangSkillDefinitions() {
        WayangA2aAgentProfile profile = new WayangA2aAgentProfile(
                "Wayang",
                "Agentic core",
                WayangA2aAgentProfile.httpJson("Wayang", "Agentic core", "https://wayang.test/a2a")
                        .supportedInterfaces(),
                null,
                "2.0.0",
                "https://wayang.test/docs",
                new A2aAgentCapabilities(true, false,
                        List.of(new A2aAgentExtension("https://a2ui.org/a2a-extension/a2ui/v0.8",
                                "A2UI rendering", false, Map.of())), true),
                Map.of("bearer", Map.of("httpAuthSecurityScheme", Map.of("scheme", "Bearer"))),
                List.of(Map.of("bearer", List.of())),
                List.of("text/plain", "application/json"),
                List.of("text/plain"),
                "https://wayang.test/icon.png");
        SkillDefinition skill = SkillDefinition.builder()
                .id("chat")
                .name("Chat")
                .description("General agent chat")
                .category("general")
                .systemPrompt("Chat helpfully.")
                .build();

        A2aAgentCard card = new WayangA2aAgentCardFactory()
                .fromSkillDefinitions(profile, List.of(skill));

        assertThat(card.name()).isEqualTo("Wayang");
        assertThat(card.version()).isEqualTo("2.0.0");
        assertThat(card.preferredInterface().protocolBinding()).isEqualTo(A2aProtocol.BINDING_HTTP_JSON);
        assertThat(card.capabilities().streaming()).isTrue();
        assertThat(card.capabilities().extensions()).singleElement()
                .extracting(A2aAgentExtension::uri)
                .isEqualTo("https://a2ui.org/a2a-extension/a2ui/v0.8");
        assertThat(card.skills()).singleElement().extracting(skillCard -> skillCard.id()).isEqualTo("chat");
        assertThat(card.securitySchemes()).containsKey("bearer");
    }
}
