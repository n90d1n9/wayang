package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP readiness probe results.
 */
public final class HttpReadinessProbeProjection {

    private HttpReadinessProbeProjection() {
    }

    public static Map<String, Object> readiness(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("actionBindingPassed", resolved.actionBindingPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", issues(resolved));
        values.put("bindingReportProbe", resolved.bindingReportProbe().toMap());
        values.put("actionBindingProbe", resolved.actionBindingProbe().toMap());
        values.put("smokeProbe", resolved.smokeProbe().toMap());
        return TransportMaps.freeze(values);
    }

    public static WayangReadinessReport standardReadiness(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        return WayangReadinessReport.from(
                WayangA2uiHttpReadinessProbeResult.READINESS_ID,
                resolved.passed(),
                WayangReadinessReports.exitCode(resolved.passed()),
                resolved.issueCount(),
                standardReadinessProbes(resolved),
                standardReadinessIssues(resolved),
                standardReadinessAttributes(resolved));
    }

    public static List<Map<String, Object>> issues(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        List<Map<String, Object>> issues = new ArrayList<>();
        if (!resolved.bindingReportPassed()) {
            issues.add(issue(
                    "binding_report_probe_failed",
                    "A2UI HTTP binding report probe did not pass.",
                    resolved.bindingReportProbe().statusCode(),
                    resolved.bindingReportProbe().routeOperation()));
        }
        if (!resolved.actionBindingPassed()) {
            issues.add(issue(
                    "action_binding_probe_failed",
                    "A2UI action binding probe did not pass.",
                    resolved.actionBindingProbe().statusCode(),
                    resolved.actionBindingProbe().routeOperation()));
        }
        if (resolved.smokeRequired() && !resolved.smokeProbe().passed()) {
            issues.add(issue(
                    "smoke_probe_failed",
                    "A2UI HTTP smoke probe did not pass.",
                    resolved.smokeProbe().statusCode(),
                    resolved.smokeProbe().routeOperation()));
        }
        return List.copyOf(issues);
    }

    private static List<Map<String, Object>> standardReadinessProbes(
            WayangA2uiHttpReadinessProbeResult result) {
        return List.of(
                WayangReadinessReports.probe(
                        "bindingReport",
                        true,
                        result.bindingReportPassed(),
                        result.bindingReportProbe().issueCount(),
                        HttpIssueMaps.probeAttributes(
                                result.bindingReportProbe().statusCode(),
                                result.bindingReportProbe().routeOperation())),
                WayangReadinessReports.probe(
                        "actionBinding",
                        true,
                        result.actionBindingPassed(),
                        result.actionBindingProbe().issueCount(),
                        HttpIssueMaps.probeAttributes(
                                result.actionBindingProbe().statusCode(),
                                result.actionBindingProbe().routeOperation())),
                WayangReadinessReports.probe(
                        "smoke",
                        result.smokeRequired(),
                        result.smokePassed(),
                        result.smokeRequired() ? result.smokeProbe().summary().issueCount() : 0,
                        HttpIssueMaps.probeAttributes(
                                result.smokeProbe().statusCode(),
                                result.smokeProbe().routeOperation())));
    }

    private static Map<String, Object> standardReadinessAttributes(
            WayangA2uiHttpReadinessProbeResult result) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingReportPassed", result.bindingReportPassed());
        values.put("actionBindingPassed", result.actionBindingPassed());
        values.put("smokeRequired", result.smokeRequired());
        values.put("smokePassed", result.smokePassed());
        return TransportMaps.freeze(values);
    }

    private static List<Map<String, Object>> standardReadinessIssues(
            WayangA2uiHttpReadinessProbeResult result) {
        List<Map<String, Object>> issues = new ArrayList<>();
        if (!result.bindingReportPassed()) {
            issues.add(standardIssue(
                    "binding_report_probe_failed",
                    "A2UI HTTP binding report probe did not pass.",
                    result.bindingReportProbe().statusCode(),
                    result.bindingReportProbe().routeOperation()));
        }
        if (!result.actionBindingPassed()) {
            issues.add(standardIssue(
                    "action_binding_probe_failed",
                    "A2UI action binding probe did not pass.",
                    result.actionBindingProbe().statusCode(),
                    result.actionBindingProbe().routeOperation()));
        }
        if (result.smokeRequired() && !result.smokeProbe().passed()) {
            issues.add(standardIssue(
                    "smoke_probe_failed",
                    "A2UI HTTP smoke probe did not pass.",
                    result.smokeProbe().statusCode(),
                    result.smokeProbe().routeOperation()));
        }
        return List.copyOf(issues);
    }

    private static Map<String, Object> issue(
            String code,
            String message,
            int statusCode,
            String routeOperation) {
        return HttpIssueMaps.probeFailure(code, message, statusCode, routeOperation);
    }

    private static Map<String, Object> standardIssue(
            String code,
            String message,
            int statusCode,
            String routeOperation) {
        return WayangReadinessReports.issue(
                code,
                "http",
                message,
                HttpIssueMaps.probeAttributes(statusCode, routeOperation));
    }
}
