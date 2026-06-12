package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcReadinessDiagnosticChecks(
        WayangA2aJsonRpcReadinessProbeResult readiness,
        WayangA2aSpecAlignmentSnapshot specAlignment,
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {

    WayangA2aJsonRpcReadinessDiagnosticChecks {
        readiness = Objects.requireNonNull(readiness, "readiness");
        specAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        breakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness)
                : breakdown;
    }

    static WayangA2aJsonRpcReadinessDiagnosticChecks from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return new WayangA2aJsonRpcReadinessDiagnosticChecks(readiness, null, null);
    }

    static WayangA2aJsonRpcReadinessDiagnosticChecks from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return new WayangA2aJsonRpcReadinessDiagnosticChecks(readiness, specAlignment, null);
    }

    static WayangA2aJsonRpcReadinessDiagnosticChecks from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment,
            WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {
        return new WayangA2aJsonRpcReadinessDiagnosticChecks(readiness, specAlignment, breakdown);
    }

    List<Map<String, Object>> toMaps() {
        return WayangA2aJsonRpcReadinessDiagnosticCheckAssembly.from(
                        readiness,
                        specAlignment,
                        breakdown)
                .toMaps();
    }
}
