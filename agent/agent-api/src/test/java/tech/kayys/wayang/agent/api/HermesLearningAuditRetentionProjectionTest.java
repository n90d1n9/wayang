package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditRetentionProjectionTest {

    @Test
    void parsesRetentionStatusMetadataToTypedValues() {
        HermesLearningAuditRetentionProjection projection = HermesLearningAuditRetentionProjection.from(Map.ofEntries(
                Map.entry("ledgerType", "file-system"),
                Map.entry("bounded", true),
                Map.entry("recordCount", "4"),
                Map.entry("maxEntries", 5),
                Map.entry("remainingEntries", 1),
                Map.entry("overflowEntries", 0),
                Map.entry("utilizationPercent", 80),
                Map.entry("nearCapacity", true),
                Map.entry("atCapacity", false),
                Map.entry("status", "near-capacity"),
                Map.entry("severity", "warning"),
                Map.entry("priority", 2),
                Map.entry("requiresAttention", true),
                Map.entry("attention", List.of("capacity warning")),
                Map.entry("recommendedActions", List.of("monitor-learning-audit-retention"))));

        assertThat(projection)
                .extracting(
                        HermesLearningAuditRetentionProjection::ledgerType,
                        HermesLearningAuditRetentionProjection::bounded,
                        HermesLearningAuditRetentionProjection::recordCount,
                        HermesLearningAuditRetentionProjection::maxEntries,
                        HermesLearningAuditRetentionProjection::remainingEntries,
                        HermesLearningAuditRetentionProjection::utilizationPercent,
                        HermesLearningAuditRetentionProjection::status,
                        HermesLearningAuditRetentionProjection::severity,
                        HermesLearningAuditRetentionProjection::priority,
                        HermesLearningAuditRetentionProjection::requiresAttention)
                .containsExactly(
                        "file-system",
                        true,
                        4,
                        5,
                        1,
                        80,
                        "near-capacity",
                        "warning",
                        2,
                        true);
        assertThat(projection.attention()).containsExactly("capacity warning");
        assertThat(projection.recommendedActions()).containsExactly("monitor-learning-audit-retention");
    }

    @Test
    void defaultsMissingRetentionStatusMetadata() {
        HermesLearningAuditRetentionProjection projection = HermesLearningAuditRetentionProjection.from(Map.of());

        assertThat(projection.ledgerType()).isEqualTo("unknown");
        assertThat(projection.status()).isEmpty();
        assertThat(projection.severity()).isEmpty();
        assertThat(projection.requiresAttention()).isFalse();
        assertThat(projection.attention()).isEmpty();
        assertThat(projection.recommendedActions()).isEmpty();
    }
}
