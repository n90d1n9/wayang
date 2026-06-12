package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for Hermes learned-skill audit inspection.
 */
public final class HermesLearningAuditRequest {

    private String skillId;
    private String status;
    private String outcome;
    private String idempotencyKey;
    private boolean persistedOnly;
    private String beforeReceiptId;
    private String afterReceiptId;
    private int limit;

    public HermesLearningAuditRequest() {
        this(null, null, null, null, false, 0);
    }

    HermesLearningAuditRequest(
            String skillId,
            String status,
            String outcome,
            String idempotencyKey,
            boolean persistedOnly,
            int limit) {
        this(skillId, status, outcome, idempotencyKey, persistedOnly, null, null, limit);
    }

    HermesLearningAuditRequest(
            String skillId,
            String status,
            String outcome,
            String idempotencyKey,
            boolean persistedOnly,
            String beforeReceiptId,
            String afterReceiptId,
            int limit) {
        this.skillId = skillId;
        this.status = status;
        this.outcome = outcome;
        this.idempotencyKey = idempotencyKey;
        this.persistedOnly = persistedOnly;
        this.beforeReceiptId = beforeReceiptId;
        this.afterReceiptId = afterReceiptId;
        this.limit = limit;
    }

    public String skillId() {
        return skillId;
    }

    @QueryParam("skillId")
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String status() {
        return status;
    }

    @QueryParam("status")
    public void setStatus(String status) {
        this.status = status;
    }

    public String outcome() {
        return outcome;
    }

    @QueryParam("outcome")
    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    @QueryParam("idempotencyKey")
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean persistedOnly() {
        return persistedOnly;
    }

    @QueryParam("persistedOnly")
    public void setPersistedOnly(boolean persistedOnly) {
        this.persistedOnly = persistedOnly;
    }

    public String beforeReceiptId() {
        return beforeReceiptId;
    }

    @QueryParam("beforeReceiptId")
    public void setBeforeReceiptId(String beforeReceiptId) {
        this.beforeReceiptId = beforeReceiptId;
    }

    public String afterReceiptId() {
        return afterReceiptId;
    }

    @QueryParam("afterReceiptId")
    public void setAfterReceiptId(String afterReceiptId) {
        this.afterReceiptId = afterReceiptId;
    }

    public int limit() {
        return limit;
    }

    @QueryParam("limit")
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
