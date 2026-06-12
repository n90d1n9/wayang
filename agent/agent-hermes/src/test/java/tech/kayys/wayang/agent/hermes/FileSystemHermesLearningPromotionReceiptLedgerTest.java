package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemHermesLearningPromotionReceiptLedgerTest {

    @Test
    void persistsFindsAndPrunesPromotionReceipts(@TempDir Path tempDir) throws Exception {
        Path ledgerPath = tempDir.resolve("learning/promotion-receipts.jsonl");
        FileSystemHermesLearningPromotionReceiptLedger ledger =
                new FileSystemHermesLearningPromotionReceiptLedger(ledgerPath, 2);

        ledger.record(receipt("key-1", "skill-1"));
        HermesLearningPromotionReceipt second = ledger.record(receipt("key-2", "skill-2"));
        HermesLearningPromotionReceipt third = ledger.record(receipt("key-3", "skill-3"));

        assertThat(Files.readAllLines(ledgerPath)).hasSize(2);
        assertThat(ledger.recordCount()).isEqualTo(2);
        assertThat(ledger.find("key-1")).isEmpty();
        assertThat(ledger.find("key-2")).contains(second);
        assertThat(ledger.find("key-3")).contains(third);
        assertThat(ledger.query(HermesLearningPromotionReceiptQuery.forSkill("skill-3", 10)).receipts())
                .containsExactly(third);
        assertThat(ledger.latest(1).receipts())
                .containsExactly(third);
        assertThat(ledger.records())
                .extracting(record -> record.get("recordType"))
                .containsOnly(HermesLearningPromotionReceiptLedgerRecords.RECORD_TYPE);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("recordCount", 2)
                .containsEntry("maxRecords", 2)
                .containsEntry("replaySupported", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> retentionPolicy =
                (Map<String, Object>) ledger.toMetadata().get("retentionPolicy");
        assertThat(retentionPolicy)
                .containsEntry("retentionMode", "max-entries")
                .containsEntry("maxEntries", 2);
    }

    @Test
    void replaysExistingReceiptWithoutAppendingDuplicate(@TempDir Path tempDir) throws Exception {
        Path ledgerPath = tempDir.resolve("learning/promotion-receipts.jsonl");
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.fileSystem(ledgerPath);
        HermesLearningPromotionReceipt first = receipt("same-key", "skill-a");
        HermesLearningPromotionReceipt duplicate = receipt("same-key", "skill-b");

        HermesLearningPromotionReceipt recorded = ledger.record(first);
        HermesLearningPromotionReceipt replayed = ledger.record(duplicate);

        assertThat(recorded).isEqualTo(first);
        assertThat(replayed).isEqualTo(first);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(Files.readAllLines(ledgerPath)).hasSize(1);
        assertThat(ledger.find("same-key")).contains(first);
    }

    private static HermesLearningPromotionReceipt receipt(String idempotencyKey, String skillId) {
        return new HermesLearningPromotionReceipt(
                "promotion-" + skillId,
                idempotencyKey,
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                skillId,
                true,
                "persisted",
                "file-system",
                "definitions=file-system,artifacts=file-system",
                Map.of("adapterId", "file-system"));
    }
}
