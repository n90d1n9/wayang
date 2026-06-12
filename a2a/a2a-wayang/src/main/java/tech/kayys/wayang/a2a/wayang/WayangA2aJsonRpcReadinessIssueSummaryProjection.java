package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC readiness issue summaries.
 */
final class WayangA2aJsonRpcReadinessIssueSummaryProjection {

    private WayangA2aJsonRpcReadinessIssueSummaryProjection() {
    }

    static WayangA2aJsonRpcReadinessIssueSummary fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        if (copy.containsKey("bindingReportProbe")
                || copy.containsKey("routeCatalogProbe")
                || copy.containsKey("smokeProbe")) {
            return WayangA2aJsonRpcReadinessIssueSummary.from(
                    WayangA2aJsonRpcReadinessProbeResult.fromMap(copy));
        }
        return new WayangA2aJsonRpcReadinessIssueSummary(
                bool(copy.get("passed"), false),
                number(copy.get("exitCode"), WayangA2aJsonRpcSmokeResult.EXIT_FAILURE),
                bool(copy.get("bindingReportPassed"), false),
                bool(copy.get("routeCatalogRequired"), false),
                bool(copy.get("routeCatalogPassed"), false),
                bool(copy.get("smokeRequired"), false),
                bool(copy.get("smokePassed"), false),
                number(copy.get("issueCount"), 0),
                number(copy.get("readinessIssueCount"), 0),
                number(copy.get("bindingReportIssueCount"), 0),
                number(copy.get("diagnosticHandlerIssueCount"), 0),
                number(copy.get("methodDispatchIssueCount"), 0),
                number(copy.get("routeCatalogIssueCount"), 0),
                number(copy.get("smokeIssueCount"), 0),
                WayangA2aMaps.objectList(copy.get("issues")));
    }

    static Map<String, Object> summary(WayangA2aJsonRpcReadinessIssueSummary summary) {
        WayangA2aJsonRpcReadinessIssueSummary resolved = Objects.requireNonNull(summary, "summary");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", resolved.passed());
        values.put("exitCode", resolved.exitCode());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("routeCatalogRequired", resolved.routeCatalogRequired());
        values.put("routeCatalogPassed", resolved.routeCatalogPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        values.put("issueCount", resolved.issueCount());
        values.put("readinessIssueCount", resolved.readinessIssueCount());
        values.put("bindingReportIssueCount", resolved.bindingReportIssueCount());
        if (resolved.diagnosticHandlerIssueCount() > 0) {
            values.put("diagnosticHandlerIssueCount", resolved.diagnosticHandlerIssueCount());
        }
        if (resolved.methodDispatchIssueCount() > 0) {
            values.put("methodDispatchIssueCount", resolved.methodDispatchIssueCount());
        }
        values.put("routeCatalogIssueCount", resolved.routeCatalogIssueCount());
        values.put("smokeIssueCount", resolved.smokeIssueCount());
        values.put("issues", resolved.issues());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcReadinessIssueSummary summary) {
        WayangA2aJsonRpcReadinessIssueSummary resolved = Objects.requireNonNull(summary, "summary");
        return WayangA2aJsonRpcHttpResponses.json(
                WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY,
                resolved.toJson());
    }
}
