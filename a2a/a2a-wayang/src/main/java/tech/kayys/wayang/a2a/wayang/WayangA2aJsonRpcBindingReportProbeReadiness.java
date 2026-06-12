package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

record WayangA2aJsonRpcBindingReportProbeReadiness(
        boolean bindingReportRoute,
        boolean jsonContent,
        boolean complete,
        boolean passed) {

    static WayangA2aJsonRpcBindingReportProbeReadiness from(
            WayangA2aJsonRpcBindingReportProbeResult probe) {
        WayangA2aJsonRpcBindingReportProbeResult resolved = Objects.requireNonNull(probe, "probe");
        boolean route = WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT
                .equals(resolved.routeOperation());
        boolean json = WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON.equals(resolved.contentType());
        boolean complete = complete(resolved);
        return new WayangA2aJsonRpcBindingReportProbeReadiness(
                route,
                json,
                complete,
                resolved.httpSuccessful() && route && json && complete);
    }

    private static boolean complete(WayangA2aJsonRpcBindingReportProbeResult probe) {
        return probe.methodCount() > 0
                && !probe.endpointPath().isBlank()
                && !probe.smokePath().isBlank()
                && !probe.routeCatalogPath().isBlank()
                && !probe.diagnosticsReportPath().isBlank()
                && !probe.specComplianceReportPath().isBlank()
                && !probe.bindingReportPath().isBlank()
                && !probe.readinessPath().isBlank()
                && !probe.readinessIssueSummaryPath().isBlank()
                && probe.diagnosticHandlersComplete()
                && (!probe.methodDispatchReported() || probe.methodDispatchComplete());
    }
}
