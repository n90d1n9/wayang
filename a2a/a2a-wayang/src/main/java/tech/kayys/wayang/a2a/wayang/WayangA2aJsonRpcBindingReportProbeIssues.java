package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcProbeResponseChecks.countMissingIssue;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcProbeResponseChecks.responseIssues;

record WayangA2aJsonRpcBindingReportProbeIssues(
        List<Map<String, Object>> issues) {

    WayangA2aJsonRpcBindingReportProbeIssues {
        issues = copyObjects(issues);
    }

    static WayangA2aJsonRpcBindingReportProbeIssues from(
            WayangA2aHttpResponse response,
            int methodCount,
            List<WayangA2aJsonRpcBindingReportSection> requiredSections,
            WayangA2aJsonRpcHttpDiagnosticHandlerCoverage diagnosticHandlerCoverage,
            WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage) {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage diagnosticCoverage =
                diagnosticHandlerCoverage == null
                        ? WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(Map.of())
                        : diagnosticHandlerCoverage;
        List<Map<String, Object>> values = new ArrayList<>(responseIssues(
                Objects.requireNonNull(response, "response"),
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_BINDING_REPORT_ROUTE_MISMATCH,
                "binding report response"));
        if (methodCount <= 0) {
            values.add(countMissingIssue(
                    WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                    WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_COUNT_MISSING,
                    "methodCount",
                    methodCount,
                    "binding report",
                    "methods"));
        }
        sections(requiredSections).stream()
                .filter(WayangA2aJsonRpcBindingReportSection::pathMissing)
                .map(WayangA2aJsonRpcBindingReportSection::missingPathIssue)
                .forEach(values::add);
        if (!diagnosticCoverage.complete()) {
            values.add(diagnosticCoverageIssue(diagnosticCoverage));
        }
        values.addAll(WayangA2aJsonRpcMethodDispatchIssues.from(methodDispatchCoverage));
        return new WayangA2aJsonRpcBindingReportProbeIssues(values);
    }

    int issueCount() {
        return issues.size();
    }

    private static List<WayangA2aJsonRpcBindingReportSection> sections(
            List<WayangA2aJsonRpcBindingReportSection> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, Object> diagnosticCoverageIssue(
            WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage) {
        return issue(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                "true",
                "missing=" + coverage.missingHandlerKeys() + ", orphan=" + coverage.orphanHandlerKeys(),
                "A2A JSON-RPC binding report diagnostic handler coverage was incomplete.");
    }
}
