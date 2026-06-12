package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillReusePolicyTest {

    @Test
    void selectsSimilarLearnedSkillForReuse() {
        HermesSkillReusePolicy policy = new HermesSkillReusePolicy();
        SkillDefinition candidate = skill(
                "candidate",
                "Create API backup nightly report",
                List.of("rag"));
        SkillDefinition existing = skill(
                "existing",
                "Generate nightly API backup report",
                List.of("rag"));

        Optional<HermesSkillReuseMatch> match = policy.findReusable(candidate, List.of(existing));

        assertThat(match).isPresent();
        assertThat(match.orElseThrow().skill().id()).isEqualTo("existing");
        assertThat(match.orElseThrow().score()).isGreaterThanOrEqualTo(0.72);
        assertThat(match.orElseThrow().reason()).contains("task=", "tools=");
    }

    @Test
    void rejectsUnrelatedLearnedSkills() {
        HermesSkillReusePolicy policy = new HermesSkillReusePolicy();
        SkillDefinition candidate = skill(
                "candidate",
                "Create API backup nightly report",
                List.of("rag"));
        SkillDefinition unrelated = skill(
                "unrelated",
                "Moderate Discord onboarding queue",
                List.of("terminal"));

        assertThat(policy.findReusable(candidate, List.of(unrelated))).isEmpty();
    }

    @Test
    void ignoresExactCandidateId() {
        HermesSkillReusePolicy policy = new HermesSkillReusePolicy();
        SkillDefinition candidate = skill(
                "same",
                "Create API backup nightly report",
                List.of("rag"));

        assertThat(policy.findReusable(candidate, List.of(candidate))).isEmpty();
    }

    private static SkillDefinition skill(String id, String task, List<String> tools) {
        return SkillDefinition.builder()
                .id(id)
                .name(task)
                .description("Learned Hermes workflow for: " + task)
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Do the work.")
                .userPromptTemplate("{{instruction}}")
                .tools(tools)
                .metadata(Map.of("hermes.task", task))
                .build();
    }
}
