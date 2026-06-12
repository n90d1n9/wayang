package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditRetentionObserverTest {

    @Test
    void remembersLatestRetentionObservation(@TempDir Path tempDir) {
        HermesLearningPromotionReceiptLedger ledger =
                HermesLearningPromotionReceiptLedger.fileSystem(
                        tempDir.resolve("learning/promotion-receipts.jsonl"),
                        1);
        ledger.record(receipt("key-a", "promotion-a", "skill-a"));
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionObserver observer = new HermesLearningAuditRetentionObserver(
                new HermesLearningAuditService(ledger),
                new HermesLearningAuditRetentionEventMonitor(events::add));

        assertThat(observer.lastObservation())
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason)
                .containsExactly("unavailable", "not-observed");

        HermesLearningAuditRetentionObservation emitted = observer.observeRetention();
        HermesLearningAuditRetentionObservation duplicate = observer.observeRetention();

        assertThat(emitted)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted)
                .containsExactly("emitted", "first-observation", true);
        assertThat(duplicate)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted)
                .containsExactly("suppressed", "duplicate-state", false);
        assertThat(observer.lastObservation()).isEqualTo(duplicate);
        assertThat(observer.toMetadata())
                .containsEntry("outcome", "suppressed")
                .containsEntry("reason", "duplicate-state")
                .containsEntry("retentionState", "at-capacity");
        assertThat(events)
                .hasSize(1)
                .first()
                .extracting(HermesRuntimeEvent::outcome)
                .isEqualTo("at-capacity");
    }

    @Test
    void recordsUnavailableObservationForNoopObserver() {
        HermesLearningAuditRetentionObserver observer = HermesLearningAuditRetentionObserver.noop();

        HermesLearningAuditRetentionObservation observation = observer.observeRetention();

        assertThat(observation)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted)
                .containsExactly("unavailable", "observer-unavailable", false);
        assertThat(observer.lastObservation()).isEqualTo(observation);
    }

    private static HermesLearningPromotionReceipt receipt(
            String idempotencyKey,
            String promotionId,
            String skillId) {
        return new HermesLearningPromotionReceipt(
                promotionId,
                idempotencyKey,
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                skillId,
                true,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                "file-system",
                "definitions=file-system,artifacts=file-system",
                Map.of("adapterId", "file-system"));
    }
}
