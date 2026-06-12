package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

record WayangA2aJsonRpcDiagnosticsReportContext(
        WayangA2aJsonRpcReadinessProbeResult readiness,
        WayangA2aJsonRpcHttpConfig config,
        WayangA2aSpecAlignmentSnapshot specAlignment,
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown,
        WayangA2aJsonRpcReadinessIssueSummary summary) {

    WayangA2aJsonRpcDiagnosticsReportContext {
        readiness = Objects.requireNonNull(readiness, "readiness");
        specAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        breakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness)
                : breakdown;
        summary = summary == null
                ? WayangA2aJsonRpcReadinessIssueSummaryBuilder.from(readiness, breakdown).build()
                : summary;
    }

    static WayangA2aJsonRpcDiagnosticsReportContext from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return new WayangA2aJsonRpcDiagnosticsReportContext(readiness, config, specAlignment, null, null);
    }
}
