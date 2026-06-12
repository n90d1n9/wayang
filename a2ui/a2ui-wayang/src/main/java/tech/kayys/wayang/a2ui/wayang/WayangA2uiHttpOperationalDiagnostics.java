package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpOperationalDiagnosticsProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Single A2UI HTTP operational diagnostics view backed by readiness probes.
 */
public record WayangA2uiHttpOperationalDiagnostics(
        WayangA2uiHttpReadinessProbeResult readinessProbe) {

    public static final String DIAGNOSTICS_ID = "a2ui.http.operational-diagnostics";

    public WayangA2uiHttpOperationalDiagnostics {
        readinessProbe = Objects.requireNonNull(readinessProbe, "readinessProbe");
    }

    public static WayangA2uiHttpOperationalDiagnostics run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return new WayangA2uiHttpOperationalDiagnostics(resolved.readinessProbe());
    }

    public static WayangA2uiHttpOperationalDiagnostics empty() {
        return new WayangA2uiHttpOperationalDiagnostics(WayangA2uiHttpReadinessProbeResult.empty());
    }

    public static WayangA2uiHttpOperationalDiagnostics fromMap(Map<?, ?> values) {
        return WayangA2uiHttpOperationalDiagnosticsDecoder.fromMap(values);
    }

    public static WayangA2uiHttpOperationalDiagnostics fromJson(String json) {
        return WayangA2uiHttpOperationalDiagnosticsDecoder.fromJson(json);
    }

    public WayangA2uiHttpBindingReportProbeResult bindingReportProbe() {
        return readinessProbe.bindingReportProbe();
    }

    public WayangA2uiHttpActionBindingProbeResult actionBindingProbe() {
        return readinessProbe.actionBindingProbe();
    }

    public WayangA2uiHttpSmokeProbeResult smokeProbe() {
        return readinessProbe.smokeProbe();
    }

    public boolean bindingReportPassed() {
        return readinessProbe.bindingReportPassed();
    }

    public boolean actionBindingPassed() {
        return readinessProbe.actionBindingPassed();
    }

    public boolean smokeRequired() {
        return readinessProbe.smokeRequired();
    }

    public boolean smokePassed() {
        return readinessProbe.smokePassed();
    }

    public boolean passed() {
        return readinessProbe.passed();
    }

    public int exitCode() {
        return readinessProbe.exitCode();
    }

    public List<Map<String, Object>> issues() {
        return readinessProbe.issues();
    }

    public int issueCount() {
        return readinessProbe.issueCount();
    }

    public WayangReadinessReport standardReadiness() {
        return readinessProbe.standardReadiness();
    }

    public WayangA2uiHttpOperationalDiagnosticsSummary summary() {
        return WayangA2uiHttpOperationalDiagnosticsSummary.from(this);
    }

    public Map<String, Object> toMap() {
        return HttpOperationalDiagnosticsProjection.diagnostics(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP operational diagnostics");
    }
}
