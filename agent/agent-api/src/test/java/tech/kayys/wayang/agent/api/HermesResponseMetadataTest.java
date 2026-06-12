package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesResponseMetadataTest {

    @Test
    void extractsNestedLearningAuditRetentionStatus() {
        Map<String, Object> status = HermesResponseMetadata.learningAuditRetentionStatus(Map.of(
                "learningAuditRetentionStatus", Map.of(
                        "ledgerType", "file-system",
                        "status", "near-capacity")));

        assertThat(status)
                .containsEntry("ledgerType", "file-system")
                .containsEntry("status", "near-capacity");
    }

    @Test
    void extractsFlattenedLearningAuditRetentionStatus() {
        Map<String, Object> status = HermesResponseMetadata.learningAuditRetentionStatus(Map.ofEntries(
                Map.entry("ledgerType", "database"),
                Map.entry("bounded", true),
                Map.entry("recordCount", 7),
                Map.entry("maxEntries", 5),
                Map.entry("overflowEntries", 2),
                Map.entry("utilizationPercent", 140),
                Map.entry("retentionState", "over-capacity"),
                Map.entry("retentionSeverity", "critical"),
                Map.entry("retentionPriority", 3),
                Map.entry("retentionRequiresAttention", true),
                Map.entry("retentionAttention", List.of("over capacity")),
                Map.entry("retentionRecommendedActions", List.of("archive-learning-audit-receipts"))));

        assertThat(status)
                .containsEntry("ledgerType", "database")
                .containsEntry("bounded", true)
                .containsEntry("recordCount", 7)
                .containsEntry("maxEntries", 5)
                .containsEntry("overflowEntries", 2)
                .containsEntry("utilizationPercent", 140)
                .containsEntry("status", "over-capacity")
                .containsEntry("severity", "critical")
                .containsEntry("priority", 3)
                .containsEntry("requiresAttention", true)
                .containsEntry("attention", List.of("over capacity"))
                .containsEntry("recommendedActions", List.of("archive-learning-audit-receipts"));
    }

    @Test
    void extractsPrefixedLearningAuditViewRetentionStatus() {
        Map<String, Object> status = HermesResponseMetadata.learningAuditRetentionStatus(Map.of(
                "retentionState", "near-capacity",
                "retentionSeverity", "warning",
                "retentionPriority", 2,
                "retentionUtilizationPercent", 80,
                "retentionAtCapacity", false,
                "retentionRemainingEntries", 1,
                "retentionRequiresAttention", true));

        assertThat(status)
                .containsEntry("status", "near-capacity")
                .containsEntry("severity", "warning")
                .containsEntry("priority", 2)
                .containsEntry("utilizationPercent", 80)
                .containsEntry("atCapacity", false)
                .containsEntry("remainingEntries", 1)
                .containsEntry("requiresAttention", true);
    }

    @Test
    void returnsEmptyLearningAuditRetentionStatusWhenAbsent() {
        assertThat(HermesResponseMetadata.learningAuditRetentionStatus(Map.of("status", "ready"))).isEmpty();
        assertThat(HermesResponseMetadata.learningAuditRetentionStatus(null)).isEmpty();
    }
}
