package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcReadinessCoverageCheckRows(
        List<WayangA2aJsonRpcReadinessProbeCheck> rows) {

    WayangA2aJsonRpcReadinessCoverageCheckRows {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    static WayangA2aJsonRpcReadinessCoverageCheckRows from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        return from(resolved, WayangA2aJsonRpcReadinessIssueBreakdown.from(resolved));
    }

    static WayangA2aJsonRpcReadinessCoverageCheckRows from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessIssueBreakdown resolvedBreakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(resolved)
                : breakdown;
        List<WayangA2aJsonRpcReadinessProbeCheck> rows = new ArrayList<>();
        if (resolvedBreakdown.diagnosticHandlerIssueCount() > 0) {
            rows.add(row(
                    WayangA2aJsonRpcReadinessIssueCatalog.PROBE_DIAGNOSTIC_HANDLERS,
                    resolved,
                    false,
                    resolvedBreakdown.diagnosticHandlerIssueCount()));
        }
        if (resolved.methodDispatchReported()) {
            rows.add(row(
                    WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH,
                    resolved,
                    resolved.methodDispatchPassed(),
                    resolved.methodDispatchPassed() ? 0 : resolvedBreakdown.methodDispatchIssueCount()));
        }
        if (resolved.methodRegistryReported()) {
            rows.add(row(
                    WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_REGISTRY,
                    resolved,
                    resolved.methodRegistryPassed(),
                    0));
        }
        return new WayangA2aJsonRpcReadinessCoverageCheckRows(rows);
    }

    List<Map<String, Object>> toMaps() {
        return rows.stream()
                .map(WayangA2aJsonRpcReadinessProbeCheck::toMap)
                .toList();
    }

    private static WayangA2aJsonRpcReadinessProbeCheck row(
            String probe,
            WayangA2aJsonRpcReadinessProbeResult readiness,
            boolean passed,
            int issueCount) {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                probe,
                true,
                passed,
                readiness.bindingReportProbe().statusCode(),
                readiness.bindingReportProbe().routeOperation(),
                issueCount,
                "",
                "");
    }
}
