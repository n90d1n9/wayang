package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for named Hermes learned-skill audit inspections.
 */
public final class HermesLearningAuditPresetRequest {

    private int limit;
    private String beforeReceiptId;
    private String afterReceiptId;

    public HermesLearningAuditPresetRequest() {
        this(0);
    }

    HermesLearningAuditPresetRequest(int limit) {
        this(limit, null, null);
    }

    HermesLearningAuditPresetRequest(
            int limit,
            String beforeReceiptId,
            String afterReceiptId) {
        this.limit = limit;
        this.beforeReceiptId = beforeReceiptId;
        this.afterReceiptId = afterReceiptId;
    }

    public int limit() {
        return limit;
    }

    @QueryParam("limit")
    public void setLimit(int limit) {
        this.limit = limit;
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
}
