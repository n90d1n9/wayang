package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters recent Hermes runtime events held by queryable event sinks.
 */
public record HermesRuntimeEventQuery(
        String type,
        String typePrefix,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String outcome,
        Instant occurredFrom,
        Instant occurredUntil,
        String beforeEventId,
        String afterEventId,
        int limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1_000;

    public HermesRuntimeEventQuery(
            String type,
            String requestId,
            String tenantId,
            String outcome,
            int limit) {
        this(type, "", requestId, tenantId, "", "", outcome, null, null, "", "", limit);
    }

    public HermesRuntimeEventQuery(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            Instant occurredFrom,
            Instant occurredUntil,
            int limit) {
        this(type, "", requestId, tenantId, sessionId, userId, outcome, occurredFrom, occurredUntil, "", "", limit);
    }

    public HermesRuntimeEventQuery(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            Instant occurredFrom,
            Instant occurredUntil,
            String beforeEventId,
            String afterEventId,
            int limit) {
        this(type, "", requestId, tenantId, sessionId, userId, outcome, occurredFrom, occurredUntil, beforeEventId, afterEventId, limit);
    }

    public HermesRuntimeEventQuery {
        type = HermesText.trimToEmpty(type);
        typePrefix = HermesText.trimToEmpty(typePrefix);
        requestId = HermesText.trimToEmpty(requestId);
        tenantId = HermesText.trimToEmpty(tenantId);
        sessionId = HermesText.trimToEmpty(sessionId);
        userId = HermesText.trimToEmpty(userId);
        outcome = HermesText.trimToEmpty(outcome);
        beforeEventId = HermesText.trimToEmpty(beforeEventId);
        afterEventId = HermesText.trimToEmpty(afterEventId);
        if (occurredFrom != null && occurredUntil != null && occurredFrom.isAfter(occurredUntil)) {
            throw new IllegalArgumentException("occurredFrom cannot be after occurredUntil");
        }
        if (!beforeEventId.isBlank() && !afterEventId.isBlank()) {
            throw new IllegalArgumentException("beforeEventId and afterEventId cannot both be set");
        }
        limit = normalizeLimit(limit);
    }

    public static HermesRuntimeEventQuery latest() {
        return new HermesRuntimeEventQuery("", "", "", "", DEFAULT_LIMIT);
    }

    public static HermesRuntimeEventQuery forRequest(String requestId, int limit) {
        return new HermesRuntimeEventQuery("", requestId, "", "", limit);
    }

    public static HermesRuntimeEventQuery forType(String type, int limit) {
        return new HermesRuntimeEventQuery(type, "", "", "", limit);
    }

    public static HermesRuntimeEventQuery forTypePrefix(String typePrefix, int limit) {
        return new HermesRuntimeEventQuery("", typePrefix, "", "", "", "", "", null, null, "", "", limit);
    }

    public static HermesRuntimeEventQuery learning(int limit) {
        return forTypePrefix("skill.learning", limit);
    }

    public static HermesRuntimeEventQuery forTenant(String tenantId, int limit) {
        return new HermesRuntimeEventQuery("", "", tenantId, "", limit);
    }

    public static HermesRuntimeEventQuery forSession(String sessionId, int limit) {
        return new HermesRuntimeEventQuery("", "", "", sessionId, "", "", null, null, "", "", limit);
    }

    public static HermesRuntimeEventQuery forUser(String userId, int limit) {
        return new HermesRuntimeEventQuery("", "", "", "", userId, "", null, null, "", "", limit);
    }

    public static HermesRuntimeEventQuery failures(int limit) {
        return new HermesRuntimeEventQuery("", "", "", "failed", limit);
    }

    public static HermesRuntimeEventQuery timeWindow(Instant occurredFrom, Instant occurredUntil, int limit) {
        return new HermesRuntimeEventQuery("", "", "", "", "", "", occurredFrom, occurredUntil, "", "", limit);
    }

    public static HermesRuntimeEventQuery beforeEvent(String eventId, int limit) {
        return new HermesRuntimeEventQuery("", "", "", "", "", "", "", null, null, eventId, "", limit);
    }

    public static HermesRuntimeEventQuery afterEvent(String eventId, int limit) {
        return new HermesRuntimeEventQuery("", "", "", "", "", "", "", null, null, "", eventId, limit);
    }

    public boolean matches(HermesRuntimeEvent event) {
        if (event == null) {
            return false;
        }
        if (!type.isBlank() && !type.equals(event.type())) {
            return false;
        }
        if (!typePrefix.isBlank() && !event.type().startsWith(typePrefix)) {
            return false;
        }
        if (!requestId.isBlank() && !requestId.equals(event.requestId())) {
            return false;
        }
        if (!tenantId.isBlank() && !tenantId.equals(event.tenantId())) {
            return false;
        }
        if (!sessionId.isBlank() && !sessionId.equals(event.sessionId())) {
            return false;
        }
        if (!userId.isBlank() && !userId.equals(event.userId())) {
            return false;
        }
        if (!outcome.isBlank() && !outcome.equals(event.outcome())) {
            return false;
        }
        if (occurredFrom != null && event.occurredAt().isBefore(occurredFrom)) {
            return false;
        }
        return occurredUntil == null || !event.occurredAt().isAfter(occurredUntil);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", type);
        metadata.put("typePrefix", typePrefix);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("outcome", outcome);
        metadata.put("occurredFrom", occurredFrom == null ? "" : occurredFrom.toString());
        metadata.put("occurredUntil", occurredUntil == null ? "" : occurredUntil.toString());
        metadata.put("beforeEventId", beforeEventId);
        metadata.put("afterEventId", afterEventId);
        metadata.put("limit", limit);
        return Map.copyOf(metadata);
    }

    private static int normalizeLimit(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

}
