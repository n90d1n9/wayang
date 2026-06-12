package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.probeIssue;

record WayangA2aJsonRpcReadinessProbeCheck(
        String probe,
        boolean required,
        boolean passed,
        int statusCode,
        String routeOperation,
        int issueCount,
        String failureCode,
        String failureMessage) {

    WayangA2aJsonRpcReadinessProbeCheck {
        probe = probe == null ? "" : probe.trim();
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        issueCount = Math.max(0, issueCount);
        failureCode = failureCode == null ? "" : failureCode.trim();
        failureMessage = failureMessage == null ? "" : failureMessage.trim();
    }

    static List<WayangA2aJsonRpcReadinessProbeCheck> probeChecks(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return WayangA2aJsonRpcReadinessBaseProbeChecks.from(readiness).checks();
    }

    static List<Map<String, Object>> issues(WayangA2aJsonRpcReadinessProbeResult readiness) {
        return WayangA2aJsonRpcReadinessBaseProbeChecks.from(readiness).issues();
    }

    static List<Map<String, Object>> diagnosticChecks(WayangA2aJsonRpcReadinessProbeResult readiness) {
        return WayangA2aJsonRpcReadinessDiagnosticChecks.from(readiness).toMaps();
    }

    static List<Map<String, Object>> diagnosticChecks(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return WayangA2aJsonRpcReadinessDiagnosticChecks.from(readiness, specAlignment).toMaps();
    }

    boolean failed() {
        return required && !passed;
    }

    Map<String, Object> failureIssue() {
        return probeIssue(failureCode, failureMessage, statusCode, routeOperation);
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("probe", probe);
        values.put("required", required);
        values.put("passed", passed);
        values.put("statusCode", statusCode);
        values.put("routeOperation", routeOperation);
        values.put("issueCount", issueCount);
        return WayangA2aMaps.copyMap(values);
    }

}
