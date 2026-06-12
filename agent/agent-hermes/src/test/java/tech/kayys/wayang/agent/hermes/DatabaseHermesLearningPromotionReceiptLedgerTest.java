package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseHermesLearningPromotionReceiptLedgerTest {

    @Test
    void persistsFindsAndPrunesPromotionReceipts() {
        InMemoryPromotionReceiptDataSource dataSource = new InMemoryPromotionReceiptDataSource();
        DatabaseHermesLearningPromotionReceiptLedger ledger =
                new DatabaseHermesLearningPromotionReceiptLedger(
                        dataSource,
                        "hermes_promotion_receipts",
                        true,
                        2);

        ledger.record(receipt("database-key-001", "skill-1"));
        HermesLearningPromotionReceipt second = ledger.record(receipt("database-key-002", "skill-2"));
        HermesLearningPromotionReceipt third = ledger.record(receipt("database-key-003", "skill-3"));

        assertThat(ledger.tableName()).isEqualTo("hermes_promotion_receipts");
        assertThat(ledger.maxRecords()).isEqualTo(2);
        assertThat(ledger.recordCount()).isEqualTo(2);
        assertThat(ledger.find("database-key-001")).isEmpty();
        assertThat(ledger.find("database-key-002")).contains(second);
        assertThat(ledger.find("database-key-003")).contains(third);
        assertThat(ledger.query(HermesLearningPromotionReceiptQuery.forSkill("skill-3", 10)).receipts())
                .containsExactly(third);
        assertThat(ledger.latest(1).receipts())
                .containsExactly(third);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "hermes_promotion_receipts")
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
    void replaysExistingReceiptWithoutInsertingDuplicateRow() {
        InMemoryPromotionReceiptDataSource dataSource = new InMemoryPromotionReceiptDataSource();
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.database(
                dataSource,
                "hermes_promotion_receipts",
                true,
                10);
        HermesLearningPromotionReceipt first = receipt("same-database-key", "skill-a");
        HermesLearningPromotionReceipt duplicate = receipt("same-database-key", "skill-b");

        HermesLearningPromotionReceipt recorded = ledger.record(first);
        HermesLearningPromotionReceipt replayed = ledger.record(duplicate);

        assertThat(recorded).isEqualTo(first);
        assertThat(replayed).isEqualTo(first);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find("same-database-key")).contains(first);
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
                "database",
                "definitions=database,artifacts=database",
                Map.of("adapterId", "database"));
    }
}
