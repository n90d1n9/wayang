package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalDirective;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesJournalDirectiveMapperTest {

    private final HermesJournalDirectiveMapper mapper = new HermesJournalDirectiveMapper();

    @Test
    void mapsRequestParametersToJournalQuery() {
        HermesRuntimeJournalDirective directive = mapper.directive(
                request(
                        "response.completed",
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        "2026-06-03T00:00:00Z",
                        "2026-06-04T00:00:00Z",
                        5));

        assertThat(directive.target()).isEqualTo("request:req-1");
        assertThat(directive.query())
                .extracting(
                        query -> query.type(),
                        query -> query.requestId(),
                        query -> query.tenantId(),
                        query -> query.sessionId(),
                        query -> query.userId(),
                        query -> query.outcome(),
                        query -> query.occurredFrom(),
                        query -> query.occurredUntil(),
                        query -> query.limit())
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
    void defaultsBlankTextFilters() {
        HermesRuntimeJournalDirective directive = mapper.directive(
                request("", null, " ", "", null, "", null, null, 0));

        assertThat(directive.query())
                .extracting(
                        query -> query.type(),
                        query -> query.requestId(),
                        query -> query.tenantId(),
                        query -> query.sessionId(),
                        query -> query.userId(),
                        query -> query.outcome())
                .containsExactly("", "", "", "", "", "");
    }

    @Test
    void mapsCursorParametersToJournalQuery() {
        HermesRuntimeJournalDirective before = mapper.directive(
                requestWithCursor("evt-older", null, 5));
        HermesRuntimeJournalDirective after = mapper.directive(
                requestWithCursor(null, "evt-newer", 6));

        assertThat(before.target()).isEqualTo("before-event:evt-older");
        assertThat(before.query())
                .extracting(
                        query -> query.beforeEventId(),
                        query -> query.afterEventId(),
                        query -> query.limit())
                .containsExactly("evt-older", "", 5);
        assertThat(after.target()).isEqualTo("after-event:evt-newer");
        assertThat(after.query())
                .extracting(
                        query -> query.beforeEventId(),
                        query -> query.afterEventId(),
                        query -> query.limit())
                .containsExactly("", "evt-newer", 6);
    }

    @Test
    void mapsLearningAuditRetentionToForcedRetentionTypeQuery() {
        HermesRuntimeJournalDirective directive = mapper.learningAuditRetention(
                request(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        null,
                        null,
                        8));

        assertThat(directive.target()).isEqualTo("session:session-a");
        assertThat(directive.query())
                .extracting(
                        query -> query.type(),
                        query -> query.tenantId(),
                        query -> query.sessionId(),
                        query -> query.userId(),
                        query -> query.outcome(),
                        query -> query.limit())
                .containsExactly(
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        8);
    }

    @Test
    void rejectsInvalidOccurredFromInstant() {
        assertThatThrownBy(() -> mapper.directive(request(null, null, null, null, null, null, "nope", null, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredFrom must be an ISO-8601 instant");
    }

    @Test
    void rejectsInvalidOccurredUntilInstant() {
        assertThatThrownBy(() -> mapper.directive(request(null, null, null, null, null, null, null, "nope", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredUntil must be an ISO-8601 instant");
    }

    @Test
    void rejectsConflictingCursorParameters() {
        assertThatThrownBy(() -> mapper.directive(requestWithCursor("evt-old", "evt-new", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("beforeEventId and afterEventId cannot both be set");
    }

    @Test
    void mapsNullRequestToDefaultQuery() {
        HermesRuntimeJournalDirective directive = mapper.directive(null);

        assertThat(directive.query())
                .extracting(
                        query -> query.type(),
                        query -> query.requestId(),
                        query -> query.tenantId(),
                        query -> query.sessionId(),
                        query -> query.userId(),
                        query -> query.outcome(),
                        query -> query.limit())
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
            String beforeEventId,
            String afterEventId,
            int limit) {
        return new HermesJournalRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                beforeEventId,
                afterEventId,
                limit);
    }
}
