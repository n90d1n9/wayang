package tech.kayys.wayang.a2a.wayang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

record WayangA2aJsonRpcHttpDiagnosticHandlers(
        Map<String, Function<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpResponse>> handlers) {

    WayangA2aJsonRpcHttpDiagnosticHandlers {
        Map<String, Function<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpResponse>> copy =
                new LinkedHashMap<>();
        if (handlers != null) {
            handlers.forEach((key, handler) -> copy.put(
                    WayangA2aMaps.required(key, "key"),
                    Objects.requireNonNull(handler, "handler")));
        }
        handlers = Collections.unmodifiableMap(copy);
    }

    static List<String> defaultHandlerKeys() {
        return List.of(
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
    }

    static WayangA2aJsonRpcHttpDiagnosticHandlers from(
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            Supplier<WayangA2aJsonRpcBindingReport> bindingReport,
            Supplier<WayangA2aJsonRpcHttpRouteCatalog> routeCatalog,
            Supplier<WayangA2aJsonRpcDiagnosticsReport> diagnosticsReport,
            Supplier<WayangA2aJsonRpcSpecComplianceReport> specComplianceReport,
            Supplier<WayangA2aJsonRpcReadinessProbeResult> readinessProbe,
            Supplier<WayangA2aJsonRpcReadinessIssueSummary> readinessIssueSummary) {
        Supplier<WayangA2aJsonRpcBindingReport> resolvedBindingReport =
                Objects.requireNonNull(bindingReport, "bindingReport");
        Supplier<WayangA2aJsonRpcHttpRouteCatalog> resolvedRouteCatalog =
                Objects.requireNonNull(routeCatalog, "routeCatalog");
        Supplier<WayangA2aJsonRpcDiagnosticsReport> resolvedDiagnosticsReport =
                Objects.requireNonNull(diagnosticsReport, "diagnosticsReport");
        Supplier<WayangA2aJsonRpcSpecComplianceReport> resolvedSpecComplianceReport =
                Objects.requireNonNull(specComplianceReport, "specComplianceReport");
        Supplier<WayangA2aJsonRpcReadinessProbeResult> resolvedReadinessProbe =
                Objects.requireNonNull(readinessProbe, "readinessProbe");
        Supplier<WayangA2aJsonRpcReadinessIssueSummary> resolvedReadinessIssueSummary =
                Objects.requireNonNull(readinessIssueSummary, "readinessIssueSummary");
        Map<String, Function<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpResponse>> handlers =
                new LinkedHashMap<>();
        handlers.put(WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE, route -> smokeResponse(smokeRunner, route));
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                () -> resolvedRouteCatalog.get().response());
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                () -> resolvedDiagnosticsReport.get().response());
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                () -> resolvedSpecComplianceReport.get().response());
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                () -> resolvedBindingReport.get().response());
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                () -> resolvedReadinessProbe.get().response());
        put(handlers, WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY,
                () -> resolvedReadinessIssueSummary.get().response());
        return new WayangA2aJsonRpcHttpDiagnosticHandlers(handlers);
    }

    List<String> handlerKeys() {
        return List.copyOf(handlers.keySet());
    }

    WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverageFor(
            List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        return WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(routes, handlerKeys());
    }

    List<String> missingHandlerKeys(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        return coverageFor(routes).missingHandlerKeys();
    }

    List<String> orphanHandlerKeys(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        return coverageFor(routes).orphanHandlerKeys();
    }

    boolean completeFor(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        return coverageFor(routes).complete();
    }

    void requireCompleteFor(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage coverage = coverageFor(routes);
        List<String> missing = coverage.missingHandlerKeys();
        List<String> orphan = coverage.orphanHandlerKeys();
        if (!missing.isEmpty() || !orphan.isEmpty()) {
            throw new IllegalStateException(
                    "Incomplete A2A JSON-RPC diagnostic handler coverage: missing="
                            + missing + ", orphan=" + orphan + ".");
        }
    }

    Supplier<WayangA2aHttpResponse> responseSupplier(WayangA2aJsonRpcHttpRouteDescriptor route) {
        WayangA2aJsonRpcHttpRouteDescriptor resolved = Objects.requireNonNull(route, "route");
        Function<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpResponse> handler = handlers.get(resolved.key());
        if (handler == null) {
            throw new IllegalStateException("Unsupported A2A JSON-RPC diagnostic route key: " + resolved.key());
        }
        return () -> handler.apply(resolved);
    }

    private static void put(
            Map<String, Function<WayangA2aJsonRpcHttpRouteDescriptor, WayangA2aHttpResponse>> handlers,
            String key,
            Supplier<WayangA2aHttpResponse> supplier) {
        Supplier<WayangA2aHttpResponse> resolved = Objects.requireNonNull(supplier, key);
        handlers.put(key, route -> resolved.get());
    }

    private static WayangA2aHttpResponse smokeResponse(
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            WayangA2aJsonRpcHttpRouteDescriptor route) {
        if (smokeRunner == null) {
            return WayangA2aJsonRpcHttpResponses.error(
                    501,
                    route.operation(),
                    route.allow(),
                    "jsonrpc_smoke_not_configured",
                    "A2A JSON-RPC smoke runner is not configured.");
        }
        return WayangA2aJsonRpcSmokeProbeResult.response(smokeRunner.run());
    }
}
