package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeJournalPortTest {

    @Test
    void directiveBuildsStableTargetsAndMetadata() {
        HermesRuntimeJournalDirective sessionDirective = HermesRuntimeJournalDirective.inspect(
                HermesRuntimeEventQuery.forSession("session-a", 5));
        HermesRuntimeJournalDirective latestDirective = HermesRuntimeJournalDirective.latest(2);
        HermesRuntimeJournalDirective inactive = HermesRuntimeJournalDirective.none();

        assertThat(sessionDirective.target()).isEqualTo("session:session-a");
        assertThat(sessionDirective.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "inspect")
                .containsEntry("target", "session:session-a")
                .containsKey("query");
        assertThat(latestDirective.target()).isEqualTo("latest");
        assertThat(inactive.active()).isFalse();
        assertThat(inactive.operation()).isEqualTo("none");
    }

    @Test
    void serviceBackedPortReturnsJournalViewMetadata() {
        HermesRuntimeEvent event = new HermesRuntimeEvent(
                "",
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                Instant.parse("2026-06-03T00:00:00Z"),
                Map.of("test", true));
        HermesRuntimeEventReader reader = query -> HermesRuntimeEventPages.from(List.of(event), query);
        HermesRuntimeJournalPort port = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(reader));

        HermesPortDispatchResult result = port.inspect(HermesRuntimeJournalDirective.inspect(
                HermesRuntimeEventQuery.forSession("session-a", 10)));

        assertThat(result.port()).isEqualTo("runtime-journal");
        assertThat(result.status()).isEqualTo("inspected");
        assertThat(result.successful()).isTrue();
        assertThat(result.metadata())
                .containsEntry("matchedEvents", 1)
                .containsEntry("totalMatchedEvents", 1)
                .containsEntry("returnedEvents", 1)
                .containsEntry("truncated", false)
                .containsEntry("firstCursor", event.eventId())
                .containsEntry("lastCursor", event.eventId())
                .containsEntry("cursorResolved", true)
                .containsEntry("status", "completed")
                .containsEntry("resumable", false)
                .containsEntry("requiresAttention", false)
                .containsKeys("journalView", "summary", "sessionSnapshot", "events");
        assertThat(metadataMap(result.metadata(), "summary"))
                .containsEntry("latestEventId", event.eventId())
                .containsEntry("latestRequestId", "req-1")
                .containsEntry("successfulEvents", 1L);
        assertThat(metadataMap(result.metadata(), "journalView"))
                .containsEntry("matchedEvents", 1)
                .containsEntry("totalMatchedEvents", 1)
                .containsEntry("returnedEvents", 1)
                .containsEntry("truncated", false)
                .containsEntry("firstCursor", event.eventId())
                .containsEntry("lastCursor", event.eventId())
                .containsKey("events");
        assertThat(metadataMaps(result.metadata(), "events"))
                .singleElement()
                .satisfies(returnedEvent -> assertThat(returnedEvent)
                        .containsEntry("eventId", event.eventId())
                        .containsEntry("requestId", "req-1"));
        assertThat(metadataMap(result.metadata(), "sessionSnapshot"))
                .containsEntry("status", "completed")
                .containsEntry("resumable", false)
                .containsEntry("latestRequestId", "req-1");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> metadataMaps(Map<String, Object> metadata, String key) {
        return (List<Map<String, Object>>) metadata.get(key);
    }
}
