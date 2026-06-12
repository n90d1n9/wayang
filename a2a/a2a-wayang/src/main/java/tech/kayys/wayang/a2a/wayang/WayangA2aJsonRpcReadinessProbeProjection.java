package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReports;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC readiness probe envelopes.
 */
final class WayangA2aJsonRpcReadinessProbeProjection {

    private WayangA2aJsonRpcReadinessProbeProjection() {
    }

    static Map<String, Object> probe(WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessMethodDispatchSnapshot methodDispatch = methodDispatchSnapshot(resolved);
        List<Map<String, Object>> issues = resolved.issues();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", resolved.passed());
        values.put("exitCode", resolved.exitCode());
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("routeCatalogRequired", resolved.routeCatalogRequired());
        values.put("routeCatalogPassed", resolved.routeCatalogPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        if (methodDispatch.reported()) {
            values.put("methodDispatch", methodDispatch.toMap());
        }
        if (resolved.methodRegistryReported()) {
            values.put("methodRegistry", resolved.methodRegistrySnapshot().toMap());
        }
        values.put("issueCount", issues.size());
        values.put("issues", issues);
        values.put("bindingReportProbe", resolved.bindingReportProbe().toMap());
        values.put("routeCatalogProbe", resolved.routeCatalogProbe().toMap());
        values.put("smokeProbe", resolved.smokeProbe().toMap());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangReadinessReport standardReadiness(WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessBaseProbeChecks checks = WayangA2aJsonRpcReadinessBaseProbeChecks.from(resolved);
        List<Map<String, Object>> issues = standardIssues(checks);
        return WayangReadinessReport.from(
                WayangA2aJsonRpcReadinessProbeResult.READINESS_ID,
                resolved.passed(),
                WayangReadinessReports.exitCode(resolved.passed()),
                issues.size(),
                standardProbes(checks),
                issues,
                standardAttributes(resolved));
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        return WayangA2aJsonRpcHttpResponses.jsonWithProtocolVersion(
                WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS,
                resolved.bindingReportProbe().protocolVersion(),
                resolved.toJson());
    }

    static Map<String, Object> standardAttributes(WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessMethodDispatchSnapshot methodDispatch = methodDispatchSnapshot(resolved);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingReportPassed", resolved.bindingReportPassed());
        values.put("routeCatalogRequired", resolved.routeCatalogRequired());
        values.put("routeCatalogPassed", resolved.routeCatalogPassed());
        values.put("smokeRequired", resolved.smokeRequired());
        values.put("smokePassed", resolved.smokePassed());
        if (methodDispatch.reported()) {
            values.put("methodDispatch", methodDispatch.toMap());
        }
        if (resolved.methodRegistryReported()) {
            values.put("methodRegistry", resolved.methodRegistrySnapshot().toMap());
        }
        return WayangA2aMaps.copyMap(values);
    }

    private static WayangA2aJsonRpcReadinessMethodDispatchSnapshot methodDispatchSnapshot(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return WayangA2aJsonRpcReadinessMethodDispatchSnapshot.from(readiness.bindingReportProbe());
    }

    private static List<Map<String, Object>> standardProbes(WayangA2aJsonRpcReadinessBaseProbeChecks checks) {
        return checks.checks().stream()
                .map(WayangA2aJsonRpcReadinessProbeProjection::standardProbe)
                .toList();
    }

    private static Map<String, Object> standardProbe(WayangA2aJsonRpcReadinessProbeCheck check) {
        return WayangReadinessReports.probe(
                check.probe(),
                check.required(),
                check.passed(),
                check.issueCount(),
                probeAttributes(check));
    }

    private static List<Map<String, Object>> standardIssues(WayangA2aJsonRpcReadinessBaseProbeChecks checks) {
        return checks.checks().stream()
                .filter(WayangA2aJsonRpcReadinessProbeCheck::failed)
                .map(WayangA2aJsonRpcReadinessProbeProjection::standardIssue)
                .toList();
    }

    private static Map<String, Object> standardIssue(WayangA2aJsonRpcReadinessProbeCheck check) {
        return WayangReadinessReports.issue(
                check.failureCode(),
                "http",
                check.failureMessage(),
                probeAttributes(check));
    }

    private static Map<String, Object> probeAttributes(WayangA2aJsonRpcReadinessProbeCheck check) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", check.statusCode());
        values.put("routeOperation", check.routeOperation());
        return WayangA2aMaps.copyMap(values);
    }
}
