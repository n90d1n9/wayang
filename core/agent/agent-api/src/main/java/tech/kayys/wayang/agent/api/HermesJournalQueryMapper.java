package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimeEventQuery;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Maps Hermes journal query parameters to a validated runtime-event query.
 */
final class HermesJournalQueryMapper {

    HermesRuntimeEventQuery query(HermesJournalRequest request) {
        HermesJournalRequest resolved = request == null ? new HermesJournalRequest() : request;
        return new HermesRuntimeEventQuery(
                text(resolved.type()),
                text(resolved.requestId()),
                text(resolved.tenantId()),
                text(resolved.sessionId()),
                text(resolved.userId()),
                text(resolved.outcome()),
                instant(resolved.occurredFrom(), "occurredFrom"),
                instant(resolved.occurredUntil(), "occurredUntil"),
                text(resolved.beforeEventId()),
                text(resolved.afterEventId()),
                resolved.limit());
    }

    HermesRuntimeEventQuery queryForType(HermesJournalRequest request, String type) {
        HermesRuntimeEventQuery query = query(request);
        return new HermesRuntimeEventQuery(
                text(type),
                query.requestId(),
                query.tenantId(),
                query.sessionId(),
                query.userId(),
                query.outcome(),
                query.occurredFrom(),
                query.occurredUntil(),
                query.beforeEventId(),
                query.afterEventId(),
                query.limit());
    }

    private Instant instant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 instant", error);
        }
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
