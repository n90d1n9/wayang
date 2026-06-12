package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

record WayangA2aJsonRpcReadinessIssueSummaryBuilder(
        WayangA2aJsonRpcReadinessProbeResult readiness,
        WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {

    WayangA2aJsonRpcReadinessIssueSummaryBuilder {
        readiness = Objects.requireNonNull(readiness, "readiness");
        breakdown = breakdown == null
                ? WayangA2aJsonRpcReadinessIssueBreakdown.from(readiness)
                : breakdown;
    }

    static WayangA2aJsonRpcReadinessIssueSummaryBuilder from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        return new WayangA2aJsonRpcReadinessIssueSummaryBuilder(readiness, null);
    }

    static WayangA2aJsonRpcReadinessIssueSummaryBuilder from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcReadinessIssueBreakdown breakdown) {
        return new WayangA2aJsonRpcReadinessIssueSummaryBuilder(readiness, breakdown);
    }

    WayangA2aJsonRpcReadinessIssueSummary build() {
        return new WayangA2aJsonRpcReadinessIssueSummary(
                readiness.passed(),
                readiness.exitCode(),
                readiness.bindingReportPassed(),
                readiness.routeCatalogRequired(),
                readiness.routeCatalogPassed(),
                readiness.smokeRequired(),
                readiness.smokePassed(),
                breakdown.issueCount(),
                breakdown.readinessIssueCount(),
                breakdown.bindingReportIssueCount(),
                breakdown.diagnosticHandlerIssueCount(),
                breakdown.methodDispatchIssueCount(),
                breakdown.routeCatalogIssueCount(),
                breakdown.smokeIssueCount(),
                breakdown.issues());
    }
}
