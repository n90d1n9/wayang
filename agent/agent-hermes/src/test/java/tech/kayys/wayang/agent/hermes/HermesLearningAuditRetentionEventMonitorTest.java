package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditRetentionEventMonitorTest {

    @Test
    void emitsOnlyWhenRetentionAttentionStateChanges() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionEventMonitor monitor =
                new HermesLearningAuditRetentionEventMonitor(events::add);

        Optional<HermesRuntimeEvent> firstNearCapacity = monitor.publishIfChanged(status(8, 10));
        Optional<HermesRuntimeEvent> duplicateNearCapacity = monitor.publishIfChanged(status(9, 10));
        Optional<HermesRuntimeEvent> atCapacity = monitor.publishIfChanged(status(10, 10));

        assertThat(firstNearCapacity).isPresent();
        assertThat(duplicateNearCapacity).isEmpty();
        assertThat(atCapacity).isPresent();
        assertThat(events)
                .extracting(HermesRuntimeEvent::outcome)
                .containsExactly("near-capacity", "at-capacity");
        assertThat(firstNearCapacity.orElseThrow().metadata())
                .containsEntry("retentionEventReason", "first-observation")
                .containsEntry("previousRetentionState", "")
                .containsEntry("currentRetentionState", "near-capacity");
        assertThat(atCapacity.orElseThrow().metadata())
                .containsEntry("retentionEventReason", "state-transition")
                .containsEntry("previousRetentionState", "near-capacity")
                .containsEntry("currentRetentionState", "at-capacity")
                .containsEntry("previousRetentionSeverity", "warning")
                .containsEntry("currentRetentionSeverity", "warning");
    }

    @Test
    void resetsSuppressionAfterRetentionRecovers() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionEventMonitor monitor =
                new HermesLearningAuditRetentionEventMonitor(events::add);

        assertThat(monitor.publishIfChanged(status(8, 10))).isPresent();
        assertThat(monitor.publishIfChanged(status(1, 10))).isEmpty();
        Optional<HermesRuntimeEvent> renewedPressure = monitor.publishIfChanged(status(8, 10));

        assertThat(renewedPressure).isPresent();
        assertThat(events)
                .extracting(HermesRuntimeEvent::outcome)
                .containsExactly("near-capacity", "near-capacity");
        assertThat(renewedPressure.orElseThrow().metadata())
                .containsEntry("retentionEventReason", "first-observation")
                .containsEntry("previousRetentionState", "")
                .containsEntry("currentRetentionState", "near-capacity");
    }

    @Test
    void reportsStructuredObservationOutcomes() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionEventMonitor monitor =
                new HermesLearningAuditRetentionEventMonitor(events::add);

        HermesLearningAuditRetentionObservation emitted = monitor.observe(status(8, 10));
        HermesLearningAuditRetentionObservation duplicate = monitor.observe(status(9, 10));
        HermesLearningAuditRetentionObservation recovered = monitor.observe(status(1, 10));

        assertThat(emitted)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted,
                        HermesLearningAuditRetentionObservation::requiresAttention)
                .containsExactly("emitted", "first-observation", true, true);
        assertThat(emitted.event()).isPresent();
        assertThat(emitted.toMetadata())
                .containsEntry("outcome", "emitted")
                .containsEntry("reason", "first-observation")
                .containsEntry("retentionState", "near-capacity")
                .containsKey("event");

        assertThat(duplicate)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted,
                        HermesLearningAuditRetentionObservation::requiresAttention)
                .containsExactly("suppressed", "duplicate-state", false, true);
        assertThat(duplicate.event()).isEmpty();
        assertThat(duplicate.metadata())
                .containsEntry("previousRetentionState", "near-capacity")
                .containsEntry("currentRetentionState", "near-capacity");

        assertThat(recovered)
                .extracting(
                        HermesLearningAuditRetentionObservation::outcome,
                        HermesLearningAuditRetentionObservation::reason,
                        HermesLearningAuditRetentionObservation::emitted,
                        HermesLearningAuditRetentionObservation::requiresAttention)
                .containsExactly("suppressed", "recovered", false, false);
        assertThat(recovered.metadata())
                .containsEntry("previousRetentionState", "near-capacity")
                .containsEntry("currentRetentionState", "healthy");
    }

    private static HermesLearningAuditRetentionStatus status(int recordCount, int maxEntries) {
        return HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "file-system",
                "recordCount", recordCount,
                "retentionPolicy", Map.of(
                        "retentionMode", "max-entries",
                        "maxEntries", maxEntries)));
    }
}
