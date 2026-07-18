package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesJournalQueryMapperTest {

    private final HermesJournalQueryMapper mapper = new HermesJournalQueryMapper();

    @Test
    void mapsRequestParametersToRuntimeQuery() {
        var query = mapper.query(request(
                "response.completed",
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                5));

        assertThat(query)
                .extracting(
                        value -> value.type(),
                        value -> value.requestId(),
                        value -> value.tenantId(),
                        value -> value.sessionId(),
                        value -> value.userId(),
                        value -> value.outcome(),
                        value -> value.occurredFrom(),
                        value -> value.occurredUntil(),
                        value -> value.limit())
                .containsExactly(
                        "response.completed",
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        Instant.parse("2026-06-03T00:00:00Z"),
                        Instant.parse("2026-06-04T00:00:00Z"),
                        5);
    }

    @Test
    void defaultsBlankFiltersAndLimit() {
        var query = mapper.query(request("", null, " ", "", null, "", null, null, 0));

        assertThat(query)
                .extracting(
                        value -> value.type(),
                        value -> value.requestId(),
                        value -> value.tenantId(),
                        value -> value.sessionId(),
                        value -> value.userId(),
                        value -> value.outcome(),
                        value -> value.limit())
                .containsExactly("", "", "", "", "", "", 100);
    }

    @Test
    void mapsForcedTypeWhilePreservingFiltersAndCursor() {
        var query = mapper.queryForType(
                requestWithCursor(
                        "response.completed",
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        "2026-06-03T00:00:00Z",
                        "2026-06-04T00:00:00Z",
                        "evt-before",
                        null,
                        7),
                HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);

        assertThat(query)
                .extracting(
                        value -> value.type(),
                        value -> value.requestId(),
                        value -> value.tenantId(),
                        value -> value.sessionId(),
                        value -> value.userId(),
                        value -> value.outcome(),
                        value -> value.occurredFrom(),
                        value -> value.occurredUntil(),
                        value -> value.beforeEventId(),
                        value -> value.afterEventId(),
                        value -> value.limit())
                .containsExactly(
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        Instant.parse("2026-06-03T00:00:00Z"),
                        Instant.parse("2026-06-04T00:00:00Z"),
                        "evt-before",
                        "",
                        7);
    }

    @Test
    void rejectsInvalidOccurredFromInstant() {
        assertThatThrownBy(() -> mapper.query(request(null, null, null, null, null, null, "nope", null, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredFrom must be an ISO-8601 instant");
    }

    @Test
    void rejectsInvalidOccurredUntilInstant() {
        assertThatThrownBy(() -> mapper.query(request(null, null, null, null, null, null, null, "nope", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredUntil must be an ISO-8601 instant");
    }

    @Test
    void rejectsInvertedTimeWindow() {
        assertThatThrownBy(() -> mapper.query(request(
                null,
                null,
                null,
                null,
                null,
                null,
                "2026-06-04T00:00:00Z",
                "2026-06-03T00:00:00Z",
                0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredFrom cannot be after occurredUntil");
    }

    @Test
    void mapsNullRequestToDefaultQuery() {
        var query = mapper.query(null);

        assertThat(query)
                .extracting(
                        value -> value.type(),
                        value -> value.requestId(),
                        value -> value.tenantId(),
                        value -> value.sessionId(),
                        value -> value.userId(),
                        value -> value.outcome(),
                        value -> value.limit())
                .containsExactly("", "", "", "", "", "", 100);
    }

    private HermesJournalRequest request(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredFrom,
            String occurredUntil,
            int limit) {
        return new HermesJournalRequest(
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                occurredFrom,
                occurredUntil,
                limit);
    }

    private HermesJournalRequest requestWithCursor(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredFrom,
            String occurredUntil,
            String beforeEventId,
            String afterEventId,
            int limit) {
        return new HermesJournalRequest(
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                occurredFrom,
                occurredUntil,
                beforeEventId,
                afterEventId,
                limit);
    }
}
