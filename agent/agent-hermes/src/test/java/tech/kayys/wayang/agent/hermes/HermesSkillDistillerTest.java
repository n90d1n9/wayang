package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentState;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillDistillerTest {

    @Test
    void distillsInitialRevisionMetadataForLearnedSkill() {
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        HermesLearningSignal signal = signal(
                "req-a",
                "Generate nightly API backup report",
                List.of("rag"));

        SkillDefinition skill = distiller.distill(signal, HermesAgentModeConfig.defaults());

        assertThat(skill.metadata())
                .containsEntry("hermes.revision", "1")
                .containsEntry("hermes.revisionStatus", "initial")
                .containsEntry("hermes.lineageRootSkillId", skill.id())
                .containsEntry("hermes.lineageDepth", "1")
                .containsEntry("hermes.createdRequestId", "req-a")
                .containsEntry("hermes.latestRequestId", "req-a")
                .containsEntry("hermes.sourceRequestIds", "req-a")
                .containsEntry("hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY)
                .containsEntry("hermes.lastCandidateSkillId", skill.id());
    }

    @Test
    void refinesSkillWithLineageAndMergeMetadata() {
        HermesSkillDistiller distiller = new HermesSkillDistiller();
        SkillDefinition existing = distiller.distill(
                signal("req-a", "Generate nightly API backup report", List.of("rag")),
                HermesAgentModeConfig.defaults());
        SkillDefinition candidate = distiller.distill(
                signal("req-b", "Create API backup nightly report", List.of("terminal")),
                HermesAgentModeConfig.defaults());

        SkillDefinition refined = distiller.refine(
                existing,
                candidate,
                signal("req-b", "Create API backup nightly report", List.of("terminal")),
                "similar skill already exists: " + existing.id() + " (task=0.750, tools=0.000)");

        assertThat(refined.id()).isEqualTo(existing.id());
        assertThat(refined.tools()).containsExactly("rag", "terminal");
        assertThat(refined.metadata())
                .containsEntry("hermes.revision", "2")
                .containsEntry("hermes.previousRevision", "1")
                .containsEntry("hermes.supersedesRevision", "1")
                .containsEntry("hermes.revisionStatus", "refined")
                .containsEntry("hermes.lineageRootSkillId", existing.id())
                .containsEntry("hermes.lineageDepth", "2")
                .containsEntry("hermes.createdRequestId", "req-a")
                .containsEntry("hermes.latestRequestId", "req-b")
                .containsEntry("hermes.sourceRequestIds", "req-a,req-b")
                .containsEntry("hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)
                .containsEntry("hermes.mergeReason",
                        "similar skill already exists: " + existing.id() + " (task=0.750, tools=0.000)")
                .containsEntry("hermes.supersedesSkillId", existing.id())
                .containsEntry("hermes.derivedFromSkillId", candidate.id())
                .containsEntry("hermes.lastCandidateSkillId", candidate.id())
                .containsEntry("hermes.task", "Create API backup nightly report")
                .containsEntry("hermes.requestId", "req-b");
    }

    private static HermesLearningSignal signal(String requestId, String task, List<String> toolIds) {
        List<AgentState.ReasoningStep> steps = java.util.stream.IntStream.rangeClosed(1, 3)
                .mapToObj(index -> new AgentState.ReasoningStep(
                        index,
                        "Step " + index,
                        new AgentState.AgentAction(
                                toolIds.get(Math.min(index - 1, toolIds.size() - 1)),
                                "do work",
                                Map.of("index", index),
                                Instant.parse("2026-06-02T00:00:00Z")),
                        "Observation " + index,
                        10,
                        true))
                .toList();
        return new HermesLearningSignal(
                requestId,
                task,
                "Completed " + task,
                true,
                steps,
                toolIds,
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }
}
