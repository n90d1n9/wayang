package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesJournalSummaryResponseTest {

    @Test
    void projectsJournalSummaryMetadataToTypedFields() {
        HermesJournalSummaryResponse response = HermesJournalSummaryResponse.from(Map.ofEntries(
                Map.entry("scannedEvents", "2"),
                Map.entry("matchedEvents", 3),
                Map.entry("truncated", true),
                Map.entry("failedEvents", 1L),
                Map.entry("successfulEvents", "1"),
                Map.entry("distinctRequests", 2),
                Map.entry("firstOccurredAt", "2026-06-03T00:00:00Z"),
                Map.entry("latestOccurredAt", "2026-06-03T01:00:00Z"),
                Map.entry("latestEventId", "evt-2"),
                Map.entry("latestRequestId", "req-2"),
                Map.entry("typeCounts", Map.of("response.completed", 1, "response.failed", "1")),
                Map.entry("outcomeCounts", Map.of("successful", 1L, "failed", 1L)),
                Map.entry("tenantCounts", Map.of("tenant-a", 2L))));

        assertThat(response)
                .extracting(
                        HermesJournalSummaryResponse::scannedEvents,
                        HermesJournalSummaryResponse::matchedEvents,
                        HermesJournalSummaryResponse::truncated,
                        HermesJournalSummaryResponse::failedEvents,
                        HermesJournalSummaryResponse::successfulEvents,
                        HermesJournalSummaryResponse::distinctRequests,
                        HermesJournalSummaryResponse::firstOccurredAt,
                        HermesJournalSummaryResponse::latestOccurredAt,
                        HermesJournalSummaryResponse::latestEventId,
                        HermesJournalSummaryResponse::latestRequestId)
                .containsExactly(
                        2,
                        3,
                        true,
                        1L,
                        1L,
                        2L,
                        "2026-06-03T00:00:00Z",
                        "2026-06-03T01:00:00Z",
                        "evt-2",
                        "req-2");
        assertThat(response.typeCounts()).containsEntry("response.completed", 1L);
        assertThat(response.outcomeCounts()).containsEntry("failed", 1L);
        assertThat(response.tenantCounts()).containsEntry("tenant-a", 2L);
    }

    @Test
    void handlesMissingSummaryMetadata() {
        HermesJournalSummaryResponse response = HermesJournalSummaryResponse.from(null);

        assertThat(response.scannedEvents()).isZero();
        assertThat(response.matchedEvents()).isZero();
        assertThat(response.truncated()).isFalse();
        assertThat(response.latestEventId()).isEmpty();
        assertThat(response.typeCounts()).isEmpty();
    }
}
