package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HybridHermesLearningPromotionReceiptLedgerTest {

    @Test
    void recordsReceiptToPrimaryAndFallback(@TempDir Path tempDir) {
        HermesLearningPromotionReceiptLedger primary = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceiptLedger fallback =
                HermesLearningPromotionReceiptLedger.fileSystem(tempDir.resolve("promotion-receipts.jsonl"));
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedger.hybrid(primary, fallback);
        HermesLearningPromotionReceipt receipt = receipt("key-hybrid", "skill-hybrid");

        HermesLearningPromotionReceipt recorded = ledger.record(receipt);

        assertThat(recorded).isEqualTo(receipt);
        assertThat(primary.find("key-hybrid")).contains(receipt);
        assertThat(fallback.find("key-hybrid")).contains(receipt);
        assertThat(ledger.query(HermesLearningPromotionReceiptQuery.forSkill("skill-hybrid", 10)).receipts())
                .containsExactly(receipt);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "hybrid")
                .containsEntry("recordCount", 1)
                .containsEntry("replaySupported", true)
                .containsKeys("primaryLedger", "fallbackLedger");
    }

    @Test
    void fallsBackWhenPrimaryFails() {
        HermesLearningPromotionReceiptLedger fallback = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.hybrid(
                new FailingLedger(),
                fallback);
        HermesLearningPromotionReceipt receipt = receipt("key-fallback", "skill-fallback");

        HermesLearningPromotionReceipt recorded = ledger.record(receipt);

        assertThat(recorded).isEqualTo(receipt);
        assertThat(fallback.find("key-fallback")).contains(receipt);
        assertThat(ledger.find("key-fallback")).contains(receipt);
        assertThat(ledger.query(HermesLearningPromotionReceiptQuery.forSkill("skill-fallback", 10)).receipts())
                .containsExactly(receipt);
        assertThat(ledger.recordCount()).isEqualTo(1);
    }

    @Test
    void replaysExistingFallbackReceiptBeforeRecording() {
        HermesLearningPromotionReceiptLedger primary = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceiptLedger fallback = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt existing = receipt("key-existing", "skill-existing");
        HermesLearningPromotionReceipt duplicate = receipt("key-existing", "skill-duplicate");
        fallback.record(existing);
        HermesLearningPromotionReceiptLedger ledger = new HybridHermesLearningPromotionReceiptLedger(
                primary,
                fallback);

        HermesLearningPromotionReceipt replayed = ledger.record(duplicate);

        assertThat(replayed).isEqualTo(existing);
        assertThat(primary.find("key-existing")).isEmpty();
        assertThat(fallback.find("key-existing")).contains(existing);
        assertThat(ledger.recordCount()).isEqualTo(1);
    }

    @Test
    void queriesPrimaryAndFallbackReceipts() {
        HermesLearningPromotionReceiptLedger primary = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceiptLedger fallback = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt primaryReceipt = receipt("key-primary", "skill-primary");
        HermesLearningPromotionReceipt fallbackReceipt = receipt("key-fallback-query", "skill-fallback-query");
        primary.record(primaryReceipt);
        fallback.record(fallbackReceipt);
        HermesLearningPromotionReceiptLedger ledger = new HybridHermesLearningPromotionReceiptLedger(
                primary,
                fallback);

        HermesLearningPromotionReceiptPage page = ledger.query(HermesLearningPromotionReceiptQuery.recent(10));

        assertThat(page.receipts())
                .extracting(HermesLearningPromotionReceipt::skillId)
                .containsExactlyInAnyOrder("skill-primary", "skill-fallback-query");
        assertThat(page.matchedReceipts()).isEqualTo(2);
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
                "hybrid",
                "definitions=hybrid,artifacts=file-system",
                Map.of("adapterId", "hybrid"));
    }

    private static final class FailingLedger implements HermesLearningPromotionReceiptLedger {

        @Override
        public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
            throw new IllegalStateException("primary unavailable");
        }

        @Override
        public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
            throw new IllegalStateException("primary unavailable");
        }

        @Override
        public int recordCount() {
            throw new IllegalStateException("primary unavailable");
        }

        @Override
        public Map<String, Object> toMetadata() {
            return Map.of("ledgerType", "failing");
        }
    }
}
