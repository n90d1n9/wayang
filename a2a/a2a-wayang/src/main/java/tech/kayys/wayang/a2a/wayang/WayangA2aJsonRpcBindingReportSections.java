package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

record WayangA2aJsonRpcBindingReportSections(
        WayangA2aJsonRpcBindingReportSection endpoint,
        WayangA2aJsonRpcBindingReportSection smoke,
        WayangA2aJsonRpcBindingReportSection routeCatalog,
        WayangA2aJsonRpcBindingReportSection diagnosticsReport,
        WayangA2aJsonRpcBindingReportSection specComplianceReport,
        WayangA2aJsonRpcBindingReportSection bindingReport,
        WayangA2aJsonRpcBindingReportSection readiness,
        WayangA2aJsonRpcBindingReportSection readinessIssueSummary) {

    static WayangA2aJsonRpcBindingReportSections from(Map<String, Object> body) {
        return new WayangA2aJsonRpcBindingReportSections(
                section(body, WayangA2aJsonRpcHttpRouteSurface.endpointSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.smokeSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.routeCatalogSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.diagnosticsReportSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.specComplianceReportSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.bindingReportSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.readinessSurface()),
                section(body, WayangA2aJsonRpcHttpRouteSurface.readinessIssueSummarySurface()));
    }

    List<WayangA2aJsonRpcBindingReportSection> required() {
        return WayangA2aJsonRpcHttpRouteSurface.bindingReportRequiredOrder().stream()
                .map(this::section)
                .toList();
    }

    private static WayangA2aJsonRpcBindingReportSection section(
            Map<String, Object> body,
            WayangA2aJsonRpcHttpRouteSurface surface) {
        return WayangA2aJsonRpcBindingReportSection.from(body, surface.key());
    }

    private WayangA2aJsonRpcBindingReportSection section(WayangA2aJsonRpcHttpRouteSurface surface) {
        return switch (surface.key()) {
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT -> endpoint;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE -> smoke;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG -> routeCatalog;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT -> diagnosticsReport;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT -> specComplianceReport;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT -> bindingReport;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS -> readiness;
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY -> readinessIssueSummary;
            default -> WayangA2aJsonRpcBindingReportSection.fromMap(surface.key(), Map.of());
        };
    }
}
