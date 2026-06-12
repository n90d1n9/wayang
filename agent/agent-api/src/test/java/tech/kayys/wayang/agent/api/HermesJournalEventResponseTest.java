package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesJournalEventResponseTest {

    @Test
    void projectsRuntimeEventMetadataToTypedFields() {
        HermesJournalEventResponse response = HermesJournalEventResponse.from(Map.of(
                "eventId", "evt-1",
                "type", "learning-audit.retention.attention",
                "requestId", "req-1",
                "tenantId", "tenant-a",
                "sessionId", "session-a",
                "userId", "user-a",
                "outcome", "warning",
                "occurredAt", "2026-06-03T00:00:00Z",
                "metadata", Map.of("retentionStatus", "warning")));

        assertThat(response)
                .extracting(
                        HermesJournalEventResponse::eventId,
                        HermesJournalEventResponse::type,
                        HermesJournalEventResponse::requestId,
                        HermesJournalEventResponse::tenantId,
                        HermesJournalEventResponse::sessionId,
                        HermesJournalEventResponse::userId,
                        HermesJournalEventResponse::outcome,
                        HermesJournalEventResponse::occurredAt)
                .containsExactly(
                        "evt-1",
                        "learning-audit.retention.attention",
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        "2026-06-03T00:00:00Z");
        assertThat(response.metadata()).containsEntry("retentionStatus", "warning");
    }

    @Test
    void handlesMissingEventMetadata() {
        HermesJournalEventResponse response = HermesJournalEventResponse.from(null);

        assertThat(response)
                .extracting(
                        HermesJournalEventResponse::eventId,
                        HermesJournalEventResponse::type,
                        HermesJournalEventResponse::requestId,
                        HermesJournalEventResponse::tenantId,
                        HermesJournalEventResponse::sessionId,
                        HermesJournalEventResponse::userId,
                        HermesJournalEventResponse::outcome,
                        HermesJournalEventResponse::occurredAt)
                .containsExactly("", "", "", "", "", "", "", "");
        assertThat(response.metadata()).isEmpty();
    }
}
