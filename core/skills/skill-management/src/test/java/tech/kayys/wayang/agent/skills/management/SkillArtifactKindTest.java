package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactKindTest {

    @Test
    void resolvesKindsFromStableLabels() {
        assertThat(SkillArtifactKind.fromLabel(" rag-index "))
                .isEqualTo(SkillArtifactKind.RAG_INDEX);
        assertThat(SkillArtifactKind.fromLabel("mcp-descriptor"))
                .isEqualTo(SkillArtifactKind.MCP_DESCRIPTOR);
    }

    @Test
    void rejectsUnknownLabels() {
        assertThatThrownBy(() -> SkillArtifactKind.fromLabel("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown skill artifact kind label: unknown");
    }
}
