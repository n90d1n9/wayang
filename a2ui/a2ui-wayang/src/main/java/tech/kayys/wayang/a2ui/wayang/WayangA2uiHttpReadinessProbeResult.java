package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpReadinessProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpReadinessProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Composite operational readiness result for the A2UI HTTP surface.
 */
public record WayangA2uiHttpReadinessProbeResult(
        WayangA2uiHttpBindingReportProbeResult bindingReportProbe,
        WayangA2uiHttpActionBindingProbeResult actionBindingProbe,
        WayangA2uiHttpSmokeProbeResult smokeProbe,
        boolean smokeRequired) {

    public static final String READINESS_ID = "a2ui.http.readiness";

    public WayangA2uiHttpReadinessProbeResult(
            WayangA2uiHttpBindingReportProbeResult bindingReportProbe,
            WayangA2uiHttpSmokeProbeResult smokeProbe,
            boolean smokeRequired) {
        this(
                bindingReportProbe,
                WayangA2uiHttpActionBindingProbeResult.compatibilityFallback(),
                smokeProbe,
                smokeRequired);
    }

    public WayangA2uiHttpReadinessProbeResult {
        bindingReportProbe = Objects.requireNonNull(bindingReportProbe, "bindingReportProbe");
        actionBindingProbe = actionBindingProbe == null
                ? WayangA2uiHttpActionBindingProbeResult.compatibilityFallback()
                : actionBindingProbe;
        smokeProbe = smokeProbe == null ? WayangA2uiHttpSmokeProbeResult.empty() : smokeProbe;
    }

    public static WayangA2uiHttpReadinessProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        WayangA2uiHttpBindingReportProbeResult bindingReportProbe = resolved.bindingReportProbe();
        WayangA2uiHttpActionBindingProbeResult actionBindingProbe = resolved.actionBindingProbe();
        boolean smokeRequired = bindingReportProbe.requiresSmokeProbe();
        WayangA2uiHttpSmokeProbeResult smokeProbe = smokeRequired
                ? resolved.smokeProbe()
                : WayangA2uiHttpSmokeProbeResult.empty();
        return new WayangA2uiHttpReadinessProbeResult(
                bindingReportProbe,
                actionBindingProbe,
                smokeProbe,
                smokeRequired);
    }

    public static WayangA2uiHttpReadinessProbeResult empty() {
        return new WayangA2uiHttpReadinessProbeResult(
                WayangA2uiHttpBindingReportProbeResult.empty(),
                WayangA2uiHttpActionBindingProbeResult.compatibilityFallback(),
                WayangA2uiHttpSmokeProbeResult.empty(),
                false);
    }

    public static WayangA2uiHttpReadinessProbeResult from(WayangA2uiHttpResponse response) {
        return HttpReadinessProbeResponseDecoder.from(response);
    }

    public static WayangA2uiHttpReadinessProbeResult fromMap(Map<?, ?> values) {
        return WayangA2uiHttpReadinessProbeResultDecoder.fromMap(values);
    }

    public static WayangA2uiHttpReadinessProbeResult fromJson(String json) {
        return WayangA2uiHttpReadinessProbeResultDecoder.fromJson(json);
    }

    public boolean bindingReportPassed() {
        return bindingReportProbe.passed();
    }

    public boolean actionBindingPassed() {
        return actionBindingProbe.passed();
    }

    public boolean smokePassed() {
        return !smokeRequired || smokeProbe.passed();
    }

    public boolean passed() {
        return bindingReportPassed() && actionBindingPassed() && smokePassed();
    }

    public int exitCode() {
        return passed()
                ? WayangA2uiHttpSmokeResult.EXIT_SUCCESS
                : WayangA2uiHttpSmokeResult.EXIT_FAILURE;
    }

    public List<Map<String, Object>> issues() {
        return HttpReadinessProbeProjection.issues(this);
    }

    public int issueCount() {
        return issues().size();
    }

    public Map<String, Object> toMap() {
        return HttpReadinessProbeProjection.readiness(this);
    }

    public WayangReadinessReport standardReadiness() {
        return HttpReadinessProbeProjection.standardReadiness(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP readiness probe result");
    }

}
