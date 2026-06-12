package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stored promotion receipt plus the ledger timestamp used for audit ordering.
 */
public record HermesLearningPromotionReceiptLedgerEntry(
        String recordedAt,
        HermesLearningPromotionReceipt receipt) {

    public HermesLearningPromotionReceiptLedgerEntry {
        recordedAt = HermesText.oneLineOr(recordedAt, "");
        receipt = Objects.requireNonNull(receipt, "receipt");
    }

    public static HermesLearningPromotionReceiptLedgerEntry recordedNow(
            HermesLearningPromotionReceipt receipt) {
        return new HermesLearningPromotionReceiptLedgerEntry(Instant.now().toString(), receipt);
    }

    public static Optional<HermesLearningPromotionReceiptLedgerEntry> fromRecord(
            Map<String, Object> record) {
        if (record == null || record.isEmpty()) {
            return Optional.empty();
        }
        String recordType = HermesLearningPromotionReceiptLedgerRecords.text(record.get("recordType"));
        if (!recordType.isBlank()
                && !HermesLearningPromotionReceiptLedgerRecords.RECORD_TYPE.equals(recordType)) {
            return Optional.empty();
        }
        HermesLearningPromotionReceipt receipt =
                HermesLearningPromotionReceiptLedgerRecords.receipt(record.get("receipt"));
        if (receipt.idempotencyKey().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new HermesLearningPromotionReceiptLedgerEntry(
                HermesLearningPromotionReceiptLedgerRecords.text(record.get("recordedAt")),
                receipt));
    }

    public boolean matches(HermesLearningPromotionReceiptQuery query) {
        HermesLearningPromotionReceiptQuery resolved =
                query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        return resolved.matches(receipt);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("recordType", HermesLearningPromotionReceiptLedgerRecords.RECORD_TYPE);
        values.put("recordedAt", recordedAt);
        values.put("promotionId", receipt.promotionId());
        values.put("idempotencyKey", receipt.idempotencyKey());
        values.put("skillId", receipt.skillId());
        values.put("status", receipt.status());
        values.put("outcome", receipt.outcome());
        values.put("receipt", receipt.toMetadata());
        return Map.copyOf(values);
    }
}
