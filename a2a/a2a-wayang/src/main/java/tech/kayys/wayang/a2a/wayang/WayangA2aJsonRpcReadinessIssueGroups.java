package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcReadinessIssueGroups(
        List<Map<String, Object>> readinessIssues,
        List<Map<String, Object>> bindingReportIssues,
        List<Map<String, Object>> diagnosticHandlerIssues,
        List<Map<String, Object>> methodDispatchIssues,
        List<Map<String, Object>> routeCatalogIssues,
        List<Map<String, Object>> smokeIssues) {

    WayangA2aJsonRpcReadinessIssueGroups {
        readinessIssues = copyObjects(readinessIssues);
        bindingReportIssues = copyObjects(bindingReportIssues);
        diagnosticHandlerIssues = copyObjects(diagnosticHandlerIssues);
        methodDispatchIssues = copyObjects(methodDispatchIssues);
        routeCatalogIssues = copyObjects(routeCatalogIssues);
        smokeIssues = copyObjects(smokeIssues);
    }

    static WayangA2aJsonRpcReadinessIssueGroups from(WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessBindingReportIssueBuckets bindingReportBuckets =
                WayangA2aJsonRpcReadinessBindingReportIssueBuckets.from(
                        resolved.bindingReportProbe().issues());
        return new WayangA2aJsonRpcReadinessIssueGroups(
                resolved.issues(),
                bindingReportBuckets.bindingReportIssues(),
                bindingReportBuckets.diagnosticHandlerIssues(),
                bindingReportBuckets.methodDispatchIssues(),
                resolved.routeCatalogRequired() ? resolved.routeCatalogProbe().issues() : List.of(),
                resolved.smokeRequired() ? resolved.smokeProbe().summary().issues() : List.of());
    }

    List<WayangA2aJsonRpcReadinessIssueGroup> orderedGroups() {
        return List.of(
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_READINESS,
                        readinessIssues),
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                        bindingReportIssues),
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_DIAGNOSTIC_HANDLERS,
                        diagnosticHandlerIssues),
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH,
                        methodDispatchIssues),
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_ROUTE_CATALOG,
                        routeCatalogIssues),
                WayangA2aJsonRpcReadinessIssueCatalog.group(
                        WayangA2aJsonRpcReadinessIssueCatalog.PROBE_SMOKE,
                        smokeIssues));
    }

    List<Map<String, Object>> wrappedIssues() {
        return orderedGroups().stream()
                .flatMap(group -> group.wrappedIssues().stream())
                .toList();
    }
}
