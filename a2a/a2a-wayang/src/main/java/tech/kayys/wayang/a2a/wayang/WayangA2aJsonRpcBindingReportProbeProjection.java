package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC binding report probe envelopes.
 */
final class WayangA2aJsonRpcBindingReportProbeProjection {

    private WayangA2aJsonRpcBindingReportProbeProjection() {
    }

    static WayangA2aJsonRpcBindingReportProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        List<String> registeredMethods = WayangA2aMaps.stringList(copy.get("registeredMethods"));
        List<String> dispatchMethods = WayangA2aMaps.stringList(copy.get("dispatchMethods"));
        List<Map<String, Object>> methodDispatchGroups = methodDispatchGroups(
                copy,
                registeredMethods,
                dispatchMethods);
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot =
                methodRegistrySnapshot(copy);
        return new WayangA2aJsonRpcBindingReportProbeResult(
                number(copy.get("statusCode"), 0),
                bool(copy.get("httpSuccessful"), false),
                text(copy.get("routeOperation"), ""),
                text(copy.get("protocolVersion"), ""),
                text(copy.get("contentType"), ""),
                text(copy.get("allow"), ""),
                number(copy.get("methodCount"), 0),
                WayangA2aMaps.stringList(copy.get("streamingMethods")),
                text(copy.get("endpointPath"), ""),
                text(copy.get("smokePath"), ""),
                bool(copy.get("smokeEnabled"), false),
                text(copy.get("routeCatalogPath"), ""),
                bool(copy.get("routeCatalogEnabled"), false),
                text(copy.get("diagnosticsReportPath"), ""),
                bool(copy.get("diagnosticsReportEnabled"), false),
                text(copy.get("specComplianceReportPath"), ""),
                bool(copy.get("specComplianceReportEnabled"), false),
                text(copy.get("bindingReportPath"), ""),
                bool(copy.get("bindingReportEnabled"), false),
                text(copy.get("readinessPath"), ""),
                bool(copy.get("readinessEnabled"), false),
                text(copy.get("readinessIssueSummaryPath"), ""),
                bool(copy.get("readinessIssueSummaryEnabled"), false),
                bool(copy.get("diagnosticHandlersComplete"), false),
                number(copy.get("diagnosticRouteKeyCount"), 0),
                number(copy.get("diagnosticHandlerKeyCount"), 0),
                WayangA2aMaps.stringList(copy.get("diagnosticRouteKeys")),
                WayangA2aMaps.stringList(copy.get("diagnosticHandlerKeys")),
                WayangA2aMaps.stringList(copy.get("missingDiagnosticHandlerKeys")),
                WayangA2aMaps.stringList(copy.get("orphanDiagnosticHandlerKeys")),
                bool(copy.get("methodDispatchReported"), false),
                bool(copy.get("methodDispatchComplete"), false),
                number(copy.get("registeredMethodCount"), 0),
                number(copy.get("dispatchMethodCount"), 0),
                registeredMethods,
                dispatchMethods,
                WayangA2aMaps.stringList(copy.get("missingDispatchMethods")),
                WayangA2aMaps.stringList(copy.get("orphanDispatchMethods")),
                methodDispatchGroups,
                methodRegistrySnapshot,
                number(copy.get("issueCount"), 0),
                WayangA2aMaps.objectList(copy.get("issues")),
                child(copy, "body"),
                child(copy, "headers"));
    }

    static Map<String, Object> probe(WayangA2aJsonRpcBindingReportProbeResult probe) {
        WayangA2aJsonRpcBindingReportProbeResult resolved = Objects.requireNonNull(probe, "probe");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("httpSuccessful", resolved.httpSuccessful());
        values.put("routeOperation", resolved.routeOperation());
        values.put("protocolVersion", resolved.protocolVersion());
        values.put("contentType", resolved.contentType());
        values.put("allow", resolved.allow());
        values.put("bindingReportRoute", resolved.bindingReportRoute());
        values.put("jsonContent", resolved.jsonContent());
        values.put("complete", resolved.complete());
        values.put("passed", resolved.passed());
        values.put("issueCount", resolved.issueCount());
        values.put("issues", resolved.issues());
        values.put("methodCount", resolved.methodCount());
        values.put("streamingMethodCount", resolved.streamingMethodCount());
        values.put("endpointPath", resolved.endpointPath());
        values.put("smokePath", resolved.smokePath());
        values.put("smokeEnabled", resolved.smokeEnabled());
        values.put("routeCatalogPath", resolved.routeCatalogPath());
        values.put("routeCatalogEnabled", resolved.routeCatalogEnabled());
        values.put("diagnosticsReportPath", resolved.diagnosticsReportPath());
        values.put("diagnosticsReportEnabled", resolved.diagnosticsReportEnabled());
        values.put("specComplianceReportPath", resolved.specComplianceReportPath());
        values.put("specComplianceReportEnabled", resolved.specComplianceReportEnabled());
        values.put("bindingReportPath", resolved.bindingReportPath());
        values.put("bindingReportEnabled", resolved.bindingReportEnabled());
        values.put("readinessPath", resolved.readinessPath());
        values.put("readinessEnabled", resolved.readinessEnabled());
        values.put("readinessIssueSummaryPath", resolved.readinessIssueSummaryPath());
        values.put("readinessIssueSummaryEnabled", resolved.readinessIssueSummaryEnabled());
        values.put("diagnosticHandlersComplete", resolved.diagnosticHandlersComplete());
        values.put("diagnosticRouteKeyCount", resolved.diagnosticRouteKeyCount());
        values.put("diagnosticHandlerKeyCount", resolved.diagnosticHandlerKeyCount());
        values.put("diagnosticRouteKeys", resolved.diagnosticRouteKeys());
        values.put("diagnosticHandlerKeys", resolved.diagnosticHandlerKeys());
        values.put("missingDiagnosticHandlerKeys", resolved.missingDiagnosticHandlerKeys());
        values.put("orphanDiagnosticHandlerKeys", resolved.orphanDiagnosticHandlerKeys());
        if (resolved.methodDispatchReported()) {
            values.put("methodDispatchReported", true);
            values.put("methodDispatchComplete", resolved.methodDispatchComplete());
            values.put("registeredMethodCount", resolved.registeredMethodCount());
            values.put("dispatchMethodCount", resolved.dispatchMethodCount());
            values.put("registeredMethods", resolved.registeredMethods());
            values.put("dispatchMethods", resolved.dispatchMethods());
            values.put("missingDispatchMethods", resolved.missingDispatchMethods());
            values.put("orphanDispatchMethods", resolved.orphanDispatchMethods());
            values.put("methodDispatchGroups", resolved.methodDispatchGroups());
        }
        if (resolved.methodRegistryReported()) {
            values.put("methodRegistryReported", true);
            values.put("methodRegistryGroupCount", resolved.methodRegistryGroupCount());
            values.put("methodRegistryGroups", resolved.methodRegistryGroups());
            values.put("methodRegistryProviderCount", resolved.methodRegistryProviderCount());
            values.put("methodRegistryProviderIds", resolved.methodRegistryProviderIds());
            values.put("methodRegistryModuleIds", resolved.methodRegistryModuleIds());
            values.put("methodRegistryCapabilityTags", resolved.methodRegistryCapabilityTags());
            values.put("methodRegistryOverridePolicy", resolved.methodRegistryOverridePolicy());
            values.put("methodRegistryOverrideCount", resolved.methodRegistryOverrideCount());
            values.put("methodRegistryOverrides", resolved.methodRegistryOverrides());
        }
        values.put("streamingMethods", resolved.streamingMethods());
        values.put("body", resolved.body());
        values.put("headers", resolved.headers());
        return WayangA2aMaps.copyMap(values);
    }

    private static List<Map<String, Object>> methodDispatchGroups(
            Map<String, Object> values,
            List<String> registeredMethods,
            List<String> dispatchMethods) {
        List<Map<String, Object>> methodDispatchGroups =
                WayangA2aMaps.objectList(values.get("methodDispatchGroups"));
        if (methodDispatchGroups.isEmpty() && bool(values.get("methodDispatchReported"), false)) {
            return WayangA2aJsonRpcMethodDispatchCoverage.from(
                    registeredMethods,
                    dispatchMethods).methodGroupMaps();
        }
        return methodDispatchGroups;
    }

    private static WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot(Map<String, Object> values) {
        Map<String, Object> methodRegistry = child(values, "methodRegistry");
        if (!methodRegistry.isEmpty()) {
            return WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(methodRegistry);
        }
        if (!bool(values.get("methodRegistryReported"), false)) {
            return WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(Map.of());
        }
        Map<String, Object> flattened = new LinkedHashMap<>();
        flattened.put("reported", true);
        flattened.put("groups", WayangA2aMaps.objectList(values.get("methodRegistryGroups")));
        if (values.containsKey("methodRegistryProviderIds")) {
            flattened.put("providerIds", WayangA2aMaps.stringList(values.get("methodRegistryProviderIds")));
        }
        if (values.containsKey("methodRegistryModuleIds")) {
            flattened.put("moduleIds", WayangA2aMaps.stringList(values.get("methodRegistryModuleIds")));
        }
        if (values.containsKey("methodRegistryCapabilityTags")) {
            flattened.put("capabilityTags", WayangA2aMaps.stringList(values.get("methodRegistryCapabilityTags")));
        }
        flattened.put("overridePolicy", text(values.get("methodRegistryOverridePolicy"), ""));
        flattened.put("overrides", WayangA2aMaps.objectList(values.get("methodRegistryOverrides")));
        return WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(flattened);
    }
}
