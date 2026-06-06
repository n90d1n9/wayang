package tech.kayys.wayang.a2ui.wayang;

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
final class WayangA2uiHttpReadinessProbeProjection {

    private WayangA2uiHttpReadinessProbeProjection() {
    }

    static Map<String, Object> readiness(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", issues(resolved));
        values.put("bindingReportProbe", resolved.bindingReportProbe().toMap());
        values.put("smokeProbe", resolved.smokeProbe().toMap());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static WayangReadinessReport standardReadiness(WayangA2uiHttpReadinessProbeResult result) {
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

    static List<Map<String, Object>> issues(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        List<Map<String, Object>> issues = new ArrayList<>();
        if (!resolved.bindingReportPassed()) {
            issues.add(issue(
                    "binding_report_probe_failed",
                    "A2UI HTTP binding report probe did not pass.",
                    resolved.bindingReportProbe().statusCode(),
                    resolved.bindingReportProbe().routeOperation()));
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
                        probeAttributes(
                                result.bindingReportProbe().statusCode(),
                                result.bindingReportProbe().routeOperation())),
                WayangReadinessReports.probe(
                        "smoke",
                        result.smokeRequired(),
                        result.smokePassed(),
                        result.smokeRequired() ? result.smokeProbe().summary().issueCount() : 0,
                        probeAttributes(result.smokeProbe().statusCode(), result.smokeProbe().routeOperation())));
    }

    private static Map<String, Object> standardReadinessAttributes(
            WayangA2uiHttpReadinessProbeResult result) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingReportPassed", result.bindingReportPassed());
        values.put("smokeRequired", result.smokeRequired());
        values.put("smokePassed", result.smokePassed());
        return WayangA2uiTransportMaps.freeze(values);
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
        if (result.smokeRequired() && !result.smokeProbe().passed()) {
            issues.add(standardIssue(
                    "smoke_probe_failed",
                    "A2UI HTTP smoke probe did not pass.",
                    result.smokeProbe().statusCode(),
                    result.smokeProbe().routeOperation()));
        }
        return List.copyOf(issues);
    }

    private static Map<String, Object> probeAttributes(int statusCode, String routeOperation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", Math.max(0, statusCode));
        values.put("routeOperation", routeOperation == null ? "" : routeOperation);
        return WayangA2uiTransportMaps.freeze(values);
    }

    private static Map<String, Object> issue(
            String code,
            String message,
            int statusCode,
            String routeOperation) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("code", code);
        issue.put("message", message);
        issue.put("statusCode", Math.max(0, statusCode));
        issue.put("routeOperation", routeOperation == null ? "" : routeOperation);
        return WayangA2uiTransportMaps.freeze(issue);
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
                probeAttributes(statusCode, routeOperation));
    }
}
