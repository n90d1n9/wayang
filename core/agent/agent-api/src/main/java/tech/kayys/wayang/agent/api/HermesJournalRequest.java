package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for Hermes runtime journal inspection.
 */
public final class HermesJournalRequest {

    private String type;
    private String requestId;
    private String tenantId;
    private String sessionId;
    private String userId;
    private String outcome;
    private String occurredFrom;
    private String occurredUntil;
    private String beforeEventId;
    private String afterEventId;
    private int limit;

    public HermesJournalRequest() {
        this(null, null, null, null, null, null, null, null, null, null, 0);
    }

    HermesJournalRequest(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredFrom,
            String occurredUntil,
            int limit) {
        this(type, requestId, tenantId, sessionId, userId, outcome, occurredFrom, occurredUntil, null, null, limit);
    }

    HermesJournalRequest(
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
        this.type = type;
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.outcome = outcome;
        this.occurredFrom = occurredFrom;
        this.occurredUntil = occurredUntil;
        this.beforeEventId = beforeEventId;
        this.afterEventId = afterEventId;
        this.limit = limit;
    }

    public String type() {
        return type;
    }

    @QueryParam("type")
    public void setType(String type) {
        this.type = type;
    }

    public String requestId() {
        return requestId;
    }

    @QueryParam("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String tenantId() {
        return tenantId;
    }

    @QueryParam("tenantId")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String sessionId() {
        return sessionId;
    }

    @QueryParam("sessionId")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String userId() {
        return userId;
    }

    @QueryParam("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String outcome() {
        return outcome;
    }

    @QueryParam("outcome")
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String occurredFrom() {
        return occurredFrom;
    }

    @QueryParam("occurredFrom")
    public void setOccurredFrom(String occurredFrom) {
        this.occurredFrom = occurredFrom;
    }

    public String occurredUntil() {
        return occurredUntil;
    }

    @QueryParam("occurredUntil")
    public void setOccurredUntil(String occurredUntil) {
        this.occurredUntil = occurredUntil;
    }

    public String beforeEventId() {
        return beforeEventId;
    }

    @QueryParam("beforeEventId")
    public void setBeforeEventId(String beforeEventId) {
        this.beforeEventId = beforeEventId;
    }

    public String afterEventId() {
        return afterEventId;
    }

    @QueryParam("afterEventId")
    public void setAfterEventId(String afterEventId) {
        this.afterEventId = afterEventId;
    }

    public int limit() {
        return limit;
    }

    @QueryParam("limit")
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
