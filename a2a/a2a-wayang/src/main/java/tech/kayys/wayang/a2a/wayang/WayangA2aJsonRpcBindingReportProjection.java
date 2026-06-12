package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2A JSON-RPC binding reports.
 */
final class WayangA2aJsonRpcBindingReportProjection {

    private WayangA2aJsonRpcBindingReportProjection() {
    }

    static Map<String, Object> report(WayangA2aJsonRpcBindingReport report) {
        WayangA2aJsonRpcBindingReport resolved = Objects.requireNonNull(report, "report");
        WayangA2aJsonRpcHttpConfig config = resolved.config();
        List<WayangA2aJsonRpcHttpRouteDescriptor> routes = WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(config);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("binding", A2aProtocol.BINDING_JSONRPC);
        values.put("protocolVersion", A2aProtocol.VERSION);
        routes.forEach(route -> values.put(route.key(), route.toBindingReportMap()));
        values.put("diagnosticHandlers", diagnosticHandlers(routes));
        values.put("methodCount", resolved.methodCount());
        values.put("methods", resolved.methods().stream()
                .map(method -> method(method, config.endpointPath()))
                .toList());
        values.put("streamingMethods", resolved.streamingMethods());
        if (methodDispatchReported(resolved.methodDispatchCoverage())) {
            values.put("methodDispatch", resolved.methodDispatchCoverage().toMap());
        }
        if (methodRegistryReported(resolved.methodHandlerRegistrySnapshot())) {
            values.put("methodRegistry", resolved.methodHandlerRegistrySnapshot().toMap());
        }
        values.put("config", config.toMap());
        return WayangA2aMaps.copyMap(values);
    }

    static WayangA2aHttpResponse response(WayangA2aJsonRpcBindingReport report) {
        WayangA2aJsonRpcBindingReport resolved = Objects.requireNonNull(report, "report");
        return WayangA2aJsonRpcHttpResponses.json(
                WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                resolved.toJson());
    }

    private static Map<String, Object> diagnosticHandlers(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        return WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.from(
                        routes,
                        WayangA2aJsonRpcHttpDiagnosticHandlers.defaultHandlerKeys())
                .toMap();
    }

    private static Map<String, Object> method(String method, String endpointPath) {
        return WayangA2aJsonRpcMethods.requireDescriptor(method)
                .toBindingReportMap(endpointPath);
    }

    private static boolean methodDispatchReported(WayangA2aJsonRpcMethodDispatchCoverage coverage) {
        return coverage != null && coverage.reported();
    }

    private static boolean methodRegistryReported(WayangA2aJsonRpcMethodHandlerRegistrySnapshot snapshot) {
        return snapshot != null && snapshot.reported();
    }
}
