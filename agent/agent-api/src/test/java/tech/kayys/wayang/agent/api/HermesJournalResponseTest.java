package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesJournalResponseTest {

    @Test
    void projectsJournalMetadataToTypedFields() {
        HermesJournalResponse response = HermesJournalResponse.from(new HermesPortResponse(
                "runtime-journal",
                "inspect",
                "session:session-a",
                true,
                true,
                true,
                "inspected",
                "runtime journal inspected",
                Map.ofEntries(
                        Map.entry("matchedEvents", 2L),
                        Map.entry("totalMatchedEvents", 3),
                        Map.entry("returnedEvents", "1"),
                        Map.entry("truncated", true),
                        Map.entry("previousCursor", "evt-prev"),
                        Map.entry("nextCursor", "evt-next"),
                        Map.entry("firstCursor", "evt-first"),
                        Map.entry("lastCursor", "evt-last"),
                        Map.entry("hasPreviousPage", true),
                        Map.entry("hasNextPage", "true"),
                        Map.entry("cursorResolved", true),
                        Map.entry("status", "needs-attention"),
                        Map.entry("resumable", "true"),
                        Map.entry("requiresAttention", true),
                        Map.entry("summary", Map.of(
                                "scannedEvents", 1,
                                "matchedEvents", 2,
                                "latestEventId", "evt-1",
                                "typeCounts", Map.of("response.completed", 1))),
                        Map.entry("events", List.of(
                                Map.of(
                                        "eventId", "evt-1",
                                        "type", "response.completed",
                                        "metadata", Map.of("durationMs", 42)),
                                Map.of(
                                        "eventId", "evt-retention",
                                        "type", "learning-audit.retention.attention",
                                        "outcome", "near-capacity",
                                        "metadata", Map.of(
                                                "source", "learning-audit-retention",
                                                "retentionStatus", retentionStatus())))),
                        Map.entry("query", Map.of("sessionId", "session-a")),
                        Map.entry("journalView", Map.of("matchedEvents", 2)),
                        Map.entry("sessionSnapshot", Map.of("latestRequestId", "req-1")))));

        assertThat(response)
                .extracting(
                        HermesJournalResponse::port,
                        HermesJournalResponse::target,
                        HermesJournalResponse::matchedEvents,
                        HermesJournalResponse::totalMatchedEvents,
                        HermesJournalResponse::returnedEvents,
                        HermesJournalResponse::truncated,
                        HermesJournalResponse::previousCursor,
                        HermesJournalResponse::nextCursor,
                        HermesJournalResponse::firstCursor,
                        HermesJournalResponse::lastCursor,
                        HermesJournalResponse::hasPreviousPage,
                        HermesJournalResponse::hasNextPage,
                        HermesJournalResponse::cursorResolved,
                        HermesJournalResponse::journalStatus,
                        HermesJournalResponse::resumable,
                        HermesJournalResponse::requiresAttention,
                        journalResponse -> journalResponse.summary().latestEventId(),
                        journalResponse -> journalResponse.events().size(),
                        journalResponse -> journalResponse.learningAuditRetentionEvents().size(),
                        journalResponse -> journalResponse.learningAuditRetentionSummary().totalEvents(),
                        journalResponse -> journalResponse.learningAuditRetentionSummary().requiresAttention(),
                        journalResponse -> journalResponse.operationalAttentionItems().size(),
                        journalResponse -> journalResponse.operationalAttentionSummary().totalItems(),
                        journalResponse -> journalResponse.operationalActionItems().size(),
                        journalResponse -> journalResponse.operationalActionSummary().totalActions())
                .containsExactly(
                        "runtime-journal",
                        "session:session-a",
                        2,
                        3,
                        1,
                        true,
                        "evt-prev",
                        "evt-next",
                        "evt-first",
                        "evt-last",
                        true,
                        true,
                        true,
                        "needs-attention",
                        true,
                        true,
                        "evt-1",
                        2,
                        1,
                        1,
                        true,
                        1,
                        1,
                        1,
                        1);
        assertThat(response.summary().typeCounts()).containsEntry("response.completed", 1L);
        assertThat(response.events().get(0))
                .extracting(
                        HermesJournalEventResponse::eventId,
                        HermesJournalEventResponse::type)
                .containsExactly("evt-1", "response.completed");
        assertThat(response.events().get(0).metadata()).containsEntry("durationMs", 42);
        assertThat(response.learningAuditRetentionEvents().get(0))
                .extracting(
                        HermesLearningAuditRetentionEventResponse::eventId,
                        HermesLearningAuditRetentionEventResponse::retentionStatus,
                        HermesLearningAuditRetentionEventResponse::retentionSeverity)
                .containsExactly("evt-retention", "near-capacity", "warning");
        assertThat(response.learningAuditRetentionSummary())
                .extracting(
                        HermesLearningAuditRetentionEventSummaryResponse::latestEventId,
                        HermesLearningAuditRetentionEventSummaryResponse::latestRetentionStatus,
                        HermesLearningAuditRetentionEventSummaryResponse::latestRetentionSeverity)
                .containsExactly("evt-retention", "near-capacity", "warning");
        assertThat(response.learningAuditRetentionSummary().retentionStatusCounts())
                .containsEntry("near-capacity", 1L);
        assertThat(response.learningAuditRetentionSummary().retentionRecommendedActionCounts())
                .containsEntry("monitor-learning-audit-retention", 1L);
        assertThat(response.operationalAttentionItems())
                .singleElement()
                .satisfies(item -> assertThat(item)
                        .extracting(
                                HermesOperationalAttention::source,
                                HermesOperationalAttention::severity,
                                HermesOperationalAttention::message)
                        .containsExactly("learning-audit-retention", "warning", "capacity warning"));
        assertThat(response.operationalAttentionSummary().severityCounts())
                .containsEntry("warning", 1L);
        assertThat(response.operationalActionItems())
                .singleElement()
                .satisfies(action -> assertThat(action)
                        .extracting(
                                HermesOperationalAction::actionId,
                                HermesOperationalAction::safe)
                        .containsExactly("monitor-learning-audit-retention", true));
        assertThat(response.operationalActionSummary().riskLevelCounts())
                .containsEntry("low", 1L);
        assertThat(response.query()).containsEntry("sessionId", "session-a");
        assertThat(response.journalView()).containsEntry("matchedEvents", 2);
        assertThat(response.sessionSnapshot()).containsEntry("latestRequestId", "req-1");
    }

    @Test
    void projectsNestedJournalPageEventsWhenTopLevelEventsAreMissing() {
        HermesJournalResponse response = HermesJournalResponse.from(new HermesPortResponse(
                "runtime-journal",
                "inspect",
                "latest",
                true,
                true,
                true,
                "inspected",
                "runtime journal inspected",
                Map.of("journalView", Map.of("page", Map.of(
                        "events", List.of(Map.of(
                                "eventId", "evt-nested",
                                "type", "learning-audit.retention.attention")))))));

        assertThat(response.events())
                .singleElement()
                .satisfies(event -> assertThat(event)
                        .extracting(
                                HermesJournalEventResponse::eventId,
                                HermesJournalEventResponse::type)
                        .containsExactly("evt-nested", "learning-audit.retention.attention"));
        assertThat(response.learningAuditRetentionSummary().totalEvents()).isEqualTo(1);
        assertThat(response.learningAuditRetentionSummary().retentionStatusCounts())
                .containsEntry("unknown", 1L);
        assertThat(response.operationalAttentionSummary().totalItems()).isZero();
        assertThat(response.operationalActionSummary().totalActions()).isZero();
    }

    @Test
    void projectsNestedJournalSummaryWhenTopLevelSummaryIsMissing() {
        HermesJournalResponse response = HermesJournalResponse.from(new HermesPortResponse(
                "runtime-journal",
                "inspect",
                "latest",
                true,
                true,
                true,
                "inspected",
                "runtime journal inspected",
                Map.of("journalView", Map.of("summary", Map.of(
                        "scannedEvents", 2,
                        "matchedEvents", 4,
                        "latestRequestId", "req-nested",
                        "outcomeCounts", Map.of("warning", 2))))));

        assertThat(response.summary())
                .extracting(
                        HermesJournalSummaryResponse::scannedEvents,
                        HermesJournalSummaryResponse::matchedEvents,
                        HermesJournalSummaryResponse::latestRequestId)
                .containsExactly(2, 4, "req-nested");
        assertThat(response.summary().outcomeCounts()).containsEntry("warning", 2L);
    }

    @Test
    void handlesMissingMetadata() {
        HermesJournalResponse response = HermesJournalResponse.from(null);

        assertThat(response.port()).isEqualTo("unknown");
        assertThat(response.matchedEvents()).isZero();
        assertThat(response.totalMatchedEvents()).isZero();
        assertThat(response.returnedEvents()).isZero();
        assertThat(response.truncated()).isFalse();
        assertThat(response.previousCursor()).isEmpty();
        assertThat(response.nextCursor()).isEmpty();
        assertThat(response.firstCursor()).isEmpty();
        assertThat(response.lastCursor()).isEmpty();
        assertThat(response.hasPreviousPage()).isFalse();
        assertThat(response.hasNextPage()).isFalse();
        assertThat(response.cursorResolved()).isFalse();
        assertThat(response.summary().scannedEvents()).isZero();
        assertThat(response.events()).isEmpty();
        assertThat(response.learningAuditRetentionEvents()).isEmpty();
        assertThat(response.learningAuditRetentionSummary().totalEvents()).isZero();
        assertThat(response.operationalAttentionItems()).isEmpty();
        assertThat(response.operationalAttentionSummary().totalItems()).isZero();
        assertThat(response.operationalActionItems()).isEmpty();
        assertThat(response.operationalActionSummary().totalActions()).isZero();
        assertThat(response.journalStatus()).isEmpty();
        assertThat(response.query()).isEmpty();
        assertThat(response.journalView()).isEmpty();
        assertThat(response.sessionSnapshot()).isEmpty();
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
