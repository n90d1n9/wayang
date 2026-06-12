package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcReadinessIssueBreakdown(
        List<Map<String, Object>> readinessIssues,
        List<Map<String, Object>> bindingReportIssues,
        List<Map<String, Object>> diagnosticHandlerIssues,
        List<Map<String, Object>> methodDispatchIssues,
        List<Map<String, Object>> routeCatalogIssues,
        List<Map<String, Object>> smokeIssues,
        List<Map<String, Object>> issues) {

    WayangA2aJsonRpcReadinessIssueBreakdown {
        readinessIssues = copyObjects(readinessIssues);
        bindingReportIssues = copyObjects(bindingReportIssues);
        diagnosticHandlerIssues = copyObjects(diagnosticHandlerIssues);
        methodDispatchIssues = copyObjects(methodDispatchIssues);
        routeCatalogIssues = copyObjects(routeCatalogIssues);
        smokeIssues = copyObjects(smokeIssues);
        issues = copyObjects(issues);
    }

    static WayangA2aJsonRpcReadinessIssueBreakdown from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessIssueGroups groups =
                WayangA2aJsonRpcReadinessIssueGroups.from(resolved);

        return new WayangA2aJsonRpcReadinessIssueBreakdown(
                groups.readinessIssues(),
                groups.bindingReportIssues(),
                groups.diagnosticHandlerIssues(),
                groups.methodDispatchIssues(),
                groups.routeCatalogIssues(),
                groups.smokeIssues(),
                groups.wrappedIssues());
    }

    int issueCount() {
        return issues.size();
    }

    int readinessIssueCount() {
        return readinessIssues.size();
    }

    int bindingReportIssueCount() {
        return bindingReportIssues.size();
    }

    int diagnosticHandlerIssueCount() {
        return diagnosticHandlerIssues.size();
    }

    int methodDispatchIssueCount() {
        return methodDispatchIssues.size();
    }

    int routeCatalogIssueCount() {
        return routeCatalogIssues.size();
    }

    int smokeIssueCount() {
        return smokeIssues.size();
    }
}
