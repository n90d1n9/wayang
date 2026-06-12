package tech.kayys.wayang.a2a.wayang;

import java.util.List;

final class WayangA2aJsonRpcHttpRouteSurfaceCatalog {

    private static final WayangA2aJsonRpcHttpRouteSurface ENDPOINT = new WayangA2aJsonRpcHttpRouteSurface(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT,
            "endpoint",
            WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
            "endpointDescriptor",
            "endpointPath",
            "",
            true,
            WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT);
    private static final WayangA2aJsonRpcHttpRouteSurface SMOKE = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
            "smoke",
            WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
            "smokeDescriptor",
            "smokePath",
            "smokeEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_SMOKE);
    private static final WayangA2aJsonRpcHttpRouteSurface ROUTE_CATALOG = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
            "route catalog",
            WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG,
            "routeCatalogDescriptor",
            "routeCatalogPath",
            "routeCatalogEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_ROUTE_CATALOG);
    private static final WayangA2aJsonRpcHttpRouteSurface DIAGNOSTICS_REPORT = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
            "diagnostics report",
            WayangA2aJsonRpcDiagnosticsReport.OPERATION_JSON_RPC_DIAGNOSTICS,
            "diagnosticsReportDescriptor",
            "diagnosticsReportPath",
            "diagnosticsReportEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_DIAGNOSTICS_REPORT);
    private static final WayangA2aJsonRpcHttpRouteSurface SPEC_COMPLIANCE_REPORT = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
            "spec compliance report",
            WayangA2aJsonRpcSpecComplianceReport.OPERATION_JSON_RPC_SPEC_COMPLIANCE,
            "specComplianceReportDescriptor",
            "specComplianceReportPath",
            "specComplianceReportEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_SPEC_COMPLIANCE_REPORT);
    private static final WayangA2aJsonRpcHttpRouteSurface BINDING_REPORT = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
            "binding report",
            WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
            "bindingReportDescriptor",
            "bindingReportPath",
            "bindingReportEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT);
    private static final WayangA2aJsonRpcHttpRouteSurface READINESS = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
            "readiness",
            WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS,
            "readinessDescriptor",
            "readinessPath",
            "readinessEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_READINESS);
    private static final WayangA2aJsonRpcHttpRouteSurface READINESS_ISSUE_SUMMARY = diagnostic(
            WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY,
            "readiness issue summary",
            WayangA2aJsonRpcReadinessIssueSummary.OPERATION_JSON_RPC_READINESS_ISSUE_SUMMARY,
            "readinessIssueSummaryDescriptor",
            "readinessIssueSummaryPath",
            "readinessIssueSummaryEnabled",
            WayangA2aJsonRpcHttpAdapter.ALLOW_READINESS_ISSUE_SUMMARY);
    private static final List<WayangA2aJsonRpcHttpRouteSurface> ORDERED = List.of(
            ENDPOINT,
            SMOKE,
            ROUTE_CATALOG,
            DIAGNOSTICS_REPORT,
            SPEC_COMPLIANCE_REPORT,
            BINDING_REPORT,
            READINESS,
            READINESS_ISSUE_SUMMARY);
    private static final List<WayangA2aJsonRpcHttpRouteSurface> BINDING_REPORT_REQUIRED_ORDER = List.of(
            ENDPOINT,
            SMOKE,
            BINDING_REPORT,
            ROUTE_CATALOG,
            DIAGNOSTICS_REPORT,
            SPEC_COMPLIANCE_REPORT,
            READINESS,
            READINESS_ISSUE_SUMMARY);

    private WayangA2aJsonRpcHttpRouteSurfaceCatalog() {
    }

    static List<WayangA2aJsonRpcHttpRouteSurface> ordered() {
        return ORDERED;
    }

    static List<WayangA2aJsonRpcHttpRouteSurface> bindingReportRequiredOrder() {
        return BINDING_REPORT_REQUIRED_ORDER;
    }

    static WayangA2aJsonRpcHttpRouteSurface endpoint() {
        return ENDPOINT;
    }

    static WayangA2aJsonRpcHttpRouteSurface smoke() {
        return SMOKE;
    }

    static WayangA2aJsonRpcHttpRouteSurface routeCatalog() {
        return ROUTE_CATALOG;
    }

    static WayangA2aJsonRpcHttpRouteSurface diagnosticsReport() {
        return DIAGNOSTICS_REPORT;
    }

    static WayangA2aJsonRpcHttpRouteSurface specComplianceReport() {
        return SPEC_COMPLIANCE_REPORT;
    }

    static WayangA2aJsonRpcHttpRouteSurface bindingReport() {
        return BINDING_REPORT;
    }

    static WayangA2aJsonRpcHttpRouteSurface readiness() {
        return READINESS;
    }

    static WayangA2aJsonRpcHttpRouteSurface readinessIssueSummary() {
        return READINESS_ISSUE_SUMMARY;
    }

    private static WayangA2aJsonRpcHttpRouteSurface diagnostic(
            String key,
            String routeName,
            String operation,
            String descriptorField,
            String pathField,
            String enabledField,
            String allow) {
        return new WayangA2aJsonRpcHttpRouteSurface(
                key,
                routeName,
                operation,
                descriptorField,
                pathField,
                enabledField,
                false,
                allow);
    }
}
