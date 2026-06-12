package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSkillRoutingTest {

    @Test
    void preservesSkillAndModeOrderWhenResolvingRequestedSkills() {
        WayangA2aSkillRouting routing = WayangA2aSkillRouting.fromAgentCard(card());
        A2aSendMessageRequest request = request(
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("json", "chat", "json")),
                Map.of("wayang.allowedSkills", List.of("files")));

        assertThat(routing.supportedSkillIds()).containsExactly("chat", "json", "files");
        assertThat(routing.requestedSkillIds(request)).containsExactly("json", "chat", "files");
        assertThat(routing.inputModes(request)).containsExactly(
                "application/json",
                "text/plain",
                "application/octet-stream");
        assertThat(routing.outputModes(request)).containsExactly("application/json", "text/plain");
    }

    @Test
    void fallsBackToDefaultModesWithoutRequestedSkills() {
        WayangA2aSkillRouting routing = WayangA2aSkillRouting.fromAgentCard(card());

        assertThat(routing.inputModes(request(Map.of(), Map.of()))).containsExactly("text/plain");
        assertThat(routing.outputModes(request(Map.of(), Map.of()))).containsExactly("text/plain");
    }

    @Test
    void reportsUnsupportedSkillIdsInRequestOrder() {
        WayangA2aSkillRouting routing = WayangA2aSkillRouting.fromAgentCard(card());
        A2aSendMessageRequest request = request(
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("missing", "chat", "tools", "missing")),
                Map.of());

        assertThat(routing.unsupportedSkillIds(request)).containsExactly("missing", "tools");
    }

    private static A2aSendMessageRequest request(
            Map<String, Object> requestMetadata,
            Map<String, Object> messageMetadata) {
        return new A2aSendMessageRequest(
                null,
                new A2aMessage(
                        "message-routing",
                        "context-routing",
                        "task-routing",
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        messageMetadata,
                        List.of(),
                        List.of()),
                null,
                requestMetadata);
    }

    private static A2aAgentCard card() {
        return new A2aAgentCard(
                "Wayang",
                "A2A endpoint",
                List.of(A2aAgentInterface.httpJson("https://wayang.test/a2a")),
                null,
                "1.0.0",
                null,
                null,
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                List.of(
                        new A2aAgentSkill(
                                "chat",
                                "Chat",
                                "Plain text chat",
                                List.of("chat"),
                                List.of(),
                                List.of("text/plain"),
                                List.of("text/plain"),
                                List.of()),
                        new A2aAgentSkill(
                                "json",
                                "JSON",
                                "Structured JSON exchange",
                                List.of("json"),
                                List.of(),
                                List.of("application/json", "text/plain"),
                                List.of("application/json"),
                                List.of()),
                        new A2aAgentSkill(
                                "files",
                                "Files",
                                "File exchange",
                                List.of("files"),
                                List.of(),
                                List.of("application/octet-stream"),
                                List.of("application/json", "text/plain"),
                                List.of())),
                List.of(),
                null);
    }
}
