package tech.kayys.wayang.agent.api;

import java.util.Map;

/**
 * Stable REST projection of a Hermes runtime journal event.
 */
public record HermesJournalEventResponse(
        String eventId,
        String type,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String outcome,
        String occurredAt,
        Map<String, Object> metadata) {

    public HermesJournalEventResponse {
        eventId = HermesResponseMetadata.text(eventId, "");
        type = HermesResponseMetadata.text(type, "");
        requestId = HermesResponseMetadata.text(requestId, "");
        tenantId = HermesResponseMetadata.text(tenantId, "");
        sessionId = HermesResponseMetadata.text(sessionId, "");
        userId = HermesResponseMetadata.text(userId, "");
        outcome = HermesResponseMetadata.text(outcome, "");
        occurredAt = HermesResponseMetadata.text(occurredAt, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    static HermesJournalEventResponse from(Map<String, Object> event) {
        Map<String, Object> values = event == null ? Map.of() : event;
        return new HermesJournalEventResponse(
                HermesResponseMetadata.text(values.get("eventId"), ""),
                HermesResponseMetadata.text(values.get("type"), ""),
                HermesResponseMetadata.text(values.get("requestId"), ""),
                HermesResponseMetadata.text(values.get("tenantId"), ""),
                HermesResponseMetadata.text(values.get("sessionId"), ""),
                HermesResponseMetadata.text(values.get("userId"), ""),
                HermesResponseMetadata.text(values.get("outcome"), ""),
                HermesResponseMetadata.text(values.get("occurredAt"), ""),
                HermesResponseMetadata.objectMap(values.get("metadata")));
    }
}
