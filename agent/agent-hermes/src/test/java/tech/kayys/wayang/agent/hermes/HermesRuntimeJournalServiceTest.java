package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesRuntimeJournalServiceTest {

    @Test
    void summarizesBoundedRuntimeJournalWindow() {
        CollectingReader reader = new CollectingReader();
        reader.emit(event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "planned",
                "2026-06-03T00:00:00Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "successful",
                "2026-06-03T00:00:01Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "req-2",
                "tenant-b",
                "failed",
                "2026-06-03T00:00:02Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED,
                "req-3",
                "tenant-b",
                "degraded",
                "2026-06-03T00:00:03Z"));

        HermesRuntimeJournalService service = new HermesRuntimeJournalService(reader);
        HermesRuntimeJournalSummary summary = service.summarize(3);

        assertThat(summary.scannedEvents()).isEqualTo(3);
        assertThat(summary.matchedEvents()).isEqualTo(4);
        assertThat(summary.truncated()).isTrue();
        assertThat(summary.failedEvents()).isEqualTo(1);
        assertThat(summary.successfulEvents()).isEqualTo(1);
        assertThat(summary.distinctRequests()).isEqualTo(3);
        assertThat(summary.firstOccurredAt()).isEqualTo(Instant.parse("2026-06-03T00:00:01Z"));
        assertThat(summary.latestOccurredAt()).isEqualTo(Instant.parse("2026-06-03T00:00:03Z"));
        assertThat(summary.latestRequestId()).isEqualTo("req-3");
        assertThat(summary.typeCounts())
                .containsEntry(HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED, 1L)
                .containsEntry(HermesRuntimeEvent.TYPE_RESPONSE_FAILED, 1L)
                .containsEntry(HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED, 1L);
        assertThat(summary.tenantCounts())
                .containsEntry("tenant-a", 1L)
                .containsEntry("tenant-b", 2L);
        assertThat(summary.toMetadata())
                .containsEntry("scannedEvents", 3)
                .containsEntry("matchedEvents", 4)
                .containsEntry("truncated", true)
                .containsEntry("latestRequestId", "req-3")
                .containsKey("typeCounts");
    }

    @Test
    void exposesCommonJournalQueries() {
        CollectingReader reader = new CollectingReader();
        reader.emit(event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "planned",
                "2026-06-03T00:00:00Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "req-2",
                "tenant-a",
                "failed",
                "2026-06-03T00:00:01Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                "req-3",
                "tenant-a",
                "created",
                "2026-06-03T00:00:02Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED,
                "req-4",
                "tenant-a",
                "failed",
                "2026-06-03T00:00:03Z"));

        HermesRuntimeJournalService service = new HermesRuntimeJournalService(reader);

        assertThat(service.failures(10).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-2", "req-4");
        assertThat(service.request("req-1", 10).events())
                .extracting(HermesRuntimeEvent::type)
                .containsExactly(HermesRuntimeEvent.TYPE_REQUEST_PLANNED);
        assertThat(service.learning(10).events())
                .extracting(HermesRuntimeEvent::type)
                .containsExactly(
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED);
        assertThat(HermesRuntimeEventQuery.learning(10).toMetadata())
                .containsEntry("typePrefix", "skill.learning")
                .containsEntry("limit", 10);
    }

    @Test
    void filtersJournalByGatewayIdentityAndTimeWindow() {
        CollectingReader reader = new CollectingReader();
        reader.emit(event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-1",
                "planned",
                "2026-06-03T00:00:00Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "req-2",
                "tenant-a",
                "session-b",
                "user-1",
                "failed",
                "2026-06-03T00:00:01Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-3",
                "tenant-b",
                "session-a",
                "user-2",
                "successful",
                "2026-06-03T00:00:02Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED,
                "req-4",
                "tenant-a",
                "session-a",
                "user-1",
                "degraded",
                "2026-06-03T00:00:03Z"));
        HermesRuntimeJournalService service = new HermesRuntimeJournalService(reader);

        assertThat(service.tenant("tenant-a", 10).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-1", "req-2", "req-4");
        assertThat(service.session("session-a", 10).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-1", "req-3", "req-4");
        assertThat(service.user("user-1", 10).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-1", "req-2", "req-4");
        assertThat(service.timeWindow(
                        Instant.parse("2026-06-03T00:00:01Z"),
                        Instant.parse("2026-06-03T00:00:02Z"),
                        10).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-2", "req-3");
        assertThat(service.query(new HermesRuntimeEventQuery(
                        "",
                        "",
                        "tenant-a",
                        "session-a",
                        "user-1",
                        "",
                        Instant.parse("2026-06-03T00:00:00Z"),
                        Instant.parse("2026-06-03T00:00:03Z"),
                        10)).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-1", "req-4");
        assertThatThrownBy(() -> HermesRuntimeEventQuery.timeWindow(
                        Instant.parse("2026-06-03T00:00:02Z"),
                        Instant.parse("2026-06-03T00:00:01Z"),
                        10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occurredFrom");
    }

    @Test
    void paginatesJournalWithStableTimelineCursors() {
        CollectingReader reader = new CollectingReader();
        reader.emit(event(
                "evt-4",
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-4",
                "tenant-a",
                "successful",
                "2026-06-03T00:00:03Z"));
        reader.emit(event(
                "evt-1",
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "planned",
                "2026-06-03T00:00:00Z"));
        reader.emit(event(
                "evt-3",
                HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "req-3",
                "tenant-a",
                "failed",
                "2026-06-03T00:00:02Z"));
        reader.emit(event(
                "evt-2",
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-2",
                "tenant-a",
                "successful",
                "2026-06-03T00:00:01Z"));

        HermesRuntimeJournalService service = new HermesRuntimeJournalService(reader);
        HermesRuntimeEventPage latest = service.latest(2);
        HermesRuntimeEventPage older = service.query(HermesRuntimeEventQuery.beforeEvent("evt-3", 2));
        HermesRuntimeEventPage newer = service.query(HermesRuntimeEventQuery.afterEvent("evt-2", 1));
        HermesRuntimeEventPage missing = service.query(HermesRuntimeEventQuery.beforeEvent("missing", 2));

        assertThat(latest.events())
                .extracting(HermesRuntimeEvent::eventId)
                .containsExactly("evt-3", "evt-4");
        assertThat(latest)
                .extracting(
                        HermesRuntimeEventPage::matchedEvents,
                        HermesRuntimeEventPage::totalMatchedEvents,
                        HermesRuntimeEventPage::previousCursor,
                        HermesRuntimeEventPage::nextCursor,
                        HermesRuntimeEventPage::hasPreviousPage,
                        HermesRuntimeEventPage::hasNextPage,
                        HermesRuntimeEventPage::cursorResolved)
                .containsExactly(4, 4, "evt-3", "", true, false, true);
        assertThat(older.events())
                .extracting(HermesRuntimeEvent::eventId)
                .containsExactly("evt-1", "evt-2");
        assertThat(older)
                .extracting(
                        HermesRuntimeEventPage::matchedEvents,
                        HermesRuntimeEventPage::totalMatchedEvents,
                        HermesRuntimeEventPage::previousCursor,
                        HermesRuntimeEventPage::nextCursor,
                        HermesRuntimeEventPage::hasPreviousPage,
                        HermesRuntimeEventPage::hasNextPage)
                .containsExactly(2, 4, "", "evt-2", false, true);
        assertThat(newer.events())
                .extracting(HermesRuntimeEvent::eventId)
                .containsExactly("evt-3");
        assertThat(newer)
                .extracting(
                        HermesRuntimeEventPage::matchedEvents,
                        HermesRuntimeEventPage::totalMatchedEvents,
                        HermesRuntimeEventPage::previousCursor,
                        HermesRuntimeEventPage::nextCursor,
                        HermesRuntimeEventPage::hasPreviousPage,
                        HermesRuntimeEventPage::hasNextPage)
                .containsExactly(2, 4, "evt-3", "evt-3", true, true);
        assertThat(newer.toMetadata())
                .containsEntry("previousCursor", "evt-3")
                .containsEntry("nextCursor", "evt-3")
                .containsEntry("totalMatchedEvents", 4)
                .containsEntry("cursorResolved", true);
        assertThat(missing.events()).isEmpty();
        assertThat(missing.toMetadata())
                .containsEntry("totalMatchedEvents", 4)
                .containsEntry("cursorResolved", false);
        assertThatThrownBy(() -> new HermesRuntimeEventQuery(
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        "evt-1",
                        "evt-2",
                        10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot both be set");
    }

    @Test
    void inspectsJournalWithStableAdapterMetadata() {
        CollectingReader reader = new CollectingReader();
        reader.emit(event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-1",
                "planned",
                "2026-06-03T00:00:00Z"));
        reader.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-2",
                "tenant-a",
                "session-a",
                "user-1",
                "successful",
                "2026-06-03T00:00:01Z"));
        HermesRuntimeEventQuery query = new HermesRuntimeEventQuery(
                "",
                "",
                "tenant-a",
                "session-a",
                "user-1",
                "",
                Instant.parse("2026-06-03T00:00:00Z"),
                Instant.parse("2026-06-03T00:00:02Z"),
                1);

        HermesRuntimeJournalView view = new HermesRuntimeJournalService(reader).inspect(query);
        Map<String, Object> metadata = view.toMetadata();

        assertThat(view.page().returnedEvents()).isEqualTo(1);
        assertThat(view.page().matchedEvents()).isEqualTo(2);
        assertThat(view.summary().latestRequestId()).isEqualTo("req-2");
        assertThat(metadata)
                .containsEntry("matchedEvents", 2)
                .containsEntry("returnedEvents", 1)
                .containsEntry("truncated", true)
                .containsKeys("query", "page", "summary");
        assertThat(metadataMap(metadata, "query"))
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-1")
                .containsEntry("limit", 1);
        assertThat(metadataMap(metadata, "page"))
                .containsEntry("matchedEvents", 2)
                .containsEntry("returnedEvents", 1)
                .containsEntry("truncated", true)
                .containsKey("events");
        assertThat(metadataMap(metadata, "summary"))
                .containsEntry("matchedEvents", 2)
                .containsEntry("scannedEvents", 1)
                .containsEntry("latestRequestId", "req-2");
    }

    @Test
    void producerCompositionExposesJournalBackedReader(@TempDir Path tempDir) {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalPath(tempDir.resolve("hermes-runtime.jsonl").toString())
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkResolver.compose(
                config,
                Optional.empty(),
                Optional.empty());
        HermesRuntimeJournalService service = HermesRuntimeJournalService.fromSink(sink);

        sink.emit(event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-produced",
                "tenant-a",
                "successful",
                "2026-06-03T00:00:04Z"));

        assertThat(service.request("req-produced", 10).events())
                .extracting(HermesRuntimeEvent::type)
                .containsExactly(HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED);
        assertThat(service.summarize(10).latestRequestId()).isEqualTo("req-produced");
    }

    @Test
    void emptyReaderProducesEmptySummary() {
        HermesRuntimeJournalSummary summary = new HermesRuntimeJournalService(null).summarize();

        assertThat(summary.hasEvents()).isFalse();
        assertThat(summary.scannedEvents()).isZero();
        assertThat(summary.matchedEvents()).isZero();
        assertThat(summary.toMetadata())
                .containsEntry("latestEventId", "")
                .containsEntry("latestOccurredAt", "");
    }

    private static HermesRuntimeEvent event(
            String type,
            String requestId,
            String tenantId,
            String outcome,
            String occurredAt) {
        return event(type, requestId, tenantId, "", "", outcome, occurredAt);
    }

    private static HermesRuntimeEvent event(
            String eventId,
            String type,
            String requestId,
            String tenantId,
            String outcome,
            String occurredAt) {
        return new HermesRuntimeEvent(
                eventId,
                type,
                requestId,
                tenantId,
                "",
                "",
                outcome,
                Instant.parse(occurredAt),
                Map.of("test", true));
    }

    private static HermesRuntimeEvent event(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredAt) {
        return new HermesRuntimeEvent(
                "",
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                Instant.parse(occurredAt),
                Map.of("test", true));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private static final class CollectingReader implements HermesRuntimeEventSink, HermesRuntimeEventReader {

        private final List<HermesRuntimeEvent> events = new ArrayList<>();

        @Override
        public void emit(HermesRuntimeEvent event) {
            events.add(event);
        }

        @Override
        public HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
            return HermesRuntimeEventPages.from(events, query);
        }
    }
}
