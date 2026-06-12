package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeEventTest {

    @Test
    void normalizesIdentityAndRendersMetadataEnvelope() {
        Instant observedAt = Instant.parse("2026-06-03T01:02:03Z");
        HermesRuntimeEvent event = new HermesRuntimeEvent(
                " ",
                " ",
                " req-a ",
                " ",
                null,
                " user-a ",
                " ",
                observedAt,
                Map.of("k", "v"));

        assertThat(event.eventId()).startsWith("hermes-event-runtime-event-req-a-");
        assertThat(event.type()).isEqualTo("runtime.event");
        assertThat(event.requestId()).isEqualTo("req-a");
        assertThat(event.tenantId()).isEqualTo("default");
        assertThat(event.sessionId()).isEmpty();
        assertThat(event.userId()).isEqualTo("user-a");
        assertThat(event.outcome()).isEqualTo("unknown");
        assertThat(event.toMetadata())
                .containsEntry("type", "runtime.event")
                .containsEntry("requestId", "req-a")
                .containsEntry("tenantId", "default")
                .containsEntry("occurredAt", observedAt.toString())
                .containsEntry("metadata", Map.of("k", "v"));
    }

    @Test
    void createsRequestPlanAndResponseEvents() {
        AgentRequest request = AgentRequest.builder()
                .requestId("req-events")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .prompt("Plan a reusable release workflow")
                .build();
        HermesRequestPlan plan = new HermesRequestPlanner(HermesAgentModeConfig.defaults()).plan(request);
        HermesRuntimeEvent planned = HermesRuntimeEvent.requestPlanned(request, plan);
        HermesRuntimeEvent completed = HermesRuntimeEvent.responseCompleted(request, AgentResponse.builder()
                .runId("run-a")
                .requestId("req-events")
                .successful(true)
                .totalSteps(4)
                .strategy(HermesAgentMode.MODE_ID)
                .durationMs(25)
                .build());

        assertThat(planned.type()).isEqualTo(HermesRuntimeEvent.TYPE_REQUEST_PLANNED);
        assertThat(planned.requestId()).isEqualTo("req-events");
        assertThat(planned.tenantId()).isEqualTo("tenant-a");
        assertThat(planned.metadata()).containsKey("requestPlan");
        assertThat(completed.type()).isEqualTo(HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED);
        assertThat(completed.outcome()).isEqualTo("successful");
        assertThat(completed.metadata())
                .containsEntry("runId", "run-a")
                .containsEntry("totalSteps", 4)
                .containsEntry("durationMs", 25L);
    }

    @Test
    void createsSkillLearningOutcomeEvents() {
        AgentRequest request = AgentRequest.builder()
                .requestId("req-learning")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .prompt("Learn a reusable release workflow")
                .build();
        AgentResponse response = AgentResponse.builder()
                .runId("run-learning")
                .requestId("req-learning")
                .successful(true)
                .totalSteps(3)
                .strategy(HermesAgentMode.MODE_ID)
                .build();
        SkillDefinition skill = SkillDefinition.builder()
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

        HermesRuntimeEvent completed = HermesRuntimeEvent.skillLearningCompleted(
                request,
                response,
                HermesLearningResult.created(skill)
                        .withPromotion(HermesLearningPromotion.approved(HermesLearningPlan.create(skill)))
                        .withPromotionReceipt(HermesLearningPromotionReceipt.from(
                                HermesLearningPromotion.approved(HermesLearningPlan.create(skill)),
                                HermesLearningResult.created(skill),
                                Map.of(
                                        "adapterId", "test-adapter",
                                        "targetPlan", Map.of("targetSummary", "definitions=file,artifacts=file"))))
                        .withLifecycleReport(HermesLearningLifecycleReport.fromStages(
                                HermesLearningStageReport.completed(
                                        HermesLearningStageCatalog.PROMOTION_RECEIPT,
                                        "receipt recorded",
                                        Map.of("persisted", true)))));
        HermesRuntimeEvent failed = HermesRuntimeEvent.skillLearningFailed(
                request,
                response,
                new IllegalStateException("artifact store unavailable"));

        assertThat(completed.type()).isEqualTo(HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED);
        assertThat(completed.outcome()).isEqualTo("created");
        assertThat(completed.metadata())
                .containsEntry("decision", "created")
                .containsEntry("skillId", "hermes-release-workflow")
                .containsEntry("persisted", true)
                .containsEntry("revision", "1")
                .containsEntry("responseSuccessful", true);
        assertThat(metadataMap(completed.metadata(), HermesLearningMetadataKeys.RESULT))
                .containsKey(HermesLearningMetadataKeys.PROMOTION);
        assertThat(metadataMap(
                metadataMap(completed.metadata(), HermesLearningMetadataKeys.RESULT),
                HermesLearningMetadataKeys.PROMOTION))
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("planDecision", "created")
                .containsEntry("sourceRequestId", "req-learning")
                .containsEntry("revision", "1")
                .containsEntry("lineageRootSkillId", "hermes-release-workflow")
                .containsKey("promotionId")
                .containsKey("idempotencyKey");
        assertThat(metadataMap(
                metadataMap(completed.metadata(), HermesLearningMetadataKeys.RESULT),
                HermesLearningMetadataKeys.PROMOTION_RECEIPT))
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_PERSISTED)
                .containsEntry("persisted", true)
                .containsEntry("adapterId", "test-adapter")
                .containsEntry("targetSummary", "definitions=file,artifacts=file");
        assertThat(metadataMap(completed.metadata(), HermesLearningMetadataKeys.LIFECYCLE))
                .containsEntry(
                        HermesLearningMetadataKeys.TERMINAL_STAGE,
                        HermesLearningStageCatalog.PROMOTION_RECEIPT);
        assertThat(failed.type()).isEqualTo(HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED);
        assertThat(failed.outcome()).isEqualTo("failed");
        assertThat(failed.metadata())
                .containsEntry("errorType", IllegalStateException.class.getName())
                .containsEntry("error", "artifact store unavailable");
    }

    @Test
    void restoresEventFromMetadataEnvelope() {
        Instant observedAt = Instant.parse("2026-06-03T02:03:04Z");
        HermesRuntimeEvent event = HermesRuntimeEvent.fromMetadata(Map.of(
                "eventId", "event-a",
                "type", HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "requestId", "req-restore",
                "tenantId", "tenant-a",
                "sessionId", "session-a",
                "userId", "user-a",
                "outcome", "failed",
                "occurredAt", observedAt.toString(),
                "metadata", Map.of("durationMs", 42L)));

        assertThat(event.eventId()).isEqualTo("event-a");
        assertThat(event.type()).isEqualTo(HermesRuntimeEvent.TYPE_RESPONSE_FAILED);
        assertThat(event.requestId()).isEqualTo("req-restore");
        assertThat(event.tenantId()).isEqualTo("tenant-a");
        assertThat(event.outcome()).isEqualTo("failed");
        assertThat(event.occurredAt()).isEqualTo(observedAt);
        assertThat(event.metadata()).containsEntry("durationMs", 42L);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
