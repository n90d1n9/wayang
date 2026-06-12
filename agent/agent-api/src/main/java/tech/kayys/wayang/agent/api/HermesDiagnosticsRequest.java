package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.QueryParam;

/**
 * Query parameters for Hermes runtime diagnostics inspection.
 */
public final class HermesDiagnosticsRequest {

    private String view;

    public HermesDiagnosticsRequest() {
        this(null);
    }

    HermesDiagnosticsRequest(String view) {
        this.view = view;
    }

    public String view() {
        return view;
    }

    @QueryParam("view")
    public void setView(String view) {
        this.view = view;
    }
}
