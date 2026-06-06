package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Composite operational readiness result for the A2UI HTTP surface.
 */
public record WayangA2uiHttpReadinessProbeResult(
        WayangA2uiHttpBindingReportProbeResult bindingReportProbe,
        WayangA2uiHttpSmokeProbeResult smokeProbe,
        boolean smokeRequired) {

    public static final String READINESS_ID = "a2ui.http.readiness";

    public WayangA2uiHttpReadinessProbeResult {
        bindingReportProbe = Objects.requireNonNull(bindingReportProbe, "bindingReportProbe");
        smokeProbe = smokeProbe == null ? emptySmokeProbe() : smokeProbe;
    }

    public static WayangA2uiHttpReadinessProbeResult run(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        WayangA2uiHttpBindingReportProbeResult bindingReportProbe = resolved.bindingReportProbe();
        boolean smokeRequired = bindingReportProbe.routeOperations().contains(WayangA2uiHttpRoute.OPERATION_SMOKE);
        WayangA2uiHttpSmokeProbeResult smokeProbe = smokeRequired ? resolved.smokeProbe() : emptySmokeProbe();
        return new WayangA2uiHttpReadinessProbeResult(bindingReportProbe, smokeProbe, smokeRequired);
    }

    public static WayangA2uiHttpReadinessProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        return fromMap(readinessBody(resolved.body()));
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

    public boolean smokePassed() {
        return !smokeRequired || smokeProbe.passed();
    }

    public boolean passed() {
        return bindingReportPassed() && smokePassed();
    }

    public int exitCode() {
        return passed()
                ? WayangA2uiHttpSmokeResult.EXIT_SUCCESS
                : WayangA2uiHttpSmokeResult.EXIT_FAILURE;
    }

    public List<Map<String, Object>> issues() {
        return WayangA2uiHttpReadinessProbeProjection.issues(this);
    }

    public int issueCount() {
        return issues().size();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpReadinessProbeProjection.readiness(this);
    }

    public WayangReadinessReport standardReadiness() {
        return WayangA2uiHttpReadinessProbeProjection.standardReadiness(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP readiness probe result");
    }

    private static WayangA2uiHttpSmokeProbeResult emptySmokeProbe() {
        return new WayangA2uiHttpSmokeProbeResult(
                0,
                false,
                "",
                "",
                "",
                new WayangA2uiHttpSmokeSummary(
                        false,
                        WayangA2uiHttpSmokeResult.EXIT_FAILURE,
                        "",
                        0,
                        0,
                        0,
                        false,
                        List.of(),
                        Map.of(),
                        Map.of()),
                Map.of());
    }

    private static Map<String, Object> readinessBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            WayangA2uiTransportResponse transport = WayangA2uiTransportResponse.fromJson(body);
            return bodyMap(transport.body());
        } catch (IllegalArgumentException ignored) {
            return bodyMap(body);
        }
    }

    private static Map<String, Object> bodyMap(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return WayangA2uiTransportJson.map(
                    body,
                    "A2UI HTTP readiness probe result JSON must not be blank",
                    "Unable to decode A2UI HTTP readiness probe result JSON");
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

}
