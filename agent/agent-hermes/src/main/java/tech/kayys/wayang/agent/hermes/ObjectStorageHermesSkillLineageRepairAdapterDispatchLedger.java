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
 * Object-store-backed idempotency ledger for cloud and S3-compatible storage.
 */
public final class ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger
        implements HermesSkillLineageRepairAdapterDispatchLedger {

    public static final String DEFAULT_PREFIX = "hermes/repair-adapter-dispatch-ledger";

    private final ObjectStorageService objectStorageService;
    private final String prefix;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger(ObjectStorageService objectStorageService) {
        this(
                objectStorageService,
                DEFAULT_PREFIX,
                FileSystemHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_MAX_RECORDS);
    }

    public ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger(
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
    public synchronized Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey) {
        String key = HermesSkillLineageRepairAdapterDispatchLedgerRecords.key(idempotencyKey);
        if (key.isBlank()) {
            return Optional.empty();
        }
        return readRecord(objectKey(key))
                .filter(record -> key.equals(HermesSkillLineageRepairAdapterDispatchLedgerRecords.text(
                        record.get("idempotencyKey"))))
                .map(record -> HermesSkillLineageRepairAdapterDispatchLedgerRecords.dispatch(
                        record.get("dispatch")));
    }

    @Override
    public synchronized HermesSkillLineageRepairAdapterDispatch record(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairAdapterDispatch dispatch) {
        if (request == null || dispatch == null || request.idempotencyKey().isBlank()) {
            return dispatch;
        }
        Optional<HermesSkillLineageRepairAdapterDispatch> existing = find(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }
        put(
                objectKey(request.idempotencyKey()),
                HermesRuntimeEventJsonCodec.toJsonLine(
                                HermesSkillLineageRepairAdapterDispatchLedgerRecords.recordMetadata(
                                        request,
                                        dispatch))
                        .getBytes(StandardCharsets.UTF_8));
        pruneToCapacity();
        return dispatch;
    }

    @Override
    public synchronized int recordCount() {
        return recordKeys().size();
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
                        HermesSkillLineageRepairAdapterDispatchLedgerRecords.key(idempotencyKey)));
    }

    private record StoredRecord(String key, Map<String, Object> record) {

        String recordedAt() {
            return HermesSkillLineageRepairAdapterDispatchLedgerRecords.text(record.get("recordedAt"));
        }
    }
}
