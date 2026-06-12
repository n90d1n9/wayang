package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.lenientBodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;

record WayangA2aJsonRpcBindingReportProbeContext(
        WayangA2aHttpResponse response,
        Map<String, Object> body,
        WayangA2aJsonRpcBindingReportSections sections,
        int methodCount,
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage diagnosticHandlerCoverage,
        WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage,
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot,
        WayangA2aJsonRpcBindingReportProbeIssues issues) {

    WayangA2aJsonRpcBindingReportProbeContext {
        response = Objects.requireNonNull(response, "response");
        body = WayangA2aMaps.copyMap(body);
        sections = sections == null
                ? WayangA2aJsonRpcBindingReportSections.from(body)
                : sections;
        methodCount = Math.max(0, methodCount);
        diagnosticHandlerCoverage = diagnosticHandlerCoverage == null
                ? WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(Map.of())
                : diagnosticHandlerCoverage;
        methodDispatchCoverage = methodDispatchCoverage == null
                ? WayangA2aJsonRpcMethodDispatchCoverage.fromMap(Map.of())
                : methodDispatchCoverage;
        methodRegistrySnapshot = methodRegistrySnapshot == null
                ? WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(Map.of())
                : methodRegistrySnapshot;
        issues = issues == null
                ? WayangA2aJsonRpcBindingReportProbeIssues.from(
                        response,
                        methodCount,
                        sections.required(),
                        diagnosticHandlerCoverage,
                        methodDispatchCoverage)
                : issues;
    }

    static WayangA2aJsonRpcBindingReportProbeContext from(WayangA2aHttpResponse response) {
        WayangA2aHttpResponse resolved = Objects.requireNonNull(response, "response");
        Map<String, Object> body = lenientBodyMap(resolved.body());
        WayangA2aJsonRpcBindingReportSections sections = WayangA2aJsonRpcBindingReportSections.from(body);
        int methodCount = number(body.get("methodCount"), 0);
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage diagnosticHandlerCoverage =
                WayangA2aJsonRpcHttpDiagnosticHandlerCoverage.fromMap(child(body, "diagnosticHandlers"));
        WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage =
                WayangA2aJsonRpcMethodDispatchCoverage.fromMap(child(body, "methodDispatch"));
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot =
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(child(body, "methodRegistry"));
        return new WayangA2aJsonRpcBindingReportProbeContext(
                resolved,
                body,
                sections,
                methodCount,
                diagnosticHandlerCoverage,
                methodDispatchCoverage,
                methodRegistrySnapshot,
                null);
    }
}
