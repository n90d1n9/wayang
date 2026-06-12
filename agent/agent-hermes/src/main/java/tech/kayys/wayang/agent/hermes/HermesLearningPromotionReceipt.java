package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator-facing receipt for the outcome of a learned-skill promotion.
 */
public record HermesLearningPromotionReceipt(
        String promotionId,
        String idempotencyKey,
        String status,
        String outcome,
        String skillId,
        boolean persisted,
        String reason,
        String adapterId,
        String targetSummary,
        Map<String, Object> persistence) {

    public static final String OUTCOME_PERSISTED = "persisted";
    public static final String OUTCOME_REJECTED = "rejected";
    public static final String OUTCOME_SKIPPED = "skipped";

    public HermesLearningPromotionReceipt {
        promotionId = HermesText.trimOr(promotionId, "");
        idempotencyKey = HermesText.trimOr(idempotencyKey, "");
        status = HermesText.oneLineOr(status, "");
        outcome = HermesText.oneLineOr(outcome, "");
        skillId = HermesText.trimOr(skillId, "");
        reason = HermesText.oneLineOr(reason, "");
        adapterId = HermesText.trimOr(adapterId, "");
        targetSummary = HermesText.oneLineOr(targetSummary, "");
        persistence = persistence == null ? Map.of() : Map.copyOf(persistence);
    }

    public static HermesLearningPromotionReceipt from(
            HermesLearningPromotion promotion,
            HermesLearningResult result,
            Map<String, Object> persistenceMetadata) {
        HermesLearningPromotion resolvedPromotion = promotion == null
                ? HermesLearningPromotion.skipped(HermesLearningPlan.skipped("promotion missing"))
                : promotion;
        HermesLearningResult resolvedResult = result == null
                ? HermesLearningResult.skipped(resolvedPromotion.reason())
                : result;
        Map<String, Object> persistence = persistenceMetadata == null ? Map.of() : Map.copyOf(persistenceMetadata);
        boolean persisted = resolvedPromotion.status().equals(HermesLearningPromotion.STATUS_APPROVED)
                && (resolvedResult.decision() == HermesLearningDecision.CREATED
                || resolvedResult.decision() == HermesLearningDecision.UPDATED);
        return new HermesLearningPromotionReceipt(
                resolvedPromotion.identity().promotionId(),
                resolvedPromotion.identity().idempotencyKey(),
                resolvedPromotion.status(),
                outcome(resolvedPromotion, persisted),
                resolvedPromotion.skillId().isBlank() ? resolvedResult.skillId() : resolvedPromotion.skillId(),
                persisted,
                resolvedResult.reason().isBlank() ? resolvedPromotion.reason() : resolvedResult.reason(),
                text(persistence.get("adapterId")),
                targetSummary(persistence.get("targetPlan")),
                persistence);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("promotionId", promotionId);
        values.put("idempotencyKey", idempotencyKey);
        values.put("status", status);
        values.put("outcome", outcome);
        values.put("skillId", skillId);
        values.put("persisted", persisted);
        values.put("reason", reason);
        values.put("adapterId", adapterId);
        values.put("targetSummary", targetSummary);
        values.put("persistence", persistence);
        return Map.copyOf(values);
    }

    private static String outcome(HermesLearningPromotion promotion, boolean persisted) {
        if (persisted) {
            return OUTCOME_PERSISTED;
        }
        if (HermesLearningPromotion.STATUS_REJECTED.equals(promotion.status())) {
            return OUTCOME_REJECTED;
        }
        return OUTCOME_SKIPPED;
    }

    @SuppressWarnings("unchecked")
    private static String targetSummary(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return "";
        }
        return text(((Map<String, Object>) map).get("targetSummary"));
    }

    private static String text(Object value) {
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }
}
