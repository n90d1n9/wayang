package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

record WayangA2aJsonRpcDiagnosticsReportBuilder(
        WayangA2aJsonRpcReadinessProbeResult readiness,
        WayangA2aJsonRpcHttpConfig config,
        WayangA2aSpecAlignmentSnapshot specAlignment) {

    WayangA2aJsonRpcDiagnosticsReportBuilder {
        readiness = Objects.requireNonNull(readiness, "readiness");
        specAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
    }

    static WayangA2aJsonRpcDiagnosticsReportBuilder from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config) {
        return new WayangA2aJsonRpcDiagnosticsReportBuilder(
                readiness,
                config,
                WayangA2aSpecAlignmentSnapshot.defaults());
    }

    static WayangA2aJsonRpcDiagnosticsReportBuilder from(
            WayangA2aJsonRpcReadinessProbeResult readiness,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return new WayangA2aJsonRpcDiagnosticsReportBuilder(readiness, config, specAlignment);
    }

    WayangA2aJsonRpcDiagnosticsReport build() {
        WayangA2aJsonRpcDiagnosticsReportContext context =
                WayangA2aJsonRpcDiagnosticsReportContext.from(readiness, config, specAlignment);
        WayangA2aJsonRpcDiagnosticsReportParts parts =
                WayangA2aJsonRpcDiagnosticsReportParts.from(context);
        return new WayangA2aJsonRpcDiagnosticsReport(
                WayangA2aJsonRpcDiagnosticsReport.DIAGNOSTICS_ID,
                parts.status().passed(),
                parts.status().exitCode(),
                parts.readinessState().bindingReportPassed(),
                parts.readinessState().routeCatalogRequired(),
                parts.readinessState().routeCatalogPassed(),
                parts.readinessState().smokeRequired(),
                parts.readinessState().smokePassed(),
                parts.issueCount(),
                parts.diagnosticChecks(),
                parts.issues(),
                parts.attributeValues());
    }
}
