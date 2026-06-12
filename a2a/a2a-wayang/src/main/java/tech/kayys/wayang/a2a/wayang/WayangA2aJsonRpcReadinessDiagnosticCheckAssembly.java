package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcReadinessDiagnosticCheckAssembly(
        WayangA2aJsonRpcReadinessBaseProbeChecks baseProbeChecks,
        WayangA2aJsonRpcReadinessCoverageCheckRows coverageCheckRows,
        WayangA2aJsonRpcReadinessSpecAlignmentChecks specAlignmentChecks,
        WayangA2aJsonRpcReadinessOverallCheck overallCheck) {

    WayangA2aJsonRpcReadinessDiagnosticCheckAssembly {
        baseProbeChecks = Objects.requireNonNull(baseProbeChecks, "baseProbeChecks");
        coverageCheckRows = Objects.requireNonNull(coverageCheckRows, "coverageCheckRows");
        specAlignmentChecks = Objects.requireNonNull(specAlignmentChecks, "specAlignmentChecks");
        overallCheck = Objects.requireNonNull(overallCheck, "overallCheck");
    }

    static WayangA2aJsonRpcReadinessDiagnosticCheckAssembly from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment,
            WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {
        WayangA2aJsonRpcReadinessProbeResult resolvedReadiness =
                Objects.requireNonNull(readiness, "readiness");
        WayangA2aJsonRpcReadinessIssueBreakdown resolvedBreakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(resolvedReadiness)
                : breakdown;
        return new WayangA2aJsonRpcReadinessDiagnosticCheckAssembly(
                WayangA2aJsonRpcReadinessBaseProbeChecks.from(resolvedReadiness),
                WayangA2aJsonRpcReadinessCoverageCheckRows.from(resolvedReadiness, resolvedBreakdown),
                WayangA2aJsonRpcReadinessSpecAlignmentChecks.from(specAlignment),
                WayangA2aJsonRpcReadinessOverallCheck.from(resolvedReadiness, resolvedBreakdown));
    }

    List<Map<String, Object>> toMaps() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.addAll(baseProbeChecks.toMaps());
        checks.addAll(coverageCheckRows.toMaps());
        checks.addAll(specAlignmentChecks.toMaps());
        checks.add(overallCheck.toMap());
        return copyObjects(checks);
    }
}
