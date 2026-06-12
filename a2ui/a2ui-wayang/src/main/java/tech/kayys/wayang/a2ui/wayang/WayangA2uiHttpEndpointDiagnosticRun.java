package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import java.util.Map;
import java.util.Objects;

/**
 * Adapter-facing envelope for one mounted endpoint diagnostics execution.
 */
public record WayangA2uiHttpEndpointDiagnosticRun(
        WayangA2uiHttpEndpointDiagnosticResult result) {

    public WayangA2uiHttpEndpointDiagnosticRun {
        result = Objects.requireNonNull(result, "result");
    }

    public String diagnosticsId() {
        return result.diagnosticsId();
    }

    public WayangA2uiHttpEndpointDiagnosticReport report() {
        return result.report();
    }

    public WayangA2uiHttpEndpointDiagnosticSummary summary() {
        return result.summary();
    }

    public boolean passed() {
        return summary().passed();
    }

    public int exitCode() {
        return summary().exitCode();
    }

    public String reportJson() {
        return report().toJson();
    }

    public String summaryJson() {
        return summary().toJson();
    }

    public Map<String, Object> toMap() {
        return HttpEndpointDiagnosticProjection.run(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic run");
    }
}
