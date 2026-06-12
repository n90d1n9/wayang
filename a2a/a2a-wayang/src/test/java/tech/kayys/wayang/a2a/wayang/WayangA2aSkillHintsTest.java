package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSkillHintsTest {

    @Test
    void mergesAllowedSkillHintsFromRequestAndMessageMetadata() {
        A2aSendMessageRequest request = new A2aSendMessageRequest(
                null,
                message(Map.of("wayang.allowedSkills", List.of("rag", "tools"))),
                null,
                Map.of(WayangA2a.METADATA_ALLOWED_SKILLS, List.of("chat", "rag")));

        assertThat(WayangA2aSkillHints.allowedSkills(request))
                .containsExactly("chat", "rag", "tools");
    }

    @Test
    void extractsAllowedSkillHintsFromMetadataAlias() {
        assertThat(WayangA2aSkillHints.allowedSkills(Map.of("wayang.allowedSkills", "chat rag")))
                .containsExactly("chat", "rag");
    }

    private static A2aMessage message(Map<String, Object> metadata) {
        return new A2aMessage(
                "message-skills",
                "context-skills",
                "task-skills",
                A2aRole.ROLE_USER,
                List.of(A2aPart.text("ping")),
                metadata,
                List.of(),
                List.of());
    }
}
