package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.lenientBodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.allowHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.protocolVersionHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.routeOperationHeader;

/**
 * HTTP-aware result for probing the A2A JSON-RPC binding report surface.
 */
public record WayangA2aJsonRpcBindingReportProbeResult(
        int statusCode,
        boolean httpSuccessful,
        String routeOperation,
        String protocolVersion,
        String contentType,
        String allow,
        int methodCount,
        List<String> streamingMethods,
        String endpointPath,
        String smokePath,
        boolean smokeEnabled,
        String routeCatalogPath,
        boolean routeCatalogEnabled,
        String diagnosticsReportPath,
        boolean diagnosticsReportEnabled,
        String specComplianceReportPath,
        boolean specComplianceReportEnabled,
        String bindingReportPath,
        boolean bindingReportEnabled,
        String readinessPath,
        boolean readinessEnabled,
        String readinessIssueSummaryPath,
        boolean readinessIssueSummaryEnabled,
        boolean diagnosticHandlersComplete,
        int diagnosticRouteKeyCount,
        int diagnosticHandlerKeyCount,
        List<String> diagnosticRouteKeys,
        List<String> diagnosticHandlerKeys,
        List<String> missingDiagnosticHandlerKeys,
        List<String> orphanDiagnosticHandlerKeys,
        boolean methodDispatchReported,
        boolean methodDispatchComplete,
        int registeredMethodCount,
        int dispatchMethodCount,
        List<String> registeredMethods,
        List<String> dispatchMethods,
        List<String> missingDispatchMethods,
        List<String> orphanDispatchMethods,
        List<Map<String, Object>> methodDispatchGroups,
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot,
        int issueCount,
        List<Map<String, Object>> issues,
        Map<String, Object> body,
        Map<String, Object> headers) {

    public WayangA2aJsonRpcBindingReportProbeResult {
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        protocolVersion = protocolVersion == null ? "" : protocolVersion.trim();
        contentType = contentType == null ? "" : contentType.trim();
        allow = allow == null ? "" : allow.trim();
        methodCount = Math.max(0, methodCount);
        streamingMethods = streamingMethods == null ? List.of() : List.copyOf(streamingMethods);
        endpointPath = endpointPath == null ? "" : endpointPath.trim();
        smokePath = smokePath == null ? "" : smokePath.trim();
        routeCatalogPath = routeCatalogPath == null ? "" : routeCatalogPath.trim();
        diagnosticsReportPath = diagnosticsReportPath == null ? "" : diagnosticsReportPath.trim();
        specComplianceReportPath = specComplianceReportPath == null ? "" : specComplianceReportPath.trim();
        bindingReportPath = bindingReportPath == null ? "" : bindingReportPath.trim();
        readinessPath = readinessPath == null ? "" : readinessPath.trim();
        readinessIssueSummaryPath = readinessIssueSummaryPath == null ? "" : readinessIssueSummaryPath.trim();
        diagnosticRouteKeyCount = Math.max(0, diagnosticRouteKeyCount);
        diagnosticHandlerKeyCount = Math.max(0, diagnosticHandlerKeyCount);
        diagnosticRouteKeys = diagnosticRouteKeys == null ? List.of() : List.copyOf(diagnosticRouteKeys);
        diagnosticHandlerKeys = diagnosticHandlerKeys == null ? List.of() : List.copyOf(diagnosticHandlerKeys);
        missingDiagnosticHandlerKeys = missingDiagnosticHandlerKeys == null
                ? List.of()
                : List.copyOf(missingDiagnosticHandlerKeys);
        orphanDiagnosticHandlerKeys = orphanDiagnosticHandlerKeys == null
                ? List.of()
                : List.copyOf(orphanDiagnosticHandlerKeys);
        registeredMethodCount = Math.max(0, registeredMethodCount);
        dispatchMethodCount = Math.max(0, dispatchMethodCount);
        registeredMethods = registeredMethods == null ? List.of() : List.copyOf(registeredMethods);
        dispatchMethods = dispatchMethods == null ? List.of() : List.copyOf(dispatchMethods);
        missingDispatchMethods = missingDispatchMethods == null ? List.of() : List.copyOf(missingDispatchMethods);
        orphanDispatchMethods = orphanDispatchMethods == null ? List.of() : List.copyOf(orphanDispatchMethods);
        methodDispatchGroups = copyObjects(methodDispatchGroups);
        methodRegistrySnapshot = methodRegistrySnapshot == null
                ? WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(Map.of())
                : methodRegistrySnapshot;
        issues = copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        body = WayangA2aMaps.copyMap(body);
        headers = WayangA2aMaps.copyMap(headers);
    }

    public static WayangA2aJsonRpcBindingReportProbeResult run(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return from(resolved.dispatch(WayangA2aJsonRpcHttpRequests.getJson(resolved.bindingReportPath())));
    }

    public static WayangA2aJsonRpcBindingReportProbeResult from(WayangA2aHttpResponse response) {
        WayangA2aJsonRpcBindingReportProbeContext context =
                WayangA2aJsonRpcBindingReportProbeContext.from(response);
        WayangA2aHttpResponse resolved = context.response();
        WayangA2aJsonRpcBindingReportSections sections = context.sections();
        WayangA2aJsonRpcHttpDiagnosticHandlerCoverage diagnosticHandlerCoverage =
                context.diagnosticHandlerCoverage();
        WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage =
                context.methodDispatchCoverage();
        WayangA2aJsonRpcBindingReportProbeIssues issues = context.issues();
        return new WayangA2aJsonRpcBindingReportProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                routeOperationHeader(resolved),
                protocolVersionHeader(resolved),
                resolved.contentType(),
                allowHeader(resolved),
                context.methodCount(),
                WayangA2aMaps.stringList(context.body().get("streamingMethods")),
                sections.endpoint().path(),
                sections.smoke().path(),
                sections.smoke().enabled(),
                sections.routeCatalog().path(),
                sections.routeCatalog().enabled(),
                sections.diagnosticsReport().path(),
                sections.diagnosticsReport().enabled(),
                sections.specComplianceReport().path(),
                sections.specComplianceReport().enabled(),
                sections.bindingReport().path(),
                sections.bindingReport().enabled(),
                sections.readiness().path(),
                sections.readiness().enabled(),
                sections.readinessIssueSummary().path(),
                sections.readinessIssueSummary().enabled(),
                diagnosticHandlerCoverage.complete(),
                diagnosticHandlerCoverage.routeKeyCount(),
                diagnosticHandlerCoverage.handlerKeyCount(),
                diagnosticHandlerCoverage.routeKeys(),
                diagnosticHandlerCoverage.handlerKeys(),
                diagnosticHandlerCoverage.missingHandlerKeys(),
                diagnosticHandlerCoverage.orphanHandlerKeys(),
                methodDispatchCoverage.reported(),
                methodDispatchCoverage.complete(),
                methodDispatchCoverage.registeredMethodCount(),
                methodDispatchCoverage.dispatchMethodCount(),
                methodDispatchCoverage.registeredMethods(),
                methodDispatchCoverage.dispatchMethods(),
                methodDispatchCoverage.missingDispatchMethods(),
                methodDispatchCoverage.orphanDispatchMethods(),
                methodDispatchCoverage.methodGroupMaps(),
                context.methodRegistrySnapshot(),
                issues.issueCount(),
                issues.issues(),
                context.body(),
                resolved.headers());
    }

    public static WayangA2aJsonRpcBindingReportProbeResult fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcBindingReportProbeProjection.fromMap(values);
    }

    public static WayangA2aJsonRpcBindingReportProbeResult fromJson(String json) {
        return fromMap(lenientBodyMap(json));
    }

    public boolean bindingReportRoute() {
        return readiness().bindingReportRoute();
    }

    public boolean jsonContent() {
        return readiness().jsonContent();
    }

    public boolean complete() {
        return readiness().complete();
    }

    public boolean passed() {
        return readiness().passed();
    }

    public int streamingMethodCount() {
        return streamingMethods.size();
    }

    public boolean methodRegistryReported() {
        return methodRegistrySnapshot.reported();
    }

    public int methodRegistryGroupCount() {
        return methodRegistrySnapshot.groupCount();
    }

    public List<Map<String, Object>> methodRegistryGroups() {
        return methodRegistrySnapshot.groups();
    }

    public int methodRegistryProviderCount() {
        return methodRegistrySnapshot.providerCount();
    }

    public List<String> methodRegistryProviderIds() {
        return methodRegistrySnapshot.providerIds();
    }

    public List<String> methodRegistryModuleIds() {
        return methodRegistrySnapshot.moduleIds();
    }

    public List<String> methodRegistryCapabilityTags() {
        return methodRegistrySnapshot.capabilityTags();
    }

    public String methodRegistryOverridePolicy() {
        return methodRegistrySnapshot.overridePolicy() == null ? "" : methodRegistrySnapshot.overridePolicy();
    }

    public int methodRegistryOverrideCount() {
        return methodRegistrySnapshot.overrideCount();
    }

    public List<Map<String, Object>> methodRegistryOverrides() {
        return methodRegistrySnapshot.overrides();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcBindingReportProbeProjection.probe(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    private WayangA2aJsonRpcBindingReportProbeReadiness readiness() {
        return WayangA2aJsonRpcBindingReportProbeReadiness.from(this);
    }

}
