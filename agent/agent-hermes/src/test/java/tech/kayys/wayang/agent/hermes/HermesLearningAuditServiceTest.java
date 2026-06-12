package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditServiceTest {

    @Test
    void summarizesRecentLearningPromotionReceipts() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt persisted = receipt(
                "key-persisted",
                "promotion-persisted",
                "skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "database");
        HermesLearningPromotionReceipt skipped = receipt(
                "key-skipped",
                "promotion-skipped",
                "skill-b",
                HermesLearningPromotion.STATUS_SKIPPED,
                HermesLearningPromotionReceipt.OUTCOME_SKIPPED,
                false,
                "file-system");
        HermesLearningPromotionReceipt rejected = receipt(
                "key-rejected",
                "promotion-rejected",
                "skill-a",
                HermesLearningPromotion.STATUS_REJECTED,
                HermesLearningPromotionReceipt.OUTCOME_REJECTED,
                false,
                "database");
        ledger.record(persisted);
        ledger.record(skipped);
        ledger.record(rejected);

        HermesLearningAuditService service = new HermesLearningAuditService(ledger);
        HermesLearningAuditSummary summary = service.summarize(10);

        assertThat(summary.hasReceipts()).isTrue();
        assertThat(summary.scannedReceipts()).isEqualTo(3);
        assertThat(summary.matchedReceipts()).isEqualTo(3);
        assertThat(summary.persistedReceipts()).isEqualTo(1);
        assertThat(summary.skippedReceipts()).isEqualTo(1);
        assertThat(summary.rejectedReceipts()).isEqualTo(1);
        assertThat(summary.approvedReceipts()).isEqualTo(1);
        assertThat(summary.distinctSkills()).isEqualTo(2);
        assertThat(summary.statusCounts())
                .containsEntry(HermesLearningPromotion.STATUS_APPROVED, 1L)
                .containsEntry(HermesLearningPromotion.STATUS_SKIPPED, 1L)
                .containsEntry(HermesLearningPromotion.STATUS_REJECTED, 1L);
        assertThat(summary.outcomeCounts())
                .containsEntry(HermesLearningPromotionReceipt.OUTCOME_PERSISTED, 1L)
                .containsEntry(HermesLearningPromotionReceipt.OUTCOME_SKIPPED, 1L)
                .containsEntry(HermesLearningPromotionReceipt.OUTCOME_REJECTED, 1L);
        assertThat(summary.adapterCounts())
                .containsEntry("database", 2L)
                .containsEntry("file-system", 1L);
        assertThat(summary.toMetadata())
                .containsEntry("scannedReceipts", 3)
                .containsEntry("matchedReceipts", 3)
                .containsEntry("persistedReceipts", 1L)
                .containsKey("outcomeCounts");
    }

    @Test
    void inspectsFilteredLearningAuditWindow() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt persisted = receipt(
                "key-skill-a",
                "promotion-skill-a",
                "skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "database");
        ledger.record(persisted);
        ledger.record(receipt(
                "key-skill-b",
                "promotion-skill-b",
                "skill-b",
                HermesLearningPromotion.STATUS_SKIPPED,
                HermesLearningPromotionReceipt.OUTCOME_SKIPPED,
                false,
                "file-system"));

        HermesLearningAuditView view = new HermesLearningAuditService(ledger)
                .inspect(HermesLearningPromotionReceiptQuery.forSkill("skill-a", 10));
        Map<String, Object> metadata = view.toMetadata();

        assertThat(view.page().receipts()).containsExactly(persisted);
        assertThat(view.summary().persistedReceipts()).isEqualTo(1);
        assertThat(view.summary().latestSkillId()).isEqualTo("skill-a");
        assertThat(metadata)
                .containsEntry("matchedReceipts", 1)
                .containsEntry("returnedReceipts", 1)
                .containsEntry("persistedReceipts", 1L)
                .containsEntry("latestSkillId", "skill-a")
                .containsEntry("retentionState", "unbounded")
                .containsKeys("query", "page", "summary");
        assertThat(metadataMap(metadata, "retentionStatus"))
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("bounded", false)
                .containsEntry("recordCount", 2)
                .containsEntry("requiresAttention", false);
        assertThat(metadataMap(metadata, "query"))
                .containsEntry("skillId", "skill-a")
                .containsEntry("limit", 10);
        assertThat(metadataMap(metadata, "summary"))
                .containsEntry("matchedReceipts", 1)
                .containsEntry("latestSkillId", "skill-a");
    }

    @Test
    void exposesCommonLearningAuditQueries() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        HermesLearningPromotionReceipt persisted = receipt(
                "key-persisted",
                "promotion-persisted",
                "skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "database");
        HermesLearningPromotionReceipt skipped = receipt(
                "key-skipped",
                "promotion-skipped",
                "skill-b",
                HermesLearningPromotion.STATUS_SKIPPED,
                HermesLearningPromotionReceipt.OUTCOME_SKIPPED,
                false,
                "file-system");
        ledger.record(persisted);
        ledger.record(skipped);
        HermesLearningAuditService service = new HermesLearningAuditService(ledger);

        assertThat(service.persisted(10).receipts()).containsExactly(persisted);
        assertThat(service.status(HermesLearningPromotion.STATUS_SKIPPED, 10).receipts())
                .containsExactly(skipped);
        assertThat(service.outcome(HermesLearningPromotionReceipt.OUTCOME_SKIPPED, 10).receipts())
                .containsExactly(skipped);
        assertThat(service.skill("skill-a", 10).receipts()).containsExactly(persisted);
        assertThat(service.inspectLatest(1).page().returnedReceipts()).isEqualTo(1);
    }

    @Test
    void resolvesConfiguredFileLedgerForAuditService(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("learning/promotion-receipts.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "learningPromotionReceiptLedgerStore", "file-system",
                        "learningPromotionReceiptLedgerPath", ledgerPath.toString()))
                .build();
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedgerResolver.resolve(config);
        HermesLearningPromotionReceipt receipt = ledger.record(receipt(
                "key-file",
                "promotion-file",
                "skill-file",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "file-system"));

        HermesLearningAuditService service = new HermesLearningAuditService(
                HermesLearningPromotionReceiptLedgerResolver.resolve(config));

        assertThat(service.latest(10).receipts()).containsExactly(receipt);
        assertThat(service.summarize(10).latestSkillId()).isEqualTo("skill-file");
    }

    @Test
    void exposesRetentionPressureForBoundedLearningAuditLedger(@TempDir Path tempDir) {
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedger.fileSystem(
                        tempDir.resolve("learning/promotion-receipts.jsonl"),
                        2);
        ledger.record(receipt(
                "key-file-a",
                "promotion-file-a",
                "skill-file-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "file-system"));
        ledger.record(receipt(
                "key-file-b",
                "promotion-file-b",
                "skill-file-b",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true,
                "file-system"));

        HermesLearningAuditService service = new HermesLearningAuditService(ledger);
        HermesLearningAuditView view = service.inspectLatest(10);

        assertThat(view.retentionStatus())
                .extracting(
                        HermesLearningAuditRetentionStatus::ledgerType,
                        HermesLearningAuditRetentionStatus::bounded,
                        HermesLearningAuditRetentionStatus::recordCount,
                        HermesLearningAuditRetentionStatus::maxEntries,
                        HermesLearningAuditRetentionStatus::atCapacity,
                        HermesLearningAuditRetentionStatus::status)
                .containsExactly(
                        "file-system",
                        true,
                        2,
                        2,
                        true,
                        "at-capacity");
        assertThat(metadataMap(view.toMetadata(), "retentionStatus"))
                .containsEntry("remainingEntries", 0)
                .containsEntry("utilizationPercent", 100)
                .containsEntry("requiresAttention", true);
        assertThat(view.retentionStatus().recommendedActions())
                .containsExactly(
                        "increase-learning-audit-retention-limit",
                        "archive-learning-audit-receipts");
        assertThat(view.toMetadata())
                .containsEntry("retentionRequiresAttention", true)
                .containsEntry("retentionSeverity", "warning")
                .containsEntry("retentionPriority", 2)
                .containsKey("retentionRecommendedActions");

        List<HermesRuntimeEvent> events = new ArrayList<>();
        assertThat(service.publishRetentionEvent(events::add)).isPresent();
        assertThat(events)
                .hasSize(1)
                .first()
                .extracting(
                        HermesRuntimeEvent::type,
                        HermesRuntimeEvent::outcome)
                .containsExactly(
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "at-capacity");
        assertThat(events.get(0).metadata())
                .containsEntry("retentionState", "at-capacity")
                .containsEntry("retentionRequiresAttention", true)
                .containsEntry("ledgerType", "file-system");

        List<HermesRuntimeEvent> monitoredEvents = new ArrayList<>();
        HermesLearningAuditRetentionEventMonitor monitor =
                new HermesLearningAuditRetentionEventMonitor(monitoredEvents::add);
        assertThat(service.publishRetentionEventIfChanged(monitor)).isPresent();
        assertThat(service.publishRetentionEventIfChanged(monitor)).isEmpty();
        assertThat(monitoredEvents)
                .hasSize(1)
                .first()
                .extracting(HermesRuntimeEvent::outcome)
                .isEqualTo("at-capacity");

        HermesLearningAuditRetentionObservation duplicateObservation = service.observeRetention(monitor);
        assertThat(duplicateObservation)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted)
                .containsExactly("suppressed", "duplicate-state", false);
    }

    @Test
    void emptyLedgerProducesEmptySummary() {
        HermesLearningAuditSummary summary = new HermesLearningAuditService(null).summarize();

        assertThat(summary.hasReceipts()).isFalse();
        assertThat(summary.scannedReceipts()).isZero();
        assertThat(summary.matchedReceipts()).isZero();
        assertThat(summary.toMetadata())
                .containsEntry("latestSkillId", "")
                .containsEntry("latestRecordedAt", "");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private static HermesLearningPromotionReceipt receipt(
            String idempotencyKey,
            String promotionId,
            String skillId,
            String status,
            String outcome,
            boolean persisted,
            String adapterId) {
        return new HermesLearningPromotionReceipt(
                promotionId,
                idempotencyKey,
                status,
                outcome,
                skillId,
                persisted,
                outcome,
                adapterId,
                "definitions=" + adapterId + ",artifacts=" + adapterId,
                Map.of("adapterId", adapterId));
    }
}
