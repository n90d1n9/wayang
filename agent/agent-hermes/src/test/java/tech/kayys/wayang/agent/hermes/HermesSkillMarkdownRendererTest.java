package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillMarkdownRendererTest {

    @Test
    void rendersPortableSkillMdWithEscapedFrontmatter() {
        SkillDefinition skill = SkillDefinition.builder()
                .id("hermes-audit")
                .name("Audit Evidence")
                .description("Learned \"audit\" workflow\nwith context")
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("## Procedure\n1. Verify evidence.")
                .metadata(Map.of(
                        "version", "2.0.0",
                        "hermes.revision", "3",
                        "hermes.lineageRootSkillId", "hermes-root",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY))
                .build();
        HermesLearningSignal signal = new HermesLearningSignal(
                "req-render",
                "Audit evidence",
                "Done",
                true,
                List.of(),
                List.of(),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));

        SkillArtifact artifact = new HermesSkillMarkdownRenderer().render(skill, signal);
        String markdown = new String(artifact.content(), StandardCharsets.UTF_8);

        assertThat(artifact.reference().skillId()).isEqualTo("hermes-audit");
        assertThat(artifact.reference().name()).isEqualTo("SKILL.md");
        assertThat(markdown)
                .contains("name: hermes-audit")
                .contains("description: Learned 'audit' workflow with context")
                .contains("version: 2.0.0")
                .contains("learned-from-request: req-render")
                .contains("revision: 3")
                .contains("lineage-root-skill-id: hermes-root")
                .contains("merge-strategy: " + HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)
                .contains("# Audit Evidence")
                .contains("## Procedure");
    }
}
