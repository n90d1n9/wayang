package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventPage;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventQuery;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalDirective;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesJournalResponseMapperTest {

    private final HermesJournalResponseMapper mapper = new HermesJournalResponseMapper();

    @Test
    void inspectsConfiguredJournalPort() {
        HermesRuntimeEvent event = runtimeEvent(
                "req-1",
                "tenant-a",
                "session-a",
                "successful",
                Instant.parse("2026-06-03T00:00:00Z"));
        HermesRuntimeJournalDirective directive = HermesRuntimeJournalDirective.inspect(
                new HermesRuntimeEventQuery("", "", "tenant-a", "session-a", "", "", null, null, 5));

        Response response = mapper.inspect(
                Optional.of(HermesRuntimeJournalPort.service(
                        new HermesRuntimeJournalService(query -> runtimeEventPage(List.of(event), query)))),
                directive);

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesJournalResponse::port,
                        HermesJournalResponse::target,
                        HermesJournalResponse::successful,
                        HermesJournalResponse::matchedEvents,
                        HermesJournalResponse::totalMatchedEvents,
                        HermesJournalResponse::returnedEvents)
                .containsExactly("runtime-journal", "session:session-a", true, 1, 1, 1);
        assertThat(body.firstCursor()).isEqualTo(event.eventId());
        assertThat(body.lastCursor()).isEqualTo(event.eventId());
        assertThat(body.cursorResolved()).isTrue();
        assertThat(body.summary())
                .extracting(
                        HermesJournalSummaryResponse::latestEventId,
                        HermesJournalSummaryResponse::latestRequestId,
                        HermesJournalSummaryResponse::successfulEvents)
                .containsExactly(event.eventId(), "req-1", 1L);
        assertThat(body.events())
                .singleElement()
                .satisfies(returnedEvent -> assertThat(returnedEvent)
                        .extracting(
                                HermesJournalEventResponse::eventId,
                                HermesJournalEventResponse::requestId)
                        .containsExactly(event.eventId(), "req-1"));
        assertThat(body.learningAuditRetentionEvents()).isEmpty();
        assertThat(body.learningAuditRetentionSummary().totalEvents()).isZero();
        assertThat(body.metadata())
                .containsEntry("matchedEvents", 1)
                .containsEntry("totalMatchedEvents", 1)
                .containsEntry("returnedEvents", 1)
                .containsKey("summary")
                .containsKey("events")
                .containsKeys("journalView", "sessionSnapshot", "query");
        assertThat(body.query())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a");
        assertThat(body.sessionSnapshot())
                .containsEntry("latestRequestId", "req-1")
                .containsEntry("status", "completed");
    }

    @Test
    void returnsNotFoundWhenJournalPortIsMissing() {
        Response response = mapper.inspect(
                Optional.empty(),
                HermesRuntimeJournalDirective.inspect(HermesRuntimeEventQuery.latest()));

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_JOURNAL_PORT));
    }

    @Test
    void returnsBadRequestWhenDeferredDirectiveIsInvalid() {
        Response response = mapper.inspect(
                Optional.of(HermesRuntimeJournalPort.noop()),
                () -> {
                    throw new IllegalArgumentException("occurredFrom must be an ISO-8601 instant");
                });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse("occurredFrom must be an ISO-8601 instant"));
    }

    private HermesRuntimeEvent runtimeEvent(
            String requestId,
            String tenantId,
            String sessionId,
            String outcome,
            Instant occurredAt) {
        return new HermesRuntimeEvent(
                "",
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                requestId,
                tenantId,
                sessionId,
                null,
                outcome,
                occurredAt,
                Map.of("test", true));
    }

    private HermesRuntimeEventPage runtimeEventPage(List<HermesRuntimeEvent> events, HermesRuntimeEventQuery query) {
        List<HermesRuntimeEvent> matched = events.stream()
                .filter(query::matches)
                .toList();
        List<HermesRuntimeEvent> returned = matched.stream().limit(query.limit()).toList();
        String firstCursor = returned.isEmpty() ? "" : returned.get(0).eventId();
        String lastCursor = returned.isEmpty() ? "" : returned.get(returned.size() - 1).eventId();
        return new HermesRuntimeEventPage(
                returned,
                matched.size(),
                matched.size(),
                "",
                "",
                firstCursor,
                lastCursor,
                false,
                false,
                true);
    }
}
