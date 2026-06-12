package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningRuntimeEventProjectionTest {

    @Test
    void completedProjectionRendersLearningResultMetadata() {
        AgentRequest request = request();
        AgentResponse response = response(true);
        SkillDefinition skill = skill();
        HermesLearningPromotion promotion = HermesLearningPromotion.approved(HermesLearningPlan.create(skill));
        HermesLearningResult result = HermesLearningResult.created(skill)
                .withPromotion(promotion)
                .withPromotionReceipt(HermesLearningPromotionReceipt.from(
                        promotion,
                        HermesLearningResult.created(skill),
                        Map.of(
                                "adapterId", "test-adapter",
                                "targetPlan", Map.of("targetSummary", "definitions=file,artifacts=file"))))
                .withSkillIndexingReceipt(new HermesLearningIndexingReceipt(
                        HermesLearningIndexingReceipt.STATUS_INDEXED,
                        HermesLearningIndexingReceipt.OUTCOME_INDEXED,
                        skill.id(),
                        true,
                        "learned skill indexed",
                        "test-indexer",
                        Map.of("indexName", "hermes-learned-skills")))
                .withLifecycleReport(HermesLearningLifecycleReport.fromStages(
                        HermesLearningStageReport.completed(
                                HermesLearningStageCatalog.SKILL_INDEXING,
                                "learned skill indexed",
                                Map.of("indexed", true))));

        HermesLearningRuntimeEventProjection projection =
                HermesLearningRuntimeEventProjection.completed(request, response, result);

        assertThat(projection.outcome()).isEqualTo("created");
        assertThat(projection.metadata())
                .containsEntry("decision", "created")
                .containsEntry("skillId", "hermes-release-workflow")
                .containsEntry("persisted", true)
                .containsEntry("skillName", "Release Workflow")
                .containsEntry("skillCategory", HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .containsEntry("revision", "1")
                .containsEntry("runId", "run-learning")
                .containsEntry("responseSuccessful", true);
        assertThat(metadataMap(projection.metadata(), HermesLearningMetadataKeys.RESULT))
                .containsKeys(
                        HermesLearningMetadataKeys.PROMOTION,
                        HermesLearningMetadataKeys.PROMOTION_RECEIPT,
                        HermesLearningMetadataKeys.SKILL_INDEXING_RECEIPT,
                        HermesLearningMetadataKeys.LIFECYCLE);
        assertThat(metadataMap(projection.metadata(), HermesLearningMetadataKeys.LIFECYCLE))
                .containsEntry(
                        HermesLearningMetadataKeys.TERMINAL_STAGE,
                        HermesLearningStageCatalog.SKILL_INDEXING);
    }

    @Test
    void completedProjectionUsesFailedOutcomeForUnsuccessfulResponse() {
        HermesLearningRuntimeEventProjection projection = HermesLearningRuntimeEventProjection.completed(
                request(),
                response(false),
                HermesLearningResult.skipped("run was not successful"));

        assertThat(projection.outcome()).isEqualTo("failed");
        assertThat(projection.metadata())
                .containsEntry("decision", "skipped")
                .containsEntry("responseSuccessful", false);
    }

    @Test
    void failedProjectionRendersFailureMetadata() {
        HermesLearningRuntimeEventProjection projection = HermesLearningRuntimeEventProjection.failed(
                request(),
                response(true),
                new IllegalStateException("artifact store unavailable"));

        assertThat(projection.outcome()).isEqualTo("failed");
        assertThat(projection.metadata())
                .containsEntry("runId", "run-learning")
                .containsEntry("responseSuccessful", true)
                .containsEntry("errorType", IllegalStateException.class.getName())
                .containsEntry("error", "artifact store unavailable");
    }

    private static AgentRequest request() {
        return AgentRequest.builder()
                .requestId("req-learning")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .prompt("Learn a reusable release workflow")
                .build();
    }

    private static AgentResponse response(boolean successful) {
        return AgentResponse.builder()
                .runId("run-learning")
                .requestId("req-learning")
                .successful(successful)
                .totalSteps(3)
                .strategy(HermesAgentMode.MODE_ID)
                .build();
    }

    private static SkillDefinition skill() {
        return SkillDefinition.builder()
                .id("hermes-release-workflow")
                .name("Release Workflow")
                .description("Learned Hermes release workflow")
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Do the release work.")
                .userPromptTemplate("{{instruction}}")
                .metadata(Map.of(
                        "hermes.revision", "1",
                        "hermes.latestRequestId", "req-learning",
                        "hermes.lineageRootSkillId", "hermes-release-workflow"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
