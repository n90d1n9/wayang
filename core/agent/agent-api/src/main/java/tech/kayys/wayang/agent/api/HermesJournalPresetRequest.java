package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for Hermes runtime journal preset inspections.
 */
public final class HermesJournalPresetRequest {

    private int limit;

    public HermesJournalPresetRequest() {
        this(0);
    }

    HermesJournalPresetRequest(int limit) {
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }

    @QueryParam("limit")
    public void setLimit(int limit) {
        this.limit = limit;
    }
}
