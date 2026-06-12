package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared JSON record shape for promotion receipt idempotency ledgers.
 */
final class HermesLearningPromotionReceiptLedgerRecords {

    static final String RECORD_TYPE = "hermes.learning.promotion-receipt";

    private HermesLearningPromotionReceiptLedgerRecords() {
    }

    static String key(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? ""
                : HermesText.oneLine(idempotencyKey);
    }

    static Map<String, Object> recordMetadata(HermesLearningPromotionReceipt receipt) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("recordType", RECORD_TYPE);
        values.put("recordedAt", Instant.now().toString());
        values.put("promotionId", receipt.promotionId());
        values.put("idempotencyKey", receipt.idempotencyKey());
        values.put("skillId", receipt.skillId());
        values.put("status", receipt.status());
        values.put("outcome", receipt.outcome());
        values.put("receipt", receipt.toMetadata());
        return Map.copyOf(values);
    }

    static HermesLearningPromotionReceipt receipt(Object value) {
        Map<String, Object> values = objectMap(value);
        return new HermesLearningPromotionReceipt(
                text(values.get("promotionId")),
                text(values.get("idempotencyKey")),
                text(values.get("status")),
                text(values.get("outcome")),
                text(values.get("skillId")),
                bool(values.get("persisted")),
                text(values.get("reason")),
                text(values.get("adapterId")),
                text(values.get("targetSummary")),
                objectMap(values.get("persistence")));
    }

    static String text(Object value) {
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }

    static boolean bool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        return Map.copyOf(values);
    }
}
