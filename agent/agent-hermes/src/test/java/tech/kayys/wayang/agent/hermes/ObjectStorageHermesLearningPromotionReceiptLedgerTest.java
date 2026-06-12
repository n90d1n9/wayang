package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageHermesLearningPromotionReceiptLedgerTest {

    @Test
    void persistsFindsAndPrunesPromotionReceipts() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        ObjectStorageHermesLearningPromotionReceiptLedger ledger =
                new ObjectStorageHermesLearningPromotionReceiptLedger(
                        storage,
                        "/tenant-a/hermes/promotion-receipts",
                        2);

        ledger.record(receipt("object-key-001", "skill-1"));
        HermesLearningPromotionReceipt second = ledger.record(receipt("object-key-002", "skill-2"));
        HermesLearningPromotionReceipt third = ledger.record(receipt("object-key-003", "skill-3"));

        assertThat(storage.objects)
                .hasSize(2)
                .allSatisfy((key, value) -> assertThat(key)
                        .startsWith("tenant-a/hermes/promotion-receipts/")
                        .endsWith(".jsonl"));
        assertThat(ledger.find("object-key-001")).isEmpty();
        assertThat(ledger.find("object-key-002")).contains(second);
        assertThat(ledger.find("object-key-003")).contains(third);
        assertThat(ledger.query(HermesLearningPromotionReceiptQuery.forSkill("skill-3", 10)).receipts())
                .containsExactly(third);
        assertThat(ledger.latest(1).receipts())
                .containsExactly(third);
        assertThat(ledger.records())
                .extracting(record -> record.get("recordType"))
                .containsOnly(HermesLearningPromotionReceiptLedgerRecords.RECORD_TYPE);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "tenant-a/hermes/promotion-receipts/")
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
    void replaysExistingReceiptWithoutWritingDuplicateObject() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.objectStorage(
                storage,
                "tenant-a/hermes/promotion-receipts",
                10);
        HermesLearningPromotionReceipt first = receipt("same-object-key", "skill-a");
        HermesLearningPromotionReceipt duplicate = receipt("same-object-key", "skill-b");

        HermesLearningPromotionReceipt recorded = ledger.record(first);
        HermesLearningPromotionReceipt replayed = ledger.record(duplicate);

        assertThat(recorded).isEqualTo(first);
        assertThat(replayed).isEqualTo(first);
        assertThat(storage.objects).hasSize(1);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find("same-object-key")).contains(first);
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
                "object-storage",
                "definitions=object-storage,artifacts=object-storage",
                Map.of("adapterId", "object-storage"));
    }
}
