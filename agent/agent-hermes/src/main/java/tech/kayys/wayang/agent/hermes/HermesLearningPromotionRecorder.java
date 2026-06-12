package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Records operator-facing promotion metadata for a learning result.
 */
public final class HermesLearningPromotionRecorder {

    private final Supplier<Map<String, Object>> persistenceMetadata;
    private final HermesLearningPromotionReceiptLedger receiptLedger;

    public HermesLearningPromotionRecorder(HermesLearnedSkillRepository learnedSkills) {
        this(learnedSkills, HermesLearningPromotionReceiptLedger.noop());
    }

    public HermesLearningPromotionRecorder(
            HermesLearnedSkillRepository learnedSkills,
            HermesLearningPromotionReceiptLedger receiptLedger) {
        this(Objects.requireNonNull(learnedSkills, "learnedSkills")::persistenceMetadata, receiptLedger);
    }

    public HermesLearningPromotionRecorder(Supplier<Map<String, Object>> persistenceMetadata) {
        this(persistenceMetadata, HermesLearningPromotionReceiptLedger.noop());
    }

    public HermesLearningPromotionRecorder(
            Supplier<Map<String, Object>> persistenceMetadata,
            HermesLearningPromotionReceiptLedger receiptLedger) {
        this.persistenceMetadata = Objects.requireNonNull(persistenceMetadata, "persistenceMetadata");
        this.receiptLedger = receiptLedger == null ? HermesLearningPromotionReceiptLedger.noop() : receiptLedger;
    }

    public HermesLearningResult record(
            HermesLearningResult result,
            HermesLearningPromotion promotion) {
        HermesLearningPromotion resolvedPromotion = promotion == null
                ? HermesLearningPromotion.skipped(HermesLearningPlan.skipped("promotion missing"))
                : promotion;
        HermesLearningResult resolvedResult = result == null
                ? HermesLearningResult.skipped(resolvedPromotion.reason())
                : result;
        HermesLearningResult promoted = resolvedResult.withPromotion(resolvedPromotion);
        HermesLearningPromotionReceipt receipt = HermesLearningPromotionReceipt.from(
                resolvedPromotion,
                promoted,
                metadata());
        HermesLearningPromotionReceipt recordedReceipt = recordReceipt(receipt);
        HermesLearningLifecycleReport lifecycle = lifecycleReport(resolvedPromotion, recordedReceipt);
        return promoted
                .withPromotionReceipt(recordedReceipt)
                .withLifecycleReport(lifecycle)
                .withMetadata(Map.of(
                        HermesLearningMetadataKeys.PROMOTION_RECEIPT_LEDGER,
                        receiptLedger.toMetadata()));
    }

    private HermesLearningPromotionReceipt recordReceipt(HermesLearningPromotionReceipt receipt) {
        HermesLearningPromotionReceipt recorded = receiptLedger.record(receipt);
        return recorded == null ? receipt : recorded;
    }

    private Map<String, Object> metadata() {
        Map<String, Object> metadata = persistenceMetadata.get();
        return metadata == null ? Map.of() : metadata;
    }

    private static HermesLearningLifecycleReport lifecycleReport(
            HermesLearningPromotion promotion,
            HermesLearningPromotionReceipt receipt) {
        HermesLearningLifecycleReport planning = HermesLearningLifecycleReport.fromMetadata(
                promotion.metadata().get(HermesLearningMetadataKeys.PLANNING_LIFECYCLE));
        return planning
                .withStage(promotionStage(promotion))
                .withStage(persistenceStage(promotion, receipt))
                .withStage(receiptStage(receipt));
    }

    private static HermesLearningStageReport promotionStage(HermesLearningPromotion promotion) {
        return HermesLearningStageReport.completed(
                HermesLearningStageCatalog.PROMOTION_DECISION,
                promotion.reason(),
                Map.of(
                        "status", promotion.status(),
                        "decision", promotion.decision().name().toLowerCase(java.util.Locale.ROOT),
                        "promotionId", promotion.identity().promotionId(),
                        "idempotencyKey", promotion.identity().idempotencyKey()));
    }

    private static HermesLearningStageReport persistenceStage(
            HermesLearningPromotion promotion,
            HermesLearningPromotionReceipt receipt) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", receipt.status());
        metadata.put("outcome", receipt.outcome());
        metadata.put("persisted", receipt.persisted());
        metadata.put("targetSummary", receipt.targetSummary());
        metadata.put("adapterId", receipt.adapterId());
        if (receipt.persisted()) {
            return HermesLearningStageReport.completed(
                    HermesLearningStageCatalog.SKILL_PERSISTENCE,
                    receipt.reason(),
                    metadata);
        }
        String reason = receipt.reason().isBlank() ? promotion.reason() : receipt.reason();
        return HermesLearningStageReport.skipped(
                HermesLearningStageCatalog.SKILL_PERSISTENCE,
                reason).withMetadata(metadata);
    }

    private static HermesLearningStageReport receiptStage(HermesLearningPromotionReceipt receipt) {
        return HermesLearningStageReport.completed(
                HermesLearningStageCatalog.PROMOTION_RECEIPT,
                receipt.reason(),
                Map.of(
                        "promotionId", receipt.promotionId(),
                        "idempotencyKey", receipt.idempotencyKey(),
                        "outcome", receipt.outcome(),
                        "persisted", receipt.persisted()));
    }
}
