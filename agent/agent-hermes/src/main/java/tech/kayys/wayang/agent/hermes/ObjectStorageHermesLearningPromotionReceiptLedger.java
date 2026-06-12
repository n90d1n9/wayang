package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed idempotency ledger for cloud and S3-compatible receipt storage.
 */
public final class ObjectStorageHermesLearningPromotionReceiptLedger
        implements HermesLearningPromotionReceiptLedger {

    public static final String DEFAULT_PREFIX = "hermes/learning-promotion-receipts";

    private final ObjectStorageService objectStorageService;
    private final String prefix;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public ObjectStorageHermesLearningPromotionReceiptLedger(ObjectStorageService objectStorageService) {
        this(
                objectStorageService,
                DEFAULT_PREFIX,
                FileSystemHermesLearningPromotionReceiptLedger.DEFAULT_MAX_RECORDS);
    }

    public ObjectStorageHermesLearningPromotionReceiptLedger(
            ObjectStorageService objectStorageService,
            String prefix,
            int maxRecords) {
        this.objectStorageService = Objects.requireNonNull(objectStorageService, "objectStorageService");
        this.prefix = HermesObjectStorageLayout.normalizePrefix(prefix, DEFAULT_PREFIX);
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxRecords);
    }

    public String prefix() {
        return prefix;
    }

    public int maxRecords() {
        return retentionPolicy.maxEntries();
    }

    @Override
    public synchronized Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
        String key = HermesLearningPromotionReceiptLedgerRecords.key(idempotencyKey);
        if (key.isBlank()) {
            return Optional.empty();
        }
        return readRecord(objectKey(key))
                .filter(record -> key.equals(HermesLearningPromotionReceiptLedgerRecords.text(
                        record.get("idempotencyKey"))))
                .map(record -> HermesLearningPromotionReceiptLedgerRecords.receipt(record.get("receipt")));
    }

    @Override
    public synchronized HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
        if (receipt == null || receipt.idempotencyKey().isBlank()) {
            return receipt;
        }
        Optional<HermesLearningPromotionReceipt> existing = find(receipt.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }
        put(
                objectKey(receipt.idempotencyKey()),
                HermesRuntimeEventJsonCodec.toJsonLine(
                                HermesLearningPromotionReceiptLedgerRecords.recordMetadata(receipt))
                        .getBytes(StandardCharsets.UTF_8));
        pruneToCapacity();
        return receipt;
    }

    @Override
    public synchronized int recordCount() {
        return recordKeys().size();
    }

    @Override
    public synchronized HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        return HermesLearningPromotionReceiptPage.fromEntries(entries(), query);
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerType", "object-storage");
        values.put("ledgerPrefix", prefix);
        values.put("recordCount", recordCount());
        values.put("maxRecords", maxRecords());
        values.put("retentionPolicy", retentionPolicy.toMetadata());
        values.put("replaySupported", true);
        return Map.copyOf(values);
    }

    public synchronized List<Map<String, Object>> records() {
        return storedRecords().stream()
                .map(StoredRecord::record)
                .toList();
    }

    public synchronized List<HermesLearningPromotionReceiptLedgerEntry> entries() {
        return records().stream()
                .map(HermesLearningPromotionReceiptLedgerEntry::fromRecord)
                .flatMap(Optional::stream)
                .toList();
    }

    private List<String> recordKeys() {
        return HermesObjectStorageLayout.listKeys(objectStorageService, prefix, ".jsonl");
    }

    private List<StoredRecord> storedRecords() {
        return recordKeys().stream()
                .map(key -> readRecord(key).map(record -> new StoredRecord(key, record)))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Map<String, Object>> readRecord(String key) {
        Optional<byte[]> content = HermesObjectStorageLayout.read(objectStorageService, key);
        if (content.isEmpty()) {
            return Optional.empty();
        }
        String line = new String(content.orElseThrow(), StandardCharsets.UTF_8).trim();
        if (line.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(HermesRuntimeEventJsonCodec.objectFromJsonLine(line));
    }

    private void pruneToCapacity() {
        retentionPolicy.staleFromOldestFirst(
                        storedRecords(),
                        Comparator
                                .comparing(StoredRecord::recordedAt)
                                .thenComparing(StoredRecord::key))
                .forEach(record -> HermesObjectStorageLayout.delete(objectStorageService, record.key()));
    }

    private void put(String key, byte[] content) {
        HermesObjectStorageLayout.put(objectStorageService, key, content);
    }

    private String objectKey(String idempotencyKey) {
        return HermesObjectStorageLayout.jsonlKey(
                prefix,
                HermesObjectStorageLayout.hashedObjectId(
                        HermesLearningPromotionReceiptLedgerRecords.key(idempotencyKey)));
    }

    private record StoredRecord(String key, Map<String, Object> record) {

        String recordedAt() {
            return HermesLearningPromotionReceiptLedgerRecords.text(record.get("recordedAt"));
        }
    }
}
