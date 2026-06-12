package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HermesLearningAuditRetentionEventSummaryResponseTest {

    @Test
    void aggregatesRetentionEventPressureAndActions() {
        HermesLearningAuditRetentionEventSummaryResponse response =
                HermesLearningAuditRetentionEventSummaryResponse.from(List.of(
                        retentionEvent(
                                "evt-1",
                                "2026-06-03T00:00:00Z",
                                "near-capacity",
                                "warning",
                                2,
                                4,
                                1,
                                0,
                                80,
                                true,
                                false,
                                true,
                                List.of("capacity warning"),
                                List.of("monitor-learning-audit-retention")),
                        retentionEvent(
                                "evt-2",
                                "2026-06-03T01:00:00Z",
                                "over-capacity",
                                "critical",
                                3,
                                7,
                                0,
                                2,
                                140,
                                true,
                                true,
                                true,
                                List.of("over capacity", "capacity warning"),
                                List.of(
                                        "archive-learning-audit-receipts",
                                        "monitor-learning-audit-retention"))));

        assertThat(response)
                .extracting(
                        HermesLearningAuditRetentionEventSummaryResponse::totalEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::attentionEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::criticalEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::warningEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::highestPriority,
                        HermesLearningAuditRetentionEventSummaryResponse::nearCapacityEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::atCapacityEvents,
                        HermesLearningAuditRetentionEventSummaryResponse::maxUtilizationPercent,
                        HermesLearningAuditRetentionEventSummaryResponse::maxOverflowEntries,
                        HermesLearningAuditRetentionEventSummaryResponse::minRemainingEntries,
                        HermesLearningAuditRetentionEventSummaryResponse::latestUtilizationPercent,
                        HermesLearningAuditRetentionEventSummaryResponse::requiresAttention,
                        HermesLearningAuditRetentionEventSummaryResponse::latestEventId,
                        HermesLearningAuditRetentionEventSummaryResponse::latestRetentionStatus,
                        HermesLearningAuditRetentionEventSummaryResponse::latestRetentionSeverity)
                .containsExactly(
                        2,
                        2,
                        1,
                        1,
                        3,
                        2,
                        1,
                        140,
                        2,
                        0,
                        140,
                        true,
                        "evt-2",
                        "over-capacity",
                        "critical");
        assertThat(response.retentionStatusCounts())
                .containsEntry("near-capacity", 1L)
                .containsEntry("over-capacity", 1L);
        assertThat(response.retentionSeverityCounts())
                .containsEntry("warning", 1L)
                .containsEntry("critical", 1L);
        assertThat(response.retentionRecommendedActionCounts())
                .containsEntry("monitor-learning-audit-retention", 2L)
                .containsEntry("archive-learning-audit-receipts", 1L);
        assertThat(response.retentionAttention()).containsExactly("capacity warning", "over capacity");
        assertThat(response.retentionAttentionItems())
                .extracting(
                        HermesOperationalAttention::severity,
                        HermesOperationalAttention::priority,
                        HermesOperationalAttention::message)
                .containsExactly(
                        tuple("critical", 3, "capacity warning"),
                        tuple("critical", 3, "over capacity"));
        assertThat(response.retentionAttentionSummary())
                .extracting(
                        HermesOperationalAttentionSummaryResponse::totalItems,
                        HermesOperationalAttentionSummaryResponse::retryableItems,
                        HermesOperationalAttentionSummaryResponse::highestPriority,
                        HermesOperationalAttentionSummaryResponse::requiresAttention)
                .containsExactly(2, 0, 3, true);
        assertThat(response.retentionAttentionSummary().sourceCounts())
                .containsEntry("learning-audit-retention", 2L);
        assertThat(response.retentionAttentionSummary().severityCounts())
                .containsEntry("critical", 2L);
        assertThat(response.retentionRecommendedActionItems())
                .extracting(
                        HermesOperationalAction::actionId,
                        HermesOperationalAction::riskLevel)
                .containsExactly(
                        tuple("monitor-learning-audit-retention", "low"),
                        tuple("archive-learning-audit-receipts", "medium"));
        assertThat(response.retentionRecommendedActionSummary())
                .extracting(
                        HermesOperationalActionSummaryResponse::totalActions,
                        HermesOperationalActionSummaryResponse::safeActions,
                        HermesOperationalActionSummaryResponse::unsafeActions,
                        HermesOperationalActionSummaryResponse::dryRunSupportedActions,
                        HermesOperationalActionSummaryResponse::requiresOperatorApproval,
                        HermesOperationalActionSummaryResponse::requiresConfiguration)
                .containsExactly(2, 1, 1, 1, true, true);
        assertThat(response.retentionRecommendedActionSummary().requiredConfig())
                .containsExactly("learning-audit-archive-target");
    }

    @Test
    void handlesEmptyEvents() {
        HermesLearningAuditRetentionEventSummaryResponse response =
                HermesLearningAuditRetentionEventSummaryResponse.empty();

        assertThat(response.totalEvents()).isZero();
        assertThat(response.requiresAttention()).isFalse();
        assertThat(response.nearCapacityEvents()).isZero();
        assertThat(response.maxUtilizationPercent()).isZero();
        assertThat(response.latestEventId()).isEmpty();
        assertThat(response.retentionStatusCounts()).isEmpty();
        assertThat(response.retentionAttentionItems()).isEmpty();
        assertThat(response.retentionAttentionSummary().totalItems()).isZero();
        assertThat(response.retentionRecommendedActionItems()).isEmpty();
        assertThat(response.retentionRecommendedActionSummary().totalActions()).isZero();
    }

    private HermesLearningAuditRetentionEventResponse retentionEvent(
            String eventId,
            String occurredAt,
            String status,
            String severity,
            int priority,
            int recordCount,
            int remainingEntries,
            int overflowEntries,
            int utilizationPercent,
            boolean nearCapacity,
            boolean atCapacity,
            boolean requiresAttention,
            List<String> attention,
            List<String> actions) {
        return HermesLearningAuditRetentionEventResponse.from(new HermesJournalEventResponse(
                eventId,
                HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                "req-" + eventId,
                "tenant-a",
                "session-a",
                "user-a",
                status,
                occurredAt,
                Map.of("retentionStatus", Map.ofEntries(
                        Map.entry("ledgerType", "file-system"),
                        Map.entry("status", status),
                        Map.entry("severity", severity),
                        Map.entry("priority", priority),
                        Map.entry("recordCount", recordCount),
                        Map.entry("remainingEntries", remainingEntries),
                        Map.entry("overflowEntries", overflowEntries),
                        Map.entry("utilizationPercent", utilizationPercent),
                        Map.entry("nearCapacity", nearCapacity),
                        Map.entry("atCapacity", atCapacity),
                        Map.entry("requiresAttention", requiresAttention),
                        Map.entry("attention", attention),
                        Map.entry("recommendedActions", actions)))));
    }
}
