package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcDiagnosticsReportIssues(
        List<Map<String, Object>> issues) {

    WayangA2aJsonRpcDiagnosticsReportIssues {
        issues = copyObjects(issues);
    }

    static WayangA2aJsonRpcDiagnosticsReportIssues from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return from(
                WayangA2aJsonRpcReadinessIssueSummary.from(
                        Objects.requireNonNull(readiness, "readiness")),
                specAlignment);
    }

    static WayangA2aJsonRpcDiagnosticsReportIssues from(
            WayangA2aJsonRpcReadinessIssueSummary summary,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aJsonRpcReadinessIssueSummary resolvedSummary =
                Objects.requireNonNull(summary, "summary");
        WayangA2aSpecAlignmentSnapshot resolvedSpecAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        List<Map<String, Object>> values = new ArrayList<>(resolvedSummary.issues());
        if (!resolvedSpecAlignment.aligned()) {
            values.add(WayangA2aSpecAlignmentDiagnosticIssues.gapIssue(resolvedSpecAlignment));
        }
        return new WayangA2aJsonRpcDiagnosticsReportIssues(values);
    }

    int issueCount() {
        return issues.size();
    }
}
