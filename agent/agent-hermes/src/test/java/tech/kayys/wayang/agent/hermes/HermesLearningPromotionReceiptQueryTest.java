package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesLearningPromotionReceiptQueryTest {

    @Test
    void filtersReceiptsByPromotionFields() {
        HermesLearningPromotionReceipt receipt = receipt("key-1", "skill-a", true);
        HermesLearningPromotionReceiptQuery query = new HermesLearningPromotionReceiptQuery(
                " skill-a ",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                " key-1 ",
                true,
                2_000);

        assertThat(query.limit()).isEqualTo(HermesLearningPromotionReceiptQuery.MAX_LIMIT);
        assertThat(query.matches(receipt)).isTrue();
        assertThat(query.matches(receipt("key-2", "skill-a", true))).isFalse();
        assertThat(query.matches(receipt("key-1", "skill-b", true))).isFalse();
        assertThat(query.matches(receipt("key-1", "skill-a", false))).isFalse();
        assertThat(query.toMetadata())
                .containsEntry("skillId", "skill-a")
                .containsEntry("persistedOnly", true)
                .containsEntry("limit", HermesLearningPromotionReceiptQuery.MAX_LIMIT);
    }

    @Test
    void pagesRecentReceiptsAndMarksTruncation() {
        HermesLearningPromotionReceipt older = receipt("key-old", "skill-old", true);
        HermesLearningPromotionReceipt newer = receipt("key-new", "skill-new", true);

        HermesLearningPromotionReceiptPage page = HermesLearningPromotionReceiptPage.fromEntries(
                List.of(
                        new HermesLearningPromotionReceiptLedgerEntry("2026-01-01T00:00:00Z", older),
                        new HermesLearningPromotionReceiptLedgerEntry("2026-01-02T00:00:00Z", newer)),
                HermesLearningPromotionReceiptQuery.recent(1));

        assertThat(page.receipts()).containsExactly(newer);
        assertThat(page.matchedReceipts()).isEqualTo(2);
        assertThat(page.returnedReceipts()).isEqualTo(1);
        assertThat(page.truncated()).isTrue();
        assertThat(page.toMetadata())
                .containsEntry("matchedReceipts", 2)
                .containsEntry("returnedReceipts", 1)
                .containsEntry("truncated", true);
    }

    @Test
    void paginatesReceiptsWithStableAuditCursors() {
        List<HermesLearningPromotionReceiptLedgerEntry> entries = List.of(
                entry("2026-01-04T00:00:00Z", "key-4", "skill-4"),
                entry("2026-01-01T00:00:00Z", "key-1", "skill-1"),
                entry("2026-01-03T00:00:00Z", "key-3", "skill-3"),
                entry("2026-01-02T00:00:00Z", "key-2", "skill-2"));

        HermesLearningPromotionReceiptPage latest = HermesLearningPromotionReceiptPage.fromEntries(
                entries,
                HermesLearningPromotionReceiptQuery.recent(2));
        HermesLearningPromotionReceiptPage older = HermesLearningPromotionReceiptPage.fromEntries(
                entries,
                HermesLearningPromotionReceiptQuery.afterReceipt("key-3", 1));
        HermesLearningPromotionReceiptPage newer = HermesLearningPromotionReceiptPage.fromEntries(
                entries,
                HermesLearningPromotionReceiptQuery.beforeReceipt("key-2", 1));
        HermesLearningPromotionReceiptPage missing = HermesLearningPromotionReceiptPage.fromEntries(
                entries,
                HermesLearningPromotionReceiptQuery.beforeReceipt("missing", 2));

        assertThat(latest.receipts())
                .extracting(HermesLearningPromotionReceipt::idempotencyKey)
                .containsExactly("key-4", "key-3");
        assertThat(latest)
                .extracting(
                        HermesLearningPromotionReceiptPage::matchedReceipts,
                        HermesLearningPromotionReceiptPage::totalMatchedReceipts,
                        HermesLearningPromotionReceiptPage::previousCursor,
                        HermesLearningPromotionReceiptPage::nextCursor,
                        HermesLearningPromotionReceiptPage::hasPreviousPage,
                        HermesLearningPromotionReceiptPage::hasNextPage,
                        HermesLearningPromotionReceiptPage::cursorResolved)
                .containsExactly(4, 4, "", "key-3", false, true, true);
        assertThat(older.receipts())
                .extracting(HermesLearningPromotionReceipt::idempotencyKey)
                .containsExactly("key-2");
        assertThat(older)
                .extracting(
                        HermesLearningPromotionReceiptPage::matchedReceipts,
                        HermesLearningPromotionReceiptPage::totalMatchedReceipts,
                        HermesLearningPromotionReceiptPage::previousCursor,
                        HermesLearningPromotionReceiptPage::nextCursor,
                        HermesLearningPromotionReceiptPage::hasPreviousPage,
                        HermesLearningPromotionReceiptPage::hasNextPage)
                .containsExactly(2, 4, "key-2", "key-2", true, true);
        assertThat(newer.receipts())
                .extracting(HermesLearningPromotionReceipt::idempotencyKey)
                .containsExactly("key-3");
        assertThat(newer)
                .extracting(
                        HermesLearningPromotionReceiptPage::matchedReceipts,
                        HermesLearningPromotionReceiptPage::totalMatchedReceipts,
                        HermesLearningPromotionReceiptPage::previousCursor,
                        HermesLearningPromotionReceiptPage::nextCursor,
                        HermesLearningPromotionReceiptPage::hasPreviousPage,
                        HermesLearningPromotionReceiptPage::hasNextPage)
                .containsExactly(2, 4, "key-3", "key-3", true, true);
        assertThat(older.toMetadata())
                .containsEntry("previousCursor", "key-2")
                .containsEntry("nextCursor", "key-2")
                .containsEntry("totalMatchedReceipts", 4)
                .containsEntry("cursorResolved", true);
        assertThat(missing.receipts()).isEmpty();
        assertThat(missing.toMetadata())
                .containsEntry("totalMatchedReceipts", 4)
                .containsEntry("cursorResolved", false);
        assertThatThrownBy(() -> new HermesLearningPromotionReceiptQuery(
                        "",
                        "",
                        "",
                        "",
                        false,
                        "key-1",
                        "key-2",
                        10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot both be set");
    }

    @Test
    void parsesLedgerRecordsIntoAuditEntries() {
        HermesLearningPromotionReceipt receipt = receipt("key-record", "skill-record", true);
        Map<String, Object> record = HermesLearningPromotionReceiptLedgerRecords.recordMetadata(receipt);

        assertThat(HermesLearningPromotionReceiptLedgerEntry.fromRecord(record))
                .hasValueSatisfying(entry -> assertThat(entry.toMetadata())
                        .containsEntry("recordType", HermesLearningPromotionReceiptLedgerRecords.RECORD_TYPE)
                        .containsEntry("idempotencyKey", "key-record")
                        .containsEntry("skillId", "skill-record"));
    }

    private static HermesLearningPromotionReceipt receipt(
            String idempotencyKey,
            String skillId,
            boolean persisted) {
        return new HermesLearningPromotionReceipt(
                "promotion-" + skillId,
                idempotencyKey,
                persisted ? HermesLearningPromotion.STATUS_APPROVED : HermesLearningPromotion.STATUS_SKIPPED,
                persisted
                        ? HermesLearningPromotionReceipt.OUTCOME_PERSISTED
                        : HermesLearningPromotionReceipt.OUTCOME_SKIPPED,
                skillId,
                persisted,
                persisted ? "persisted" : "skipped",
                "test",
                "definitions=test,artifacts=test",
                Map.of("adapterId", "test"));
    }

    private static HermesLearningPromotionReceiptLedgerEntry entry(
            String recordedAt,
            String idempotencyKey,
            String skillId) {
        return new HermesLearningPromotionReceiptLedgerEntry(recordedAt, receipt(idempotencyKey, skillId, true));
    }
}
