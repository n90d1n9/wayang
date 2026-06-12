package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

record WayangA2aJsonRpcRouteCatalogDescriptorCoverage(
        boolean endpointDescriptor,
        boolean smokeDescriptor,
        boolean routeCatalogDescriptor,
        boolean diagnosticsReportDescriptor,
        boolean specComplianceReportDescriptor,
        boolean bindingReportDescriptor,
        boolean readinessDescriptor,
        boolean readinessIssueSummaryDescriptor) {

    static WayangA2aJsonRpcRouteCatalogDescriptorCoverage fromRoutes(List<Map<String, Object>> routes) {
        return new WayangA2aJsonRpcRouteCatalogDescriptorCoverage(
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.endpointSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.smokeSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.routeCatalogSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.diagnosticsReportSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.specComplianceReportSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.bindingReportSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.readinessSurface()),
                hasOperation(routes, WayangA2aJsonRpcHttpRouteSurface.readinessIssueSummarySurface()));
    }

    boolean complete() {
        return required().stream().allMatch(RequiredDescriptor::present);
    }

    List<Map<String, Object>> missingIssues() {
        return required().stream()
                .filter(descriptor -> !descriptor.present())
                .map(RequiredDescriptor::missingIssue)
                .toList();
    }

    private List<RequiredDescriptor> required() {
        return List.of(
                required(WayangA2aJsonRpcHttpRouteSurface.endpointSurface(), endpointDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.smokeSurface(), smokeDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.routeCatalogSurface(), routeCatalogDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.diagnosticsReportSurface(), diagnosticsReportDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.specComplianceReportSurface(), specComplianceReportDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.bindingReportSurface(), bindingReportDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.readinessSurface(), readinessDescriptor),
                required(WayangA2aJsonRpcHttpRouteSurface.readinessIssueSummarySurface(),
                        readinessIssueSummaryDescriptor));
    }

    private static boolean hasOperation(List<Map<String, Object>> routes, WayangA2aJsonRpcHttpRouteSurface surface) {
        if (routes == null || routes.isEmpty()) {
            return false;
        }
        String operation = surface.operation();
        return routes.stream()
                .anyMatch(route -> operation.equals(text(route.get("operation"), "")));
    }

    private static RequiredDescriptor required(WayangA2aJsonRpcHttpRouteSurface surface, boolean present) {
        return new RequiredDescriptor(surface.descriptorField(), surface.operation(), present);
    }

    private record RequiredDescriptor(String field, String operation, boolean present) {

        Map<String, Object> missingIssue() {
            return issue(
                    "routeCatalog",
                    "route_descriptor_missing",
                    field,
                    operation,
                    "missing",
                    "A2A JSON-RPC route catalog did not expose the " + operation + " descriptor.");
        }
    }
}
