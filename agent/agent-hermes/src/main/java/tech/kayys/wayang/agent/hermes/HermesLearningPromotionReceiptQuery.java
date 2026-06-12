package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator-facing filter for inspecting learned-skill promotion receipts.
 */
public record HermesLearningPromotionReceiptQuery(
        String skillId,
        String status,
        String outcome,
        String idempotencyKey,
        boolean persistedOnly,
        String beforeReceiptId,
        String afterReceiptId,
        int limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1_000;

    public HermesLearningPromotionReceiptQuery(
            String skillId,
            String status,
            String outcome,
            String idempotencyKey,
            boolean persistedOnly,
            int limit) {
        this(skillId, status, outcome, idempotencyKey, persistedOnly, "", "", limit);
    }

    public HermesLearningPromotionReceiptQuery {
        skillId = HermesText.trimToEmpty(skillId);
        status = HermesText.oneLineOr(status, "");
        outcome = HermesText.oneLineOr(outcome, "");
        idempotencyKey = HermesLearningPromotionReceiptLedgerRecords.key(idempotencyKey);
        beforeReceiptId = HermesLearningPromotionReceiptLedgerRecords.key(beforeReceiptId);
        afterReceiptId = HermesLearningPromotionReceiptLedgerRecords.key(afterReceiptId);
        if (!beforeReceiptId.isBlank() && !afterReceiptId.isBlank()) {
            throw new IllegalArgumentException("beforeReceiptId and afterReceiptId cannot both be set");
        }
        limit = normalizeLimit(limit);
    }

    public static HermesLearningPromotionReceiptQuery recent() {
        return recent(DEFAULT_LIMIT);
    }

    public static HermesLearningPromotionReceiptQuery recent(int limit) {
        return new HermesLearningPromotionReceiptQuery("", "", "", "", false, limit);
    }

    public static HermesLearningPromotionReceiptQuery forSkill(String skillId, int limit) {
        return new HermesLearningPromotionReceiptQuery(skillId, "", "", "", false, limit);
    }

    public static HermesLearningPromotionReceiptQuery forStatus(String status, int limit) {
        return new HermesLearningPromotionReceiptQuery("", status, "", "", false, limit);
    }

    public static HermesLearningPromotionReceiptQuery forOutcome(String outcome, int limit) {
        return new HermesLearningPromotionReceiptQuery("", "", outcome, "", false, limit);
    }

    public static HermesLearningPromotionReceiptQuery persisted(int limit) {
        return new HermesLearningPromotionReceiptQuery("", "", "", "", true, limit);
    }

    public static HermesLearningPromotionReceiptQuery beforeReceipt(String receiptId, int limit) {
        return new HermesLearningPromotionReceiptQuery("", "", "", "", false, receiptId, "", limit);
    }

    public static HermesLearningPromotionReceiptQuery afterReceipt(String receiptId, int limit) {
        return new HermesLearningPromotionReceiptQuery("", "", "", "", false, "", receiptId, limit);
    }

    public HermesLearningPromotionReceiptQuery withCursors(
            String beforeReceiptId,
            String afterReceiptId) {
        return new HermesLearningPromotionReceiptQuery(
                skillId,
                status,
                outcome,
                idempotencyKey,
                persistedOnly,
                beforeReceiptId,
                afterReceiptId,
                limit);
    }

    public boolean matches(HermesLearningPromotionReceipt receipt) {
        if (receipt == null) {
            return false;
        }
        if (!skillId.isBlank() && !skillId.equals(receipt.skillId())) {
            return false;
        }
        if (!status.isBlank() && !status.equals(receipt.status())) {
            return false;
        }
        if (!outcome.isBlank() && !outcome.equals(receipt.outcome())) {
            return false;
        }
        if (!idempotencyKey.isBlank() && !idempotencyKey.equals(receipt.idempotencyKey())) {
            return false;
        }
        return !persistedOnly || receipt.persisted();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("skillId", skillId);
        values.put("status", status);
        values.put("outcome", outcome);
        values.put("idempotencyKey", idempotencyKey);
        values.put("persistedOnly", persistedOnly);
        values.put("beforeReceiptId", beforeReceiptId);
        values.put("afterReceiptId", afterReceiptId);
        values.put("limit", limit);
        return Map.copyOf(values);
    }

    private static int normalizeLimit(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }
}
