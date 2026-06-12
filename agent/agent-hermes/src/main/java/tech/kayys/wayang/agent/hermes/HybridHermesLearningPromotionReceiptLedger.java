package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Primary/fallback idempotency ledger for cloud-first promotion receipt replay.
 */
public final class HybridHermesLearningPromotionReceiptLedger
        implements HermesLearningPromotionReceiptLedger {

    private final HermesLearningPromotionReceiptLedger primary;
    private final HermesLearningPromotionReceiptLedger fallback;

    public HybridHermesLearningPromotionReceiptLedger(
            HermesLearningPromotionReceiptLedger primary,
            HermesLearningPromotionReceiptLedger fallback) {
        this.primary = primary == null ? HermesLearningPromotionReceiptLedger.noop() : primary;
        this.fallback = fallback == null ? HermesLearningPromotionReceiptLedger.noop() : fallback;
    }

    @Override
    public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
        Optional<HermesLearningPromotionReceipt> primaryReceipt = findSafely(primary, idempotencyKey);
        return primaryReceipt.isPresent() ? primaryReceipt : findSafely(fallback, idempotencyKey);
    }

    @Override
    public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
        if (receipt == null || receipt.idempotencyKey().isBlank()) {
            return receipt;
        }
        Optional<HermesLearningPromotionReceipt> existing = find(receipt.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        HermesLearningPromotionReceipt recorded = null;
        RuntimeException primaryError = null;
        try {
            recorded = primary.record(receipt);
        } catch (RuntimeException error) {
            primaryError = error;
        }

        try {
            HermesLearningPromotionReceipt fallbackReceipt =
                    fallback.record(recorded == null ? receipt : recorded);
            return recorded == null ? fallbackReceipt : recorded;
        } catch (RuntimeException fallbackError) {
            if (recorded != null) {
                return recorded;
            }
            if (primaryError != null) {
                primaryError.addSuppressed(fallbackError);
                throw primaryError;
            }
            throw fallbackError;
        }
    }

    @Override
    public int recordCount() {
        return Math.max(recordCount(primary), recordCount(fallback));
    }

    @Override
    public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        Map<String, HermesLearningPromotionReceiptLedgerEntry> entries = new LinkedHashMap<>();
        addEntries(entries, querySafely(primary, query));
        addEntries(entries, querySafely(fallback, query));
        return HermesLearningPromotionReceiptPage.fromEntries(entries.values(), query);
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerType", "hybrid");
        values.put("recordCount", recordCount());
        values.put("replaySupported", true);
        values.put("primaryLedger", primary.toMetadata());
        values.put("fallbackLedger", fallback.toMetadata());
        return Map.copyOf(values);
    }

    private static void addEntries(
            Map<String, HermesLearningPromotionReceiptLedgerEntry> target,
            HermesLearningPromotionReceiptPage page) {
        for (HermesLearningPromotionReceiptLedgerEntry entry : page.entries()) {
            String key = HermesLearningPromotionReceiptLedgerRecords.key(entry.receipt().idempotencyKey());
            if (!key.isBlank()) {
                target.putIfAbsent(key, entry);
            }
        }
    }

    private static Optional<HermesLearningPromotionReceipt> findSafely(
            HermesLearningPromotionReceiptLedger ledger,
            String idempotencyKey) {
        try {
            return ledger.find(idempotencyKey);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static HermesLearningPromotionReceiptPage querySafely(
            HermesLearningPromotionReceiptLedger ledger,
            HermesLearningPromotionReceiptQuery query) {
        try {
            return ledger.query(query);
        } catch (RuntimeException ignored) {
            return HermesLearningPromotionReceiptPage.empty(query);
        }
    }

    private static int recordCount(HermesLearningPromotionReceiptLedger ledger) {
        try {
            return ledger.recordCount();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
