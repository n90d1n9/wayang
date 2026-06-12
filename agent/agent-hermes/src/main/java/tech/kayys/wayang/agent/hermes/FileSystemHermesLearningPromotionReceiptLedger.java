package tech.kayys.wayang.agent.hermes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JSONL file-backed idempotency ledger for learned-skill promotion receipts.
 */
public final class FileSystemHermesLearningPromotionReceiptLedger
        implements HermesLearningPromotionReceiptLedger {

    public static final int DEFAULT_MAX_RECORDS = HermesRecordRetentionPolicy.DEFAULT_MAX_ENTRIES;

    private final Path ledgerPath;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public FileSystemHermesLearningPromotionReceiptLedger(Path ledgerPath) {
        this(ledgerPath, DEFAULT_MAX_RECORDS);
    }

    public FileSystemHermesLearningPromotionReceiptLedger(Path ledgerPath, int maxRecords) {
        this.ledgerPath = Objects.requireNonNull(ledgerPath, "ledgerPath");
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxRecords);
    }

    public Path ledgerPath() {
        return ledgerPath;
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
        List<Map<String, Object>> records = records();
        for (int index = records.size() - 1; index >= 0; index--) {
            Map<String, Object> record = records.get(index);
            if (key.equals(HermesLearningPromotionReceiptLedgerRecords.text(record.get("idempotencyKey")))) {
                return Optional.of(HermesLearningPromotionReceiptLedgerRecords.receipt(record.get("receipt")));
            }
        }
        return Optional.empty();
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
        try {
            Path parent = ledgerPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    ledgerPath,
                    HermesRuntimeEventJsonCodec.toJsonLine(
                                    HermesLearningPromotionReceiptLedgerRecords.recordMetadata(receipt))
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            pruneToCapacity();
            return receipt;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to persist Hermes learning promotion receipt ledger", error);
        }
    }

    @Override
    public synchronized int recordCount() {
        return records().size();
    }

    @Override
    public synchronized HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        return HermesLearningPromotionReceiptPage.fromEntries(
                records().stream()
                        .map(HermesLearningPromotionReceiptLedgerEntry::fromRecord)
                        .flatMap(Optional::stream)
                        .toList(),
                query);
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerType", "file-system");
        values.put("ledgerPath", ledgerPath.toString());
        values.put("recordCount", recordCount());
        values.put("maxRecords", maxRecords());
        values.put("retentionPolicy", retentionPolicy.toMetadata());
        values.put("replaySupported", true);
        return Map.copyOf(values);
    }

    public synchronized List<Map<String, Object>> records() {
        if (!Files.exists(ledgerPath)) {
            return List.of();
        }
        try {
            return Files.readAllLines(ledgerPath, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(HermesRuntimeEventJsonCodec::objectFromJsonLine)
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to read Hermes learning promotion receipt ledger", error);
        }
    }

    private void pruneToCapacity() throws IOException {
        List<String> lines = Files.readAllLines(ledgerPath, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (retentionPolicy.allowsAll(lines.size())) {
            return;
        }
        Files.write(
                ledgerPath,
                retentionPolicy.retainNewestFromAppendOrder(lines),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}
