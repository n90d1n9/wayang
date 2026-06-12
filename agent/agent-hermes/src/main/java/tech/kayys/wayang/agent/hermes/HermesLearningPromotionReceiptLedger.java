package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency ledger for learned-skill promotion receipts.
 */
public interface HermesLearningPromotionReceiptLedger {

    Optional<HermesLearningPromotionReceipt> find(String idempotencyKey);

    HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt);

    default HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        return HermesLearningPromotionReceiptPage.empty(query);
    }

    default HermesLearningPromotionReceiptPage latest(int limit) {
        return query(HermesLearningPromotionReceiptQuery.recent(limit));
    }

    int recordCount();

    Map<String, Object> toMetadata();

    static HermesLearningPromotionReceiptLedger noop() {
        return NoopLedger.INSTANCE;
    }

    static HermesLearningPromotionReceiptLedger inMemory() {
        return new InMemoryLedger();
    }

    static HermesLearningPromotionReceiptLedger fileSystem(java.nio.file.Path ledgerPath) {
        return new FileSystemHermesLearningPromotionReceiptLedger(ledgerPath);
    }

    static HermesLearningPromotionReceiptLedger fileSystem(java.nio.file.Path ledgerPath, int maxRecords) {
        return new FileSystemHermesLearningPromotionReceiptLedger(ledgerPath, maxRecords);
    }

    static HermesLearningPromotionReceiptLedger objectStorage(
            tech.kayys.wayang.storage.spi.ObjectStorageService objectStorageService) {
        return new ObjectStorageHermesLearningPromotionReceiptLedger(objectStorageService);
    }

    static HermesLearningPromotionReceiptLedger objectStorage(
            tech.kayys.wayang.storage.spi.ObjectStorageService objectStorageService,
            String prefix,
            int maxRecords) {
        return new ObjectStorageHermesLearningPromotionReceiptLedger(objectStorageService, prefix, maxRecords);
    }

    static HermesLearningPromotionReceiptLedger database(javax.sql.DataSource dataSource) {
        return new DatabaseHermesLearningPromotionReceiptLedger(dataSource);
    }

    static HermesLearningPromotionReceiptLedger database(
            javax.sql.DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            int maxRecords) {
        return new DatabaseHermesLearningPromotionReceiptLedger(dataSource, tableName, initializeSchema, maxRecords);
    }

    static HermesLearningPromotionReceiptLedger hybrid(
            HermesLearningPromotionReceiptLedger primary,
            HermesLearningPromotionReceiptLedger fallback) {
        return new HybridHermesLearningPromotionReceiptLedger(primary, fallback);
    }

    private static String key(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? ""
                : HermesText.oneLine(idempotencyKey);
    }

    final class NoopLedger implements HermesLearningPromotionReceiptLedger {

        private static final NoopLedger INSTANCE = new NoopLedger();

        private NoopLedger() {
        }

        @Override
        public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
            return receipt;
        }

        @Override
        public int recordCount() {
            return 0;
        }

        @Override
        public Map<String, Object> toMetadata() {
            return Map.of(
                    "ledgerType", "noop",
                    "recordCount", 0,
                    "replaySupported", false);
        }
    }

    final class InMemoryLedger implements HermesLearningPromotionReceiptLedger {

        private final Map<String, HermesLearningPromotionReceiptLedgerEntry> receipts =
                new ConcurrentHashMap<>();

        @Override
        public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
            String key = key(idempotencyKey);
            return key.isBlank()
                    ? Optional.empty()
                    : Optional.ofNullable(receipts.get(key))
                            .map(HermesLearningPromotionReceiptLedgerEntry::receipt);
        }

        @Override
        public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
            if (receipt == null || receipt.idempotencyKey().isBlank()) {
                return receipt;
            }
            String key = key(receipt.idempotencyKey());
            receipts.putIfAbsent(key, HermesLearningPromotionReceiptLedgerEntry.recordedNow(receipt));
            return receipts.get(key).receipt();
        }

        @Override
        public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
            return HermesLearningPromotionReceiptPage.fromEntries(receipts.values(), query);
        }

        @Override
        public int recordCount() {
            return receipts.size();
        }

        @Override
        public Map<String, Object> toMetadata() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("ledgerType", "in-memory");
            values.put("recordCount", recordCount());
            values.put("replaySupported", true);
            return Map.copyOf(values);
        }
    }
}
