package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcReadinessBaseProbeChecks(WayangA2aJsonRpcReadinessProbeResult readiness) {

    WayangA2aJsonRpcReadinessBaseProbeChecks {
        readiness = Objects.requireNonNull(readiness, "readiness");
    }

    static WayangA2aJsonRpcReadinessBaseProbeChecks from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return new WayangA2aJsonRpcReadinessBaseProbeChecks(readiness);
    }

    List<WayangA2aJsonRpcReadinessProbeCheck> checks() {
        return List.of(
                bindingReport(),
                routeCatalog(),
                smoke());
    }

    List<Map<String, Object>> issues() {
        return checks().stream()
                .filter(WayangA2aJsonRpcReadinessProbeCheck::failed)
                .map(WayangA2aJsonRpcReadinessProbeCheck::failureIssue)
                .toList();
    }

    List<Map<String, Object>> toMaps() {
        return checks().stream()
                .map(WayangA2aJsonRpcReadinessProbeCheck::toMap)
                .toList();
    }

    private WayangA2aJsonRpcReadinessProbeCheck bindingReport() {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                true,
                readiness.bindingReportPassed(),
                readiness.bindingReportProbe().statusCode(),
                readiness.bindingReportProbe().routeOperation(),
                readiness.bindingReportProbe().issueCount(),
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_BINDING_REPORT_PROBE_FAILED,
                "A2A JSON-RPC binding report probe did not pass.");
    }

    private WayangA2aJsonRpcReadinessProbeCheck routeCatalog() {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_ROUTE_CATALOG,
                readiness.routeCatalogRequired(),
                readiness.routeCatalogPassed(),
                readiness.routeCatalogProbe().statusCode(),
                readiness.routeCatalogProbe().routeOperation(),
                readiness.routeCatalogProbe().issueCount(),
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_ROUTE_CATALOG_PROBE_FAILED,
                "A2A JSON-RPC route catalog probe did not pass.");
    }

    private WayangA2aJsonRpcReadinessProbeCheck smoke() {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_SMOKE,
                readiness.smokeRequired(),
                readiness.smokePassed(),
                readiness.smokeProbe().statusCode(),
                readiness.smokeProbe().routeOperation(),
                readiness.smokeRequired() ? readiness.smokeProbe().summary().issueCount() : 0,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_SMOKE_PROBE_FAILED,
                "A2A JSON-RPC smoke probe did not pass.");
    }
}
