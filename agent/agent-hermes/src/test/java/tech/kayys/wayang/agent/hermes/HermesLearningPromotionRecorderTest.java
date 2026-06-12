package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningPromotionRecorderTest {

    @Test
    void recordsApprovedPromotionReceiptWithPersistenceMetadata() {
        SkillDefinition skill = skill("nightly-api-backup-report");
        HermesLearningPromotion promotion = HermesLearningPromotion.approved(HermesLearningPlan.create(skill));
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionRecorder recorder = new HermesLearningPromotionRecorder(
                () -> Map.of(
                        "adapterId", "database",
                        "targetPlan", Map.of("targetSummary", "definitions=database,artifacts=s3")),
                ledger);

        HermesLearningResult result = recorder.record(HermesLearningResult.created(skill), promotion);

        assertThat(result.metadataView().promotion())
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("skillId", skill.id())
                .containsEntry("plannedPersistence", true);
        assertThat(result.metadataView().promotionReceipt())
                .containsEntry("promotionId", promotion.identity().promotionId())
                .containsEntry("idempotencyKey", promotion.identity().idempotencyKey())
                .containsEntry("status", HermesLearningPromotion.STATUS_APPROVED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_PERSISTED)
                .containsEntry("skillId", skill.id())
                .containsEntry("persisted", true)
                .containsEntry("adapterId", "database")
                .containsEntry("targetSummary", "definitions=database,artifacts=s3");
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find(promotion.identity().idempotencyKey()))
                .isPresent()
                .get()
                .extracting(HermesLearningPromotionReceipt::adapterId)
                .isEqualTo("database");
        assertThat(result.metadataView().promotionReceiptLedger())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("recordCount", 1)
                .containsEntry("replaySupported", true);
        assertThat(result.metadataView().lifecycleReport().completedStages())
                .contains(
                        HermesLearningStageCatalog.PROMOTION_DECISION,
                        HermesLearningStageCatalog.SKILL_PERSISTENCE,
                        HermesLearningStageCatalog.PROMOTION_RECEIPT);
    }

    @Test
    void recordsSkippedReceiptWhenInputsAreMissing() {
        HermesLearningPromotionRecorder recorder = new HermesLearningPromotionRecorder(() -> null);

        HermesLearningResult result = recorder.record(null, null);

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.SKIPPED);
        assertThat(result.reason()).isEqualTo("promotion missing");
        assertThat(result.metadataView().promotion())
                .containsEntry("status", HermesLearningPromotion.STATUS_SKIPPED)
                .containsEntry("planReason", "promotion missing");
        assertThat(result.metadataView().promotionReceipt())
                .containsEntry("status", HermesLearningPromotion.STATUS_SKIPPED)
                .containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_SKIPPED)
                .containsEntry("persisted", false)
                .containsEntry("reason", "promotion missing");
    }

    @Test
    void inMemoryLedgerReplaysExistingReceiptByIdempotencyKey() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt first = receipt("learning-promotion-same-key", "persisted");
        HermesLearningPromotionReceipt duplicate = receipt("learning-promotion-same-key", "skipped");

        HermesLearningPromotionReceipt recorded = ledger.record(first);
        HermesLearningPromotionReceipt replayed = ledger.record(duplicate);

        assertThat(recorded).isEqualTo(first);
        assertThat(replayed).isEqualTo(first);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find("learning-promotion-same-key")).contains(first);
    }

    private static SkillDefinition skill(String id) {
        return SkillDefinition.builder()
                .id(id)
                .name("Nightly API Backup Report")
                .description("Generate the nightly API backup report")
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Generate the report from validated backup signals.")
                .metadata(Map.of(
                        "hermes.requestId", "req-recorder",
                        "hermes.revision", "1",
                        "hermes.lineageRootSkillId", id))
                .build();
    }

    private static HermesLearningPromotionReceipt receipt(String idempotencyKey, String outcome) {
        return new HermesLearningPromotionReceipt(
                "promotion-test",
                idempotencyKey,
                HermesLearningPromotion.STATUS_APPROVED,
                outcome,
                "nightly-api-backup-report",
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED.equals(outcome),
                "",
                "test",
                "definitions=test,artifacts=test",
                Map.of());
    }

}
