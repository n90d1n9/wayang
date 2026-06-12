package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free HTTP-shaped adapter for the A2A JSON-RPC binding.
 */
public final class WayangA2aJsonRpcHttpAdapter {

    public static final String DEFAULT_ENDPOINT_PATH = "/";
    public static final String DEFAULT_SMOKE_PATH = "/a2a/jsonrpc/smoke";
    public static final String DEFAULT_ROUTE_CATALOG_PATH = "/a2a/jsonrpc/route-catalog";
    public static final String DEFAULT_DIAGNOSTICS_REPORT_PATH = "/a2a/jsonrpc/diagnostics";
    public static final String DEFAULT_SPEC_COMPLIANCE_REPORT_PATH = "/a2a/jsonrpc/spec-compliance";
    public static final String DEFAULT_BINDING_REPORT_PATH = "/a2a/jsonrpc/binding-report";
    public static final String DEFAULT_READINESS_PATH = "/a2a/jsonrpc/readiness";
    public static final String DEFAULT_READINESS_ISSUE_SUMMARY_PATH = "/a2a/jsonrpc/readiness/issues";
    public static final String OPERATION_JSON_RPC = "JsonRpc";
    public static final String ALLOW_ENDPOINT = "POST, OPTIONS";
    public static final String ALLOW_SMOKE = "GET, OPTIONS";
    public static final String ALLOW_ROUTE_CATALOG = "GET, OPTIONS";
    public static final String ALLOW_DIAGNOSTICS_REPORT = "GET, OPTIONS";
    public static final String ALLOW_SPEC_COMPLIANCE_REPORT = "GET, OPTIONS";
    public static final String ALLOW_BINDING_REPORT = "GET, OPTIONS";
    public static final String ALLOW_READINESS = "GET, OPTIONS";
    public static final String ALLOW_READINESS_ISSUE_SUMMARY = "GET, OPTIONS";

    private final WayangA2aJsonRpcHttpConfig config;
    private final WayangA2aJsonRpcDispatcher dispatcher;
    private final WayangA2aJsonRpcHttpRouteTable routeTable;
    private final WayangA2aJsonRpcHttpRouteHandlers routeHandlers;

    public WayangA2aJsonRpcHttpAdapter(WayangA2aJsonRpcDispatcher dispatcher) {
        this(dispatcher, null, WayangA2aJsonRpcHttpConfig.defaults());
    }

    public WayangA2aJsonRpcHttpAdapter(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner) {
        this(dispatcher, smokeRunner, WayangA2aJsonRpcHttpConfig.defaults());
    }

    public WayangA2aJsonRpcHttpAdapter(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcHttpConfig config) {
        this(dispatcher, null, config);
    }

    public WayangA2aJsonRpcHttpAdapter(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            WayangA2aJsonRpcHttpConfig config) {
        this(
                dispatcher,
                smokeRunner,
                config,
                WayangA2aExtendedAgentCardAuthorizer.allowAll());
    }

    public WayangA2aJsonRpcHttpAdapter(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        WayangA2aJsonRpcDispatcher resolvedDispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.config = Objects.requireNonNull(config, "config");
        this.dispatcher = resolvedDispatcher;
        this.routeTable = WayangA2aJsonRpcHttpRouteTable.fromConfig(this.config);
        WayangA2aJsonRpcHttpDiagnosticHandlers diagnosticHandlers = WayangA2aJsonRpcHttpDiagnosticHandlers.from(
                smokeRunner,
                this::bindingReport,
                this::routeCatalog,
                this::diagnosticsReport,
                this::specComplianceReport,
                this::readinessProbe,
                this::readinessIssueSummary);
        diagnosticHandlers.requireCompleteFor(this.routeTable.routes());
        this.routeHandlers = new WayangA2aJsonRpcHttpRouteHandlers(
                resolvedDispatcher,
                WayangA2aExtensionNegotiator.fromAgentCard(resolvedDispatcher.agentCard()),
                extendedAgentCardAuthorizer,
                diagnosticHandlers);
    }

    public WayangA2aJsonRpcHttpAdapter(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            String endpointPath,
            String smokePath) {
        this(dispatcher, smokeRunner, WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath(endpointPath)
                .smokePath(smokePath)
                .build());
    }

    public static WayangA2aJsonRpcHttpAdapter of(WayangA2aJsonRpcDispatcher dispatcher) {
        return new WayangA2aJsonRpcHttpAdapter(dispatcher);
    }

    public static WayangA2aJsonRpcHttpAdapter withSmoke(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner) {
        return new WayangA2aJsonRpcHttpAdapter(dispatcher, smokeRunner);
    }

    public static WayangA2aJsonRpcHttpAdapter configured(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcHttpConfig config) {
        return new WayangA2aJsonRpcHttpAdapter(dispatcher, config);
    }

    public static WayangA2aJsonRpcHttpAdapter configured(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcHttpConfig config,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        return new WayangA2aJsonRpcHttpAdapter(dispatcher, null, config, extendedAgentCardAuthorizer);
    }

    public static WayangA2aJsonRpcHttpAdapter withSmoke(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aJsonRpcSmokeRunner smokeRunner,
            WayangA2aJsonRpcHttpConfig config) {
        return new WayangA2aJsonRpcHttpAdapter(dispatcher, smokeRunner, config);
    }

    public static WayangA2aJsonRpcHttpAdapter withExtendedAgentCardAuthorizer(
            WayangA2aJsonRpcDispatcher dispatcher,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        return new WayangA2aJsonRpcHttpAdapter(
                dispatcher,
                null,
                WayangA2aJsonRpcHttpConfig.defaults(),
                extendedAgentCardAuthorizer);
    }

    public WayangA2aJsonRpcHttpConfig config() {
        return config;
    }

    public Map<String, Object> dispatchCoverage() {
        return dispatcher.dispatchCoverage();
    }

    WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage() {
        return dispatcher.methodDispatchCoverage();
    }

    WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot() {
        return dispatcher.methodHandlerRegistrySnapshot();
    }

    public String endpointPath() {
        return config.endpointPath();
    }

    public String smokePath() {
        return config.smokePath();
    }

    public String routeCatalogPath() {
        return config.routeCatalogPath();
    }

    public String diagnosticsReportPath() {
        return config.diagnosticsReportPath();
    }

    public String specComplianceReportPath() {
        return config.specComplianceReportPath();
    }

    public String bindingReportPath() {
        return config.bindingReportPath();
    }

    public String readinessPath() {
        return config.readinessPath();
    }

    public String readinessIssueSummaryPath() {
        return config.readinessIssueSummaryPath();
    }

    public WayangA2aJsonRpcBindingReport bindingReport() {
        return WayangA2aJsonRpcBindingReport.from(this);
    }

    public WayangA2aJsonRpcHttpRouteCatalog routeCatalog() {
        return WayangA2aJsonRpcHttpRouteCatalog.from(this);
    }

    public WayangA2aJsonRpcHttpPublication routePublication() {
        return WayangA2aJsonRpcHttpPublication.from(this);
    }

    public WayangA2aJsonRpcSpecHealth specHealth() {
        return WayangA2aJsonRpcSpecHealth.from(this);
    }

    public WayangA2aSpecAlignmentReport specAlignmentReport() {
        return specHealth().specAlignmentReport();
    }

    public WayangA2aSpecAlignmentSnapshot specAlignmentSnapshot() {
        return specHealth().specAlignmentSnapshot();
    }

    public WayangA2aJsonRpcSpecComplianceReport specComplianceReport() {
        return specHealth().specComplianceReport();
    }

    public WayangA2aHttpResponse specComplianceReportResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.specComplianceReportPath()));
    }

    public WayangA2aHttpResponse smokeResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.smokePath()));
    }

    public WayangA2aJsonRpcSmokeProbeResult smokeProbe() {
        return WayangA2aJsonRpcSmokeProbeResult.run(this);
    }

    public WayangA2aHttpResponse routeCatalogResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.routeCatalogPath()));
    }

    public WayangA2aJsonRpcRouteCatalogProbeResult routeCatalogProbe() {
        return WayangA2aJsonRpcRouteCatalogProbeResult.run(this);
    }

    public WayangA2aHttpResponse bindingReportResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.bindingReportPath()));
    }

    public WayangA2aJsonRpcBindingReportProbeResult bindingReportProbe() {
        return WayangA2aJsonRpcBindingReportProbeResult.run(this);
    }

    public WayangA2aJsonRpcReadinessProbeResult readinessProbe() {
        return WayangA2aJsonRpcReadinessProbeResult.run(this);
    }

    public WayangA2aJsonRpcReadinessIssueSummary readinessIssueSummary() {
        return WayangA2aJsonRpcReadinessIssueSummary.from(readinessProbe());
    }

    public WayangA2aJsonRpcDiagnosticsReport diagnosticsReport() {
        return specHealth().diagnosticsReport();
    }

    public WayangA2aHttpResponse diagnosticsReportResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.diagnosticsReportPath()));
    }

    public WayangA2aHttpResponse readinessResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.readinessPath()));
    }

    public WayangA2aHttpResponse readinessIssueSummaryResponse() {
        return dispatch(WayangA2aJsonRpcHttpRequests.getJson(config.readinessIssueSummaryPath()));
    }

    public WayangA2aHttpResponse dispatch(WayangA2aHttpRequest request) {
        return routeTable.dispatch(request, routeHandlers::dispatch);
    }
}
