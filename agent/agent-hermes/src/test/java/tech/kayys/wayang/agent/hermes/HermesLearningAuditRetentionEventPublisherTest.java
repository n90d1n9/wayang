package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditRetentionEventPublisherTest {

    @Test
    void emitsAttentionEventForPressuredRetentionStatus() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionStatus status = HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "file-system",
                "recordCount", 8,
                "retentionPolicy", Map.of(
                        "retentionMode", "max-entries",
                        "maxEntries", 10)));

        Optional<HermesRuntimeEvent> published =
                new HermesLearningAuditRetentionEventPublisher(events::add).publish(status);

        assertThat(published).isPresent();
        assertThat(events).containsExactly(published.orElseThrow());
        HermesRuntimeEvent event = published.orElseThrow();
        assertThat(event.type()).isEqualTo(HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);
        assertThat(event.outcome()).isEqualTo("near-capacity");
        assertThat(event.metadata())
                .containsEntry("mode", HermesAgentMode.MODE_ID)
                .containsEntry("source", "learning-audit-retention")
                .containsEntry("retentionState", "near-capacity")
                .containsEntry("retentionSeverity", "warning")
                .containsEntry("retentionPriority", 2)
                .containsEntry("ledgerType", "file-system")
                .containsEntry("recordCount", 8)
                .containsEntry("maxEntries", 10)
                .containsEntry("remainingEntries", 2)
                .containsEntry("utilizationPercent", 80);
        assertThat(metadataMap(event.metadata(), "retentionStatus"))
                .containsEntry("status", "near-capacity")
                .containsEntry("requiresAttention", true)
                .containsEntry("severity", "warning")
                .containsEntry("priority", 2);
        assertThat(strings(event.metadata(), "retentionRecommendedActions"))
                .containsExactly(
                        "monitor-learning-audit-retention",
                        "plan-learning-audit-retention-capacity");
    }

    @Test
    void suppressesEventForHealthyOrUnboundedRetentionStatus() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesLearningAuditRetentionEventPublisher publisher =
                new HermesLearningAuditRetentionEventPublisher(events::add);

        Optional<HermesRuntimeEvent> healthy = publisher.publish(HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "file-system",
                "recordCount", 1,
                "retentionPolicy", Map.of("maxEntries", 10))));
        Optional<HermesRuntimeEvent> unbounded = publisher.publish(HermesLearningAuditRetentionStatus.fromMetadata(Map.of(
                "ledgerType", "in-memory",
                "recordCount", 3)));

        assertThat(healthy).isEmpty();
        assertThat(unbounded).isEmpty();
        assertThat(events).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> metadata, String key) {
        return (List<String>) metadata.get(key);
    }
}
