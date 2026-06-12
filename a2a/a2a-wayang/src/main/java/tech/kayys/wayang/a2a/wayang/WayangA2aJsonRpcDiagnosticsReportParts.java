package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcDiagnosticsReportParts(
        WayangA2aJsonRpcDiagnosticsReportContext context,
        WayangA2aJsonRpcDiagnosticsReportReadinessState readinessState,
        WayangA2aJsonRpcDiagnosticsReportStatus status,
        WayangA2aJsonRpcDiagnosticsReportIssues reportIssues,
        List<Map<String, Object>> diagnosticChecks,
        WayangA2aJsonRpcDiagnosticsReportAttributes attributes) {

    WayangA2aJsonRpcDiagnosticsReportParts {
        context = Objects.requireNonNull(context, "context");
        readinessState = readinessState == null
                ? WayangA2aJsonRpcDiagnosticsReportReadinessState.from(context.readiness())
                : readinessState;
        status = status == null
                ? WayangA2aJsonRpcDiagnosticsReportStatus.from(
                        context.readiness(),
                        context.specAlignment())
                : status;
        reportIssues = reportIssues == null
                ? WayangA2aJsonRpcDiagnosticsReportIssues.from(
                        context.summary(),
                        context.specAlignment())
                : reportIssues;
        diagnosticChecks = diagnosticChecks == null
                ? WayangA2aJsonRpcReadinessDiagnosticChecks.from(
                                context.readiness(),
                                context.specAlignment(),
                                context.breakdown())
                        .toMaps()
                : copyObjects(diagnosticChecks);
        attributes = attributes == null
                ? WayangA2aJsonRpcDiagnosticsReportAttributes.from(
                        context.readiness(),
                        context.config(),
                        context.specAlignment())
                : attributes;
    }

    static WayangA2aJsonRpcDiagnosticsReportParts from(
            WayangA2aJsonRpcDiagnosticsReportContext context) {
        return new WayangA2aJsonRpcDiagnosticsReportParts(context, null, null, null, null, null);
    }

    int issueCount() {
        return reportIssues.issueCount();
    }

    List<Map<String, Object>> issues() {
        return reportIssues.issues();
    }

    Map<String, Object> attributeValues() {
        return attributes.values();
    }
}
