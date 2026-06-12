package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

record WayangA2aJsonRpcDiagnosticsReportStatus(
        boolean passed,
        int exitCode) {

    WayangA2aJsonRpcDiagnosticsReportStatus {
        exitCode = Math.max(0, exitCode);
    }

    static WayangA2aJsonRpcDiagnosticsReportStatus from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aJsonRpcReadinessProbeResult resolvedReadiness =
                Objects.requireNonNull(readiness, "readiness");
        WayangA2aSpecAlignmentSnapshot resolvedSpecAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        return from(resolvedReadiness.passed() && resolvedSpecAlignment.aligned());
    }

    static WayangA2aJsonRpcDiagnosticsReportStatus from(boolean passed) {
        return new WayangA2aJsonRpcDiagnosticsReportStatus(
                passed,
                passed ? WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS : WayangA2aJsonRpcSmokeResult.EXIT_FAILURE);
    }
}
