package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HermesLearningAuditRetentionEventResponseTest {

    @Test
    void projectsNestedRetentionStatusToTypedFields() {
        HermesLearningAuditRetentionEventResponse response = HermesLearningAuditRetentionEventResponse.from(
                new HermesJournalEventResponse(
                        "evt-1",
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "near-capacity",
                        "2026-06-03T00:00:00Z",
                        Map.of(
                                "source", "learning-audit-retention",
                                "retentionStatus", retentionStatus())));

        assertThat(response)
                .extracting(
                        HermesLearningAuditRetentionEventResponse::eventId,
                        HermesLearningAuditRetentionEventResponse::type,
                        HermesLearningAuditRetentionEventResponse::source,
                        HermesLearningAuditRetentionEventResponse::ledgerType,
                        HermesLearningAuditRetentionEventResponse::recordCount,
                        HermesLearningAuditRetentionEventResponse::maxEntries,
                        HermesLearningAuditRetentionEventResponse::remainingEntries,
                        HermesLearningAuditRetentionEventResponse::utilizationPercent,
                        HermesLearningAuditRetentionEventResponse::retentionStatus,
                        HermesLearningAuditRetentionEventResponse::retentionSeverity,
                        HermesLearningAuditRetentionEventResponse::retentionPriority,
                        HermesLearningAuditRetentionEventResponse::retentionRequiresAttention)
                .containsExactly(
                        "evt-1",
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "learning-audit-retention",
                        "file-system",
                        4,
                        5,
                        1,
                        80,
                        "near-capacity",
                        "warning",
                        2,
                        true);
        assertThat(response.retentionAttention()).containsExactly("capacity warning");
        assertThat(response.retentionAttentionItems())
                .extracting(
                        HermesOperationalAttention::source,
                        HermesOperationalAttention::severity,
                        HermesOperationalAttention::priority,
                        HermesOperationalAttention::message)
                .containsExactly(tuple("learning-audit-retention", "warning", 2, "capacity warning"));
        assertThat(response.retentionRecommendedActionItems())
                .extracting(
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::riskLevel,
                        HermesOperationalAction::safe)
                .containsExactly(tuple("monitor-learning-audit-retention", "low", true));
        assertThat(response.learningAuditRetentionStatus()).containsEntry("status", "near-capacity");
        assertThat(response.retentionPolicy()).containsEntry("maxEntries", 5);
    }

    @Test
    void projectsFlattenedRetentionMetadataWhenNestedStatusIsMissing() {
        HermesLearningAuditRetentionEventResponse response = HermesLearningAuditRetentionEventResponse.from(
                new HermesJournalEventResponse(
                        "evt-2",
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "",
                        "tenant-a",
                        "",
                        "",
                        "over-capacity",
                        "2026-06-03T01:00:00Z",
                        Map.ofEntries(
                                Map.entry("source", "learning-audit-retention"),
                                Map.entry("ledgerType", "database"),
                                Map.entry("recordCount", 7),
                                Map.entry("maxEntries", 5),
                                Map.entry("overflowEntries", 2),
                                Map.entry("utilizationPercent", 140),
                                Map.entry("retentionState", "over-capacity"),
                                Map.entry("retentionSeverity", "critical"),
                                Map.entry("retentionPriority", 3),
                                Map.entry("retentionRequiresAttention", true),
                                Map.entry("retentionAttention", List.of("over capacity")),
                                Map.entry("retentionRecommendedActions", List.of("archive-learning-audit-receipts")))));

        assertThat(response)
                .extracting(
                        HermesLearningAuditRetentionEventResponse::ledgerType,
                        HermesLearningAuditRetentionEventResponse::recordCount,
                        HermesLearningAuditRetentionEventResponse::overflowEntries,
                        HermesLearningAuditRetentionEventResponse::retentionStatus,
                        HermesLearningAuditRetentionEventResponse::retentionSeverity)
                .containsExactly("database", 7, 2, "over-capacity", "critical");
        assertThat(response.retentionRecommendedActionItems())
                .extracting(
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::requiredConfig)
                .containsExactly(tuple(
                        "archive-learning-audit-receipts",
                        List.of("learning-audit-archive-target")));
    }

    @Test
    void projectsPrefixedRetentionViewMetadataWhenNestedStatusIsMissing() {
        HermesLearningAuditRetentionEventResponse response = HermesLearningAuditRetentionEventResponse.from(
                new HermesJournalEventResponse(
                        "evt-3",
                        "response.completed",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "2026-06-03T02:00:00Z",
                        Map.of(
                                "retentionState", "near-capacity",
                                "retentionSeverity", "warning",
                                "retentionPriority", 2,
                                "retentionUtilizationPercent", 80,
                                "retentionRemainingEntries", 1,
                                "retentionAtCapacity", false,
                                "retentionRequiresAttention", true)));

        assertThat(response)
                .extracting(
                        HermesLearningAuditRetentionEventResponse::retentionStatus,
                        HermesLearningAuditRetentionEventResponse::retentionSeverity,
                        HermesLearningAuditRetentionEventResponse::retentionPriority,
                        HermesLearningAuditRetentionEventResponse::utilizationPercent,
                        HermesLearningAuditRetentionEventResponse::remainingEntries,
                        HermesLearningAuditRetentionEventResponse::atCapacity,
                        HermesLearningAuditRetentionEventResponse::retentionRequiresAttention)
                .containsExactly("near-capacity", "warning", 2, 80, 1, false, true);
    }

    @Test
    void detectsRetentionEventsFromTypeSourceOrMetadata() {
        assertThat(HermesLearningAuditRetentionEventResponse.isRetentionEvent(
                HermesJournalEventResponse.from(Map.of("type", HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION))))
                .isTrue();
        assertThat(HermesLearningAuditRetentionEventResponse.isRetentionEvent(
                HermesJournalEventResponse.from(Map.of("metadata", Map.of("source", "learning-audit-retention")))))
                .isTrue();
        assertThat(HermesLearningAuditRetentionEventResponse.isRetentionEvent(
                HermesJournalEventResponse.from(Map.of("metadata", Map.of("retentionStatus", retentionStatus())))))
                .isTrue();
        assertThat(HermesLearningAuditRetentionEventResponse.isRetentionEvent(
                HermesJournalEventResponse.from(Map.of("metadata", Map.of(
                        "retentionState", "near-capacity",
                        "retentionUtilizationPercent", 80)))))
                .isTrue();
        assertThat(HermesLearningAuditRetentionEventResponse.isRetentionEvent(
                HermesJournalEventResponse.from(Map.of("type", "response.completed"))))
                .isFalse();
    }

    private Map<String, Object> retentionStatus() {
        return Map.ofEntries(
                Map.entry("ledgerType", "file-system"),
                Map.entry("bounded", true),
                Map.entry("recordCount", 4),
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
                Map.entry("recommendedActions", List.of("monitor-learning-audit-retention")),
                Map.entry("retentionPolicy", Map.of("retentionMode", "max-entries", "maxEntries", 5)));
    }
}
