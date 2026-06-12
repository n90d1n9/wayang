package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageViewTest {

    @Test
    void groupsEntriesByLineageRootAndSummarizesCurrentRevision() {
        SkillDefinition first = skill(
                "hermes-api-report",
                "Generate nightly API backup report",
                Map.ofEntries(
                        Map.entry("hermes.revision", "1"),
                        Map.entry("hermes.revisionStatus", "initial"),
                        Map.entry("hermes.lineageRootSkillId", "hermes-api-report"),
                        Map.entry("hermes.lineageDepth", "1"),
                        Map.entry("hermes.sourceRequestIds", "req-a"),
                        Map.entry("hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY),
                        Map.entry("hermes.requestId", "req-a"),
                        Map.entry("hermes.learningQualityScore", "0.85"),
                        Map.entry("hermes.learningQualityThreshold", "0.60")));
        SkillDefinition refined = skill(
                "hermes-api-report-shadow",
                "Create API backup nightly report",
                Map.ofEntries(
                        Map.entry("hermes.revision", "2"),
                        Map.entry("hermes.previousRevision", "1"),
                        Map.entry("hermes.supersedesRevision", "1"),
                        Map.entry("hermes.revisionStatus", "refined"),
                        Map.entry("hermes.lineageRootSkillId", "hermes-api-report"),
                        Map.entry("hermes.lineageDepth", "2"),
                        Map.entry("hermes.sourceRequestIds", "req-a,req-b"),
                        Map.entry("hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY),
                        Map.entry("hermes.mergeReason", "similar skill already exists"),
                        Map.entry("hermes.requestId", "req-b")));
        SkillDefinition unrelated = skill(
                "hermes-discord-moderation",
                "Moderate Discord onboarding queue",
                Map.of(
                        "hermes.revision", "1",
                        "hermes.lineageRootSkillId", "hermes-discord-moderation"));

        HermesSkillLineageView view = HermesSkillLineageView.from(
                refined.id(),
                Optional.of(refined),
                List.of(unrelated, refined, first));

        assertThat(view.found()).isTrue();
        assertThat(view.rootSkillId()).isEqualTo(first.id());
        assertThat(view.currentSkillId()).isEqualTo(refined.id());
        assertThat(view.currentRevision()).isEqualTo("2");
        assertThat(view.hasRefinements()).isTrue();
        assertThat(view.sourceRequestIds()).containsExactly("req-a", "req-b");
        assertThat(view.mergeStrategies())
                .containsExactly(
                        HermesSkillRevisionMetadata.INITIAL_STRATEGY,
                        HermesSkillRevisionMetadata.REFINEMENT_STRATEGY);
        assertThat(view.entries()).extracting(HermesSkillLineageEntry::skillId)
                .containsExactly(first.id(), refined.id());
        assertThat(view.toMetadata())
                .containsEntry("requestedSkillId", refined.id())
                .containsEntry("found", true)
                .containsEntry("rootSkillId", first.id())
                .containsEntry("currentSkillId", refined.id())
                .containsEntry("currentRevision", "2")
                .containsEntry("entryCount", 2)
                .containsEntry("hasRefinements", true);
        assertThat(view.toMetadata().get("entries")).asList().hasSize(2);
    }

    @Test
    void rendersMissingViewWhenSkillCannotBeFound() {
        HermesSkillLineageView view = HermesSkillLineageView.from(
                "missing",
                Optional.empty(),
                List.of());

        assertThat(view.found()).isFalse();
        assertThat(view.entries()).isEmpty();
        assertThat(view.toMetadata())
                .containsEntry("requestedSkillId", "missing")
                .containsEntry("found", false)
                .containsEntry("entryCount", 0);
    }

    private static SkillDefinition skill(String id, String task, Map<String, Object> metadata) {
        return SkillDefinition.builder()
                .id(id)
                .name(task)
                .description("Learned Hermes workflow for: " + task)
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Do the work.")
                .userPromptTemplate("{{instruction}}")
                .tools(List.of("rag"))
                .metadata(withTask(metadata, task))
                .build();
    }

    private static Map<String, Object> withTask(Map<String, Object> metadata, String task) {
        Map<String, Object> values = new java.util.LinkedHashMap<>(metadata);
        values.put("hermes.task", task);
        return values;
    }
}
